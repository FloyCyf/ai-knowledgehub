param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$NacosUrl = "http://localhost:8848",
    [string]$AdminToken,
    [long]$ArticleId = 0
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($AdminToken)) {
    throw "AdminToken is required. Login with admin_demo/123456 or pass an ADMIN JWT."
}

function Read-ResponseBody {
    param($Response)
    if ($null -eq $Response) { return "" }
    $stream = $Response.GetResponseStream()
    if ($null -eq $stream) { return "" }
    $reader = New-Object System.IO.StreamReader($stream)
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}

function Invoke-AkhHttp {
    param(
        [string]$Method,
        [string]$Url,
        [object]$Body = $null,
        [hashtable]$Headers = @{},
        [int]$TimeoutSec = 30
    )

    $params = @{
        Uri = $Url
        Method = $Method
        Headers = $Headers
        TimeoutSec = $TimeoutSec
        UseBasicParsing = $true
        ErrorAction = "Stop"
    }
    if ($null -ne $Body) {
        $params["ContentType"] = "application/json"
        $params["Body"] = ($Body | ConvertTo-Json -Depth 20)
    }

    try {
        $response = Invoke-WebRequest @params
        $status = [int]$response.StatusCode
        $content = [string]$response.Content
    } catch {
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            $content = Read-ResponseBody $_.Exception.Response
        } else {
            throw
        }
    }

    $json = $null
    if (-not [string]::IsNullOrWhiteSpace($content)) {
        try {
            $json = $content | ConvertFrom-Json
        } catch {
            $json = $null
        }
    }

    return [pscustomobject]@{
        Method = $Method
        Url = $Url
        Status = $status
        Content = $content
        Json = $json
    }
}

function Assert-Status {
    param([string]$Name, [object]$Response, [int[]]$Expected)
    if ($Expected -contains $Response.Status) {
        Write-Host "[PASS] $Name -> HTTP $($Response.Status)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] $Name -> HTTP $($Response.Status)" -ForegroundColor Red
        Write-Host $Response.Content
        throw "$Name failed"
    }
}

function Get-Token {
    param([object]$LoginResponse)
    if ($LoginResponse.Json -and $LoginResponse.Json.data -and $LoginResponse.Json.data.token) {
        return [string]$LoginResponse.Json.data.token
    }
    throw "Login response does not contain data.token"
}

function Get-ArticleId {
    param([object]$DraftResponse)
    if ($DraftResponse.Json -and $DraftResponse.Json.data -and $DraftResponse.Json.data.articleId) {
        return [long]$DraftResponse.Json.data.articleId
    }
    throw "Draft response does not contain data.articleId"
}

function Get-NacosConfigContent {
    param([string]$Base)
    $url = "$($Base.TrimEnd('/'))/nacos/v1/cs/configs?dataId=gateway-service.yml&group=DEFAULT_GROUP"
    return (Invoke-AkhHttp -Method GET -Url $url).Content
}

function Wait-NacosConfigContent {
    param(
        [string]$Base,
        [string]$ExpectedText,
        [int]$MaxAttempts = 12
    )

    $content = ""
    for ($i = 1; $i -le $MaxAttempts; $i++) {
        $content = Get-NacosConfigContent -Base $Base
        if ($content -like "*$ExpectedText*") {
            return $content
        }
        Write-Host "waiting Nacos config $i/$MaxAttempts ..."
        Start-Sleep -Seconds 1
    }

    Write-Host "Last Nacos gateway-service.yml:"
    Write-Host $content
    throw "Nacos config does not contain $ExpectedText"
}

function Wait-RateLimitConfig {
    param([string]$ExpectedText, [hashtable]$Headers)
    for ($i = 1; $i -le 12; $i++) {
        Start-Sleep -Seconds 1
        $current = Invoke-AkhHttp -Method GET -Url "$BaseUrl/api/admin/rate-limit/article-detail" -Headers $Headers
        if ($current.Content -like "*$ExpectedText*") {
            Write-Host "[PASS] gateway refreshed Nacos config: $ExpectedText" -ForegroundColor Green
            return
        }
        Write-Host "waiting gateway Nacos refresh $i/12 ..."
    }
    throw "Gateway did not refresh Nacos config: $ExpectedText"
}

