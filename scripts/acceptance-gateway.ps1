param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AdminToken = "",
    [long]$ArticleId = 0,
    [string]$OutputDir = "docs/acceptance/runtime",
    [switch]$SkipRateLimit
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Read-ResponseBody {
    param($Response)
    if ($null -eq $Response) {
        return ""
    }
    $stream = $Response.GetResponseStream()
    if ($null -eq $stream) {
        return ""
    }
    $reader = New-Object System.IO.StreamReader($stream)
    try {
        return $reader.ReadToEnd()
    } finally {
        $reader.Dispose()
    }
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
    param(
        [string]$Name,
        [object]$Response,
        [int[]]$Expected
    )
    $ok = $Expected -contains $Response.Status
    $expectedText = $Expected -join "/"
    if ($ok) {
        Write-Host "[PASS] $Name -> HTTP $($Response.Status)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] $Name -> HTTP $($Response.Status), expected $expectedText" -ForegroundColor Red
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

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$BaseUrl = $BaseUrl.TrimEnd("/")
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$username = "a_accept_$timestamp"
$password = "123456"
$results = New-Object System.Collections.Generic.List[object]

Write-Step "Gateway health"
$health = Invoke-AkhHttp -Method GET -Url "$BaseUrl/actuator/health"
$results.Add($health)
Assert-Status "gateway health" $health @(200)

Write-Step "User register and login through gateway"
$register = Invoke-AkhHttp -Method POST -Url "$BaseUrl/api/user/register" -Body @{
    username = $username
    password = $password
}
$results.Add($register)
Assert-Status "register" $register @(200)

$login = Invoke-AkhHttp -Method POST -Url "$BaseUrl/api/user/login" -Body @{
    username = $username
    password = $password
}
$results.Add($login)
Assert-Status "login" $login @(200)
$userToken = Get-Token $login
$userAuth = @{ "Authorization" = "Bearer $userToken" }

Write-Step "JWT protection and trusted gateway headers"
$profile401 = Invoke-AkhHttp -Method GET -Url "$BaseUrl/api/user/profile"
$results.Add($profile401)
Assert-Status "profile without token" $profile401 @(401)

$profile = Invoke-AkhHttp -Method GET -Url "$BaseUrl/api/user/profile" -Headers @{
    "Authorization" = "Bearer $userToken"
    "X-User-Id" = "999999"
    "X-User-Role" = "ADMIN"
    "X-User-Name" = "forged"
}
$results.Add($profile)
Assert-Status "profile with token and forged headers" $profile @(200)

Write-Step "Admin permission check with normal user token"
$adminForbidden = Invoke-AkhHttp -Method PUT -Url "$BaseUrl/api/admin/rate-limit/article-detail" -Headers $userAuth -Body @{
    windowSeconds = 10
    maxRequests = 5
    enabled = $true
}
$results.Add($adminForbidden)
Assert-Status "normal user calls admin API" $adminForbidden @(403)

Write-Step "Article route through gateway"
if ($ArticleId -le 0) {
    $draft = Invoke-AkhHttp -Method POST -Url "$BaseUrl/api/articles/draft" -Headers $userAuth -Body @{
        title = "Gateway acceptance article $timestamp"
        content = "This article is created by scripts/acceptance-gateway.ps1 for gateway acceptance."
        summary = "Gateway acceptance"
    }
    $results.Add($draft)
    Assert-Status "create article draft" $draft @(200)
    $ArticleId = Get-ArticleId $draft

    $publish = Invoke-AkhHttp -Method POST -Url "$BaseUrl/api/articles/$ArticleId/publish" -Headers $userAuth
    $results.Add($publish)
    Assert-Status "publish article" $publish @(200)
}

$latest = Invoke-AkhHttp -Method GET -Url "$BaseUrl/api/articles/latest"
$results.Add($latest)
Assert-Status "latest articles" $latest @(200)

$detail = Invoke-AkhHttp -Method GET -Url "$BaseUrl/api/articles/$ArticleId"
$results.Add($detail)
Assert-Status "article detail" $detail @(200)

Write-Step "Ranking and AI routes through gateway"
$ranking = Invoke-AkhHttp -Method GET -Url "$BaseUrl/api/ranking/top10"
$results.Add($ranking)
Assert-Status "ranking top10" $ranking @(200)

$ai = Invoke-AkhHttp -Method POST -Url "$BaseUrl/api/ai/continue-writing" -Headers $userAuth -Body @{
    prompt = "Write one sentence for AI KnowledgeHub acceptance."
}
$results.Add($ai)
Assert-Status "ai continue writing" $ai @(200)

$streamPrompt = [System.Uri]::EscapeDataString("AI KnowledgeHub gateway SSE acceptance")
$sse = Invoke-AkhHttp -Method GET -Url "$BaseUrl/api/ai/continue-writing/stream?prompt=$streamPrompt" -Headers $userAuth -TimeoutSec 20
$results.Add($sse)
Assert-Status "ai sse stream" $sse @(200)

$analysis = Invoke-AkhHttp -Method GET -Url "$BaseUrl/api/ai/articles/$ArticleId/analysis" -Headers $userAuth
$results.Add($analysis)
Assert-Status "ai article analysis" $analysis @(200)

if (-not $SkipRateLimit) {
    Write-Step "Rate limit check"
    if ([string]::IsNullOrWhiteSpace($AdminToken)) {
        Write-Host "[SKIP] AdminToken is empty. Pass -AdminToken to verify dynamic config and 429 trigger." -ForegroundColor Yellow
    } else {
        $adminAuth = @{ "Authorization" = "Bearer $AdminToken" }
        $setLimit = Invoke-AkhHttp -Method PUT -Url "$BaseUrl/api/admin/rate-limit/article-detail" -Headers $adminAuth -Body @{
            windowSeconds = 10
            maxRequests = 5
            enabled = $true
        }
        $results.Add($setLimit)
        Assert-Status "admin updates rate limit to 5" $setLimit @(200)

        $hit429 = $false
        for ($i = 1; $i -le 8; $i++) {
            $rateResp = Invoke-AkhHttp -Method GET -Url "$BaseUrl/api/articles/$ArticleId" -Headers @{
                "X-Real-IP" = "127.0.0.88"
            }
            $results.Add($rateResp)
            Write-Host "rate-limit request $i -> HTTP $($rateResp.Status)"
            if ($rateResp.Status -eq 429) {
                $hit429 = $true
                break
            }
        }
        if (-not $hit429) {
            throw "Rate limit did not return 429 after dynamic config"
        }
        Write-Host "[PASS] rate limit returned 429" -ForegroundColor Green

        $restoreLimit = Invoke-AkhHttp -Method PUT -Url "$BaseUrl/api/admin/rate-limit/article-detail" -Headers $adminAuth -Body @{
            windowSeconds = 10
            maxRequests = 20
            enabled = $true
        }
        $results.Add($restoreLimit)
        Assert-Status "restore default rate limit" $restoreLimit @(200)
    }
}

$summary = [pscustomobject]@{
    baseUrl = $BaseUrl
    username = $username
    articleId = $ArticleId
    adminRateLimitVerified = (-not [string]::IsNullOrWhiteSpace($AdminToken) -and -not $SkipRateLimit)
    generatedAt = (Get-Date).ToString("s")
    results = $results
}

$summaryPath = Join-Path $OutputDir "last-gateway-acceptance.json"
$summary | ConvertTo-Json -Depth 30 | Set-Content -Path $summaryPath -Encoding UTF8

Write-Host ""
Write-Host "Acceptance finished. Runtime record: $summaryPath" -ForegroundColor Green
