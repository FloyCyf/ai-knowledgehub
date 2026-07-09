param(
    [string]$LogDir = "docs/acceptance/runtime/logs",
    [int]$StartupTimeoutSeconds = 90,
    [switch]$RankingUseRedis,
    [string]$RedisHost = "127.0.0.1",
    [int]$RedisPort = 6379,
    [int]$RedisDatabase = 0,
    [string]$NacosUrl = "http://localhost:8848"
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$runtimeDir = Join-Path $root "docs/acceptance/runtime"
$logPath = Join-Path $root $LogDir
$pidFile = Join-Path $runtimeDir "service-pids.json"

New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null
New-Item -ItemType Directory -Force -Path $logPath | Out-Null

$mvnCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if ($null -eq $mvnCommand) {
    $mvnCommand = Get-Command mvn -ErrorAction Stop
}

function Test-PortListening {
    param([int]$Port)
    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return $null -ne $connections
}

$requiredPorts = @(8080, 8081, 8082, 8083, 8084)
$busyPorts = @($requiredPorts | Where-Object { Test-PortListening -Port $_ })
if ($busyPorts.Count -gt 0) {
    $portText = $busyPorts -join ", "
    throw "Ports already in use: $portText. Run scripts\stop-all-services.ps1 first, then start again."
}

function Test-NacosReady {
    param([string]$BaseUrl)
    $readyUrl = $BaseUrl.TrimEnd("/") + "/nacos/v1/console/health/readiness"
    try {
        $response = Invoke-WebRequest -Uri $readyUrl -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
        return ($response.StatusCode -eq 200 -and [string]$response.Content -match "OK|UP")
    } catch {
        return $false
    }
}

if (Test-NacosReady -BaseUrl $NacosUrl) {
    Write-Host "[OK] Nacos config center is ready at $NacosUrl" -ForegroundColor Green
} else {
    Write-Host "[WARN] Nacos is not ready at $NacosUrl. Gateway will use local defaults, and dynamic config publishing will fail until Nacos starts." -ForegroundColor Yellow
    Write-Host "       Run: docker compose up -d nacos" -ForegroundColor Yellow
}

Write-Host "Installing project modules to local Maven repository ..." -ForegroundColor Cyan
$installProcess = Start-Process `
    -FilePath $mvnCommand.Source `
    -ArgumentList @("-DskipTests", "install") `
    -WorkingDirectory $root `
    -NoNewWindow `
    -Wait `
    -PassThru

if ($installProcess.ExitCode -ne 0) {
    throw "mvn -DskipTests install failed. Fix the build before starting services."
}
Write-Host "[OK] Maven local install finished" -ForegroundColor Green

function Wait-Port {
    param(
        [int]$Port,
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $client = New-Object System.Net.Sockets.TcpClient
        try {
            $async = $client.BeginConnect("127.0.0.1", $Port, $null, $null)
            if ($async.AsyncWaitHandle.WaitOne(1000, $false)) {
                $client.EndConnect($async)
                return $true
            }
        } catch {
        } finally {
            $client.Close()
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Start-ServiceModule {
    param(
        [string]$Name,
        [string]$ModuleDir,
        [int]$Port,
        [string[]]$MavenArguments = @("spring-boot:run")
    )

    Write-Host "Starting $Name on port $Port ..." -ForegroundColor Cyan
    $workDir = Join-Path $root $ModuleDir
    $stdout = Join-Path $logPath "$Name.out.log"
    $stderr = Join-Path $logPath "$Name.err.log"

    $process = Start-Process `
        -FilePath $mvnCommand.Source `
        -ArgumentList $MavenArguments `
        -WorkingDirectory $workDir `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -WindowStyle Hidden `
        -PassThru

    $ready = Wait-Port -Port $Port -TimeoutSeconds $StartupTimeoutSeconds
    if (-not $ready) {
        Write-Host "[WARN] $Name did not open port $Port within $StartupTimeoutSeconds seconds. Check $stderr" -ForegroundColor Yellow
    } else {
        Write-Host "[OK] $Name is listening on $Port" -ForegroundColor Green
    }

    return [pscustomobject]@{
        name = $Name
        module = $ModuleDir
        port = $Port
        pid = $process.Id
        stdout = $stdout
        stderr = $stderr
        ready = $ready
        arguments = $MavenArguments
    }
}

$rankingArguments = @("spring-boot:run")
if ($RankingUseRedis) {
    $rankingArguments = @(
        "spring-boot:run",
        "-Dranking.use-redis=true",
        "-Dspring.data.redis.host=$RedisHost",
        "-Dspring.data.redis.port=$RedisPort",
        "-Dspring.data.redis.database=$RedisDatabase"
    )
    Write-Host "Ranking service will use Redis ZSET mode for acceptance video. Redis=$RedisHost`:$RedisPort/$RedisDatabase" -ForegroundColor Cyan
}

$nacosServerAddr = $NacosUrl.TrimEnd("/") -replace "^https?://", ""
$gatewayArguments = @("spring-boot:run", "-Dspring-boot.run.arguments=--spring.cloud.nacos.config.server-addr=$nacosServerAddr")
Write-Host "Gateway Nacos config enabled. server-addr=$nacosServerAddr, dataId=gateway-service.yml" -ForegroundColor Cyan

$services = @(
    @{ Name = "user-service"; ModuleDir = "user-service"; Port = 8081 },
    @{ Name = "article-service"; ModuleDir = "article-service"; Port = 8082 },
    @{ Name = "ranking-service"; ModuleDir = "ranking-service"; Port = 8083; MavenArguments = $rankingArguments },
    @{ Name = "ai-service"; ModuleDir = "ai-service"; Port = 8084 },
    @{ Name = "gateway-service"; ModuleDir = "gateway-service"; Port = 8080; MavenArguments = $gatewayArguments }
)

$started = New-Object System.Collections.Generic.List[object]
foreach ($service in $services) {
    $argsForService = @("spring-boot:run")
    if ($service.ContainsKey("MavenArguments")) {
        $argsForService = $service.MavenArguments
    }
    $started.Add((Start-ServiceModule -Name $service.Name -ModuleDir $service.ModuleDir -Port $service.Port -MavenArguments $argsForService))
}

$started | ConvertTo-Json -Depth 10 | Set-Content -Path $pidFile -Encoding UTF8

Write-Host ""
Write-Host "Service startup finished. PID file: $pidFile" -ForegroundColor Green
Write-Host "Logs: $logPath"
Write-Host "Next: powershell -ExecutionPolicy Bypass -File scripts\acceptance-gateway.ps1"