$BaseUrl = $BaseUrl.TrimEnd("/")
$NacosUrl = $NacosUrl.TrimEnd("/")
$adminAuth = @{ Authorization = "Bearer $AdminToken" }

Write-Host "==> Ensure article exists"
if ($ArticleId -le 0) {
    $timestamp = Get-Date -Format "yyyyMMddHHmmss"
    $userBody = @{ username = "nacos_rate_$timestamp"; password = "123456" }
    $register = Invoke-AkhHttp -Method POST -Url "$BaseUrl/api/user/register" -Body $userBody
    Assert-Status "register rate-limit user" $register @(200)
    $login = Invoke-AkhHttp -Method POST -Url "$BaseUrl/api/user/login" -Body $userBody
    Assert-Status "login rate-limit user" $login @(200)
    $userAuth = @{ Authorization = "Bearer $(Get-Token $login)" }
    $draft = Invoke-AkhHttp -Method POST -Url "$BaseUrl/api/articles/draft" -Headers $userAuth -Body @{
        title = "Nacos rate limit article $timestamp"
        summary = "Nacos rate limit acceptance"
        content = "This article is used to verify Nacos dynamic rate-limit configuration."
    }
    Assert-Status "create rate-limit article" $draft @(200)
    $ArticleId = Get-ArticleId $draft
    $publish = Invoke-AkhHttp -Method POST -Url "$BaseUrl/api/articles/$ArticleId/publish" -Headers $userAuth
    Assert-Status "publish rate-limit article" $publish @(200)
}

Write-Host ""
Write-Host "==> Publish maxRequests=5 to Nacos through admin API"
$setLimit = Invoke-AkhHttp -Method PUT -Url "$BaseUrl/api/admin/rate-limit/article-detail" -Headers $adminAuth -Body @{
    windowSeconds = 10
    maxRequests = 5
    enabled = $true
}
Assert-Status "admin publishes Nacos rate limit to 5" $setLimit @(200)

$nacosConfig = Wait-NacosConfigContent -Base $NacosUrl -ExpectedText "max-requests: 5"
Write-Host "Nacos gateway-service.yml:"
Write-Host $nacosConfig
Write-Host "[PASS] Nacos config contains max-requests: 5" -ForegroundColor Green
Wait-RateLimitConfig -ExpectedText '"maxRequests":5' -Headers $adminAuth

Write-Host ""
Write-Host "==> Hit article detail until 429"
$hit429 = $false
for ($i = 1; $i -le 8; $i++) {
    $response = Invoke-AkhHttp -Method GET -Url "$BaseUrl/api/articles/$ArticleId" -Headers @{
        "X-Real-IP" = "127.0.0.89"
    }
    Write-Host "rate-limit request $i -> HTTP $($response.Status)"
    if ($response.Status -eq 429) {
        $hit429 = $true
        break
    }
}
if (-not $hit429) {
    throw "Rate limit did not return 429 after Nacos config update"
}
Write-Host "[PASS] Nacos dynamic rate limit returned 429" -ForegroundColor Green

Write-Host ""
Write-Host "==> Restore maxRequests=20"
$restore = Invoke-AkhHttp -Method PUT -Url "$BaseUrl/api/admin/rate-limit/article-detail" -Headers $adminAuth -Body @{
    windowSeconds = 10
    maxRequests = 20
    enabled = $true
}
Assert-Status "restore default Nacos rate limit" $restore @(200)
Wait-RateLimitConfig -ExpectedText '"maxRequests":20' -Headers $adminAuth
Write-Host "Nacos rate-limit acceptance finished." -ForegroundColor Green
