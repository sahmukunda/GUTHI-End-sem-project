# Dumps database `guthi` to mongo-snapshots/bson-dump/ (layout: bson-dump/guthi/*.bson).
# Host: mongodump on PATH. Else: Docker service mongo.
# Optional: MONGODB_URI (e.g. mongodb://localhost:27017). Skip Docker: GUTHI_NO_DOCKER=1

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "mongo-tool-path.ps1")
$guthiRoot = Split-Path $PSScriptRoot -Parent
$snapRoot = Join-Path $guthiRoot "mongo-snapshots"
$dumpOut = Join-Path $snapRoot "bson-dump"
$toolsUrl = "https://www.mongodb.com/try/download/database-tools"

New-Item -ItemType Directory -Force -Path $snapRoot | Out-Null
$stale = Join-Path $snapRoot "guthi-bson-dump"
if (Test-Path $dumpOut) { Remove-Item -Recurse -Force $dumpOut }
if (Test-Path $stale) { Remove-Item -Recurse -Force $stale }

function Invoke-HostDump {
  param([string]$Uri, [string]$MongodumpExe)
  Write-Host "$MongodumpExe --uri ... --db guthi --out $dumpOut"
  Write-Host "(URI: $Uri)"
  & $MongodumpExe --uri="$Uri" --db=guthi --out=$dumpOut
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$mongodumpExe = Get-MongoDatabaseToolPath -Name "mongodump"
if ($mongodumpExe) {
  $uri = if ($env:MONGODB_URI) { $env:MONGODB_URI } else { "mongodb://localhost:27017" }
  Invoke-HostDump -Uri $uri -MongodumpExe $mongodumpExe
  Write-Host "Done. Data under: $dumpOut\guthi\"
  exit 0
}

if ($env:GUTHI_NO_DOCKER -eq "1") {
  Write-Host "mongodump not found and GUTHI_NO_DOCKER=1." -ForegroundColor Yellow
  Write-Host "Install MongoDB Database Tools, add its bin to PATH, or set MONGODB_TOOLS_BIN / MONGODB_BIN." -ForegroundColor Yellow
  Write-Host "  $toolsUrl"
  exit 1
}

function Test-DockerEngineRunning {
  if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    return $false
  }
  $null = & docker info 2>&1
  return $LASTEXITCODE -eq 0
}

function Write-MongodumpMissingHelp {
  Write-Host ""
  Write-Host "mongodump.exe was not found (PATH, MONGODB_TOOLS_BIN, MONGODB_HOME\bin, D:\MongoDB\bin, Program Files\MongoDB\Tools)." -ForegroundColor Yellow
  if (Test-Path -LiteralPath "D:\MongoDB\bin\mongod.exe") {
    Write-Host "You have MongoDB server under D:\MongoDB - mongodump is usually NOT in that folder." -ForegroundColor Yellow
    Write-Host "Install MongoDB Database Tools (separate download), then either:" -ForegroundColor Yellow
    Write-Host '  $env:MONGODB_TOOLS_BIN = "C:\Program Files\MongoDB\Tools\100\bin"   # version folder may differ' -ForegroundColor Gray
    Write-Host "or add that Tools bin folder to your user PATH." -ForegroundColor Gray
  }
  Write-Host ""
  Write-Host "Download: $toolsUrl"
  Write-Host ""
  Write-Host "Optional: use Docker only if Docker Desktop is running, then: cd guthi; docker compose up -d"
  Write-Host "Or skip Docker attempts: `$env:GUTHI_NO_DOCKER = '1'"
}

if (-not (Test-DockerEngineRunning)) {
  Write-Host "mongodump not found; Docker engine is not running (or docker.exe missing)." -ForegroundColor Red
  Write-MongodumpMissingHelp
  exit 1
}

Write-Host "mongodump not on PATH; using Docker service mongo..."

$remoteParent = "/tmp/guthi-bson-dump"
Push-Location $guthiRoot
try {
  docker compose exec -T mongo sh -c "rm -rf '$remoteParent' && mkdir -p '$remoteParent'"
  docker compose exec -T mongo mongodump --db=guthi --out=$remoteParent
  if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker mongodump failed. Try: cd `"$guthiRoot`"; docker compose up -d" -ForegroundColor Red
    Write-Host "Or install Database Tools: $toolsUrl"
    exit 1
  }
  # Container has $remoteParent/guthi/ - copy whole tree to host bson-dump
  docker compose cp "mongo:${remoteParent}" $snapRoot
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
  docker compose exec -T mongo sh -c "rm -rf '$remoteParent'" 2>$null
  # docker names copied folder guthi-bson-dump; rename to bson-dump for restore script
  $copied = Join-Path $snapRoot "guthi-bson-dump"
  if (Test-Path $copied) {
    if (Test-Path $dumpOut) { Remove-Item -Recurse -Force $dumpOut }
    Rename-Item -Path $copied -NewName "bson-dump"
  }
} finally {
  Pop-Location
}
Write-Host "Done. Data under: $dumpOut\guthi\"
