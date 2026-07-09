param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$RedisContainer = "akh-redis",
    [string]$RedisKey = "article:hot:ranking",
    [long]$DemoArticleId = 9101,
    [long]$SecondDemoArticleId = 9102
)

$ErrorActionPreference = "Stop"

function Invoke-DockerRedis {
    param([string[]]$Arguments)

    $output = & docker exec $RedisContainer redis-cli @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker exec $RedisContainer redis-cli $($Arguments -join ' ') failed"
    }
    return @($output)
}

function Get-DemoToken {
    $username = "ranking_demo_" + (Get-Date -Format "HHmmss")
    $body = @{
        username = $username
        password = "123456"
    } | ConvertTo-Json -Compress

    $registerResponse = Invoke-RestMethod `
        -Method Post `
        -Uri "$BaseUrl/api/user/register" `
        -ContentType "application/json" `
        -Body $body
    if ($registerResponse.code -ne 200 -or -not $registerResponse.success) {
        throw "ranking demo user register failed"
    }

    $loginResponse = Invoke-RestMethod `
        -Method Post `
        -Uri "$BaseUrl/api/user/login" `
        -ContentType "application/json" `
        -Body $body
    if ($loginResponse.code -ne 200 -or -not $loginResponse.success -or [string]::IsNullOrWhiteSpace($loginResponse.data.token)) {
        throw "ranking demo user login failed"
    }

    return $loginResponse.data.token
}

function Invoke-RankingAction {
    param(
        [long]$ArticleId,
        [string]$Action,
        [string]$Token
    )

    $url = "$BaseUrl/api/ranking/articles/$ArticleId/$Action"
    $response = Invoke-RestMethod `
        -Method Post `
        -Uri $url `
        -Headers @{ Authorization = "Bearer $Token" }
    if ($response.code -ne 200 -or -not $response.success) {
        $message = $response.message
        throw "ranking action failed: $url, code=$($response.code), message=$message"
    }
}

Write-Host "==> Seed Redis ZSET through gateway ranking APIs" -ForegroundColor Cyan
$token = Get-DemoToken
Write-Host "[PASS] ranking demo user logged in through gateway"
Invoke-RankingAction -ArticleId $DemoArticleId -Action "publish" -Token $token
Invoke-RankingAction -ArticleId $DemoArticleId -Action "view" -Token $token
Invoke-RankingAction -ArticleId $DemoArticleId -Action "like" -Token $token
Invoke-RankingAction -ArticleId $SecondDemoArticleId -Action "publish" -Token $token
Invoke-RankingAction -ArticleId $SecondDemoArticleId -Action "comment" -Token $token
Write-Host "[PASS] ranking demo scores submitted through gateway"
Write-Host ""

Write-Host "==> Ranking Top10 through gateway" -ForegroundColor Cyan
$topRaw = curl.exe -s "$BaseUrl/api/ranking/top10"
$topObj = $topRaw | ConvertFrom-Json
$topObj | ConvertTo-Json -Depth 8

if ($topObj.code -ne 200 -or -not $topObj.success) {
    throw "ranking top10 API did not return success"
}

Write-Host ""
Write-Host "==> Redis ZSET proof from Docker container: $RedisContainer" -ForegroundColor Cyan
$type = (Invoke-DockerRedis @("TYPE", $RedisKey) | Select-Object -First 1).Trim()
$cardRaw = (Invoke-DockerRedis @("ZCARD", $RedisKey) | Select-Object -First 1).Trim()
$card = [int]$cardRaw
$range = Invoke-DockerRedis @("ZREVRANGE", $RedisKey, "0", "9", "WITHSCORES")

Write-Host "TYPE $RedisKey = $type"
Write-Host "ZCARD $RedisKey = $card"
Write-Host "ZREVRANGE $RedisKey 0 9 WITHSCORES:"
$range | ForEach-Object { Write-Host $_ }

if ($type -ne "zset") {
    throw "Redis key $RedisKey is '$type', expected 'zset'. Check whether ranking-service is using the same Redis container."
}

$httpArticles = @($topObj.data.articles)
$httpTotal = [int]$topObj.data.total
$redisTopCount = [Math]::Min($card, 10)

Write-Host ""
Write-Host "HTTP data.total = $httpTotal   Redis ZCARD = $card   Redis top count = $redisTopCount"

if ($httpTotal -ne $redisTopCount) {
    throw "HTTP total does not match Redis Top10 count"
}

for ($i = 0; $i -lt $httpArticles.Count; $i++) {
    $redisArticleId = [long]$range[$i * 2]
    $redisScore = [double]$range[$i * 2 + 1]
    $httpArticleId = [long]$httpArticles[$i].articleId
    $httpScore = [double]$httpArticles[$i].hotScore

    if ($httpArticleId -ne $redisArticleId -or [Math]::Abs($httpScore - $redisScore) -gt 0.0001) {
        throw "Mismatch at rank $($i + 1): HTTP=($httpArticleId,$httpScore), Redis=($redisArticleId,$redisScore)"
    }
}

Write-Host "[PASS] HTTP Top10 matches Redis ZSET $RedisKey" -ForegroundColor Green
