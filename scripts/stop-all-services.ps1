param(
    [string]$PidFile = "docs/acceptance/runtime/service-pids.json",
    [int[]]$Ports = @(8080, 8081, 8082, 8083, 8084)
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$fullPidFile = Join-Path $root $PidFile

if (-not (Test-Path $fullPidFile)) {
    Write-Host "PID file not found: $fullPidFile" -ForegroundColor Yellow
    exit 0
}

$services = Get-Content -Raw $fullPidFile | ConvertFrom-Json
foreach ($service in $services) {
    $pidValue = [int]$service.pid
    $name = [string]$service.name
    $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        Write-Host "[SKIP] $name pid $pidValue is not running" -ForegroundColor Yellow
        continue
    }
    Write-Host "Stopping $name pid $pidValue ..." -ForegroundColor Cyan
    Stop-Process -Id $pidValue -Force
}

foreach ($port in $Ports) {
    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        $pidValue = [int]$connection.OwningProcess
        $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
        if ($null -eq $process) {
            continue
        }
        Write-Host "Stopping process on port $port pid $pidValue ($($process.ProcessName)) ..." -ForegroundColor Cyan
        Stop-Process -Id $pidValue -Force
    }
}

Write-Host "Services stopped." -ForegroundColor Green
