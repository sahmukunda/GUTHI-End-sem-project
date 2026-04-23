# Restores mongo-snapshots/bson-dump/ (expects bson-dump/guthi/*.bson). Uses --drop.
# Host: mongorestore on PATH. Else: Docker service mongo.
# Optional: MONGODB_URI. Skip Docker: GUTHI_NO_DOCKER=1

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "mongo-tool-path.ps1")
$guthiRoot = Split-Path $PSScriptRoot -Parent
$dumpDir = Join-Path $guthiRoot "mongo-snapshots\bson-dump"
$guthiNs = Join-Path $dumpDir "guthi"
$toolsUrl = "https://www.mongodb.com/try/download/database-tools"

if (-not (Test-Path $guthiNs)) {
  Write-Host "Missing BSON dump: $guthiNs" -ForegroundColor Red
  Write-Host "Run scripts/mongo-dump.ps1 first, or copy a bson-dump folder (with guthi inside) into mongo-snapshots\."
  exit 1
}

$mongorestoreExe = Get-MongoDatabaseToolPath -Name "mongorestore"
if ($mongorestoreExe) {
  $uri = if ($env:MONGODB_URI) { $env:MONGODB_URI } else { "mongodb://localhost:27017" }
  Write-Host "$mongorestoreExe --drop --uri ... $dumpDir"
  & $mongorestoreExe --uri="$uri" --drop $dumpDir
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
  Write-Host "Done."
  exit 0
}

if ($env:GUTHI_NO_DOCKER -eq "1") {
  Write-Host "mongorestore not found and GUTHI_NO_DOCKER=1." -ForegroundColor Yellow
  Write-Host "Install MongoDB Database Tools or set MONGODB_TOOLS_BIN. $toolsUrl"
  exit 1
}

function Test-DockerEngineRunning {
  if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    return $false
  }
  $null = & docker info 2>&1
  return $LASTEXITCODE -eq 0
}

if (-not (Test-DockerEngineRunning)) {
  Write-Host "mongorestore not found; Docker engine is not running (or docker.exe missing)." -ForegroundColor Red
  Write-Host "Install MongoDB Database Tools, then add Tools bin to PATH or set MONGODB_TOOLS_BIN." -ForegroundColor Yellow
  Write-Host "  $toolsUrl"
  exit 1
}

Write-Host "mongorestore not on PATH; using Docker service mongo..."

$remote = "/tmp/guthi-bson-restore"
Push-Location $guthiRoot
try {
  docker compose exec -T mongo sh -c "rm -rf '$remote'" 2>$null
  docker compose cp $dumpDir "mongo:${remote}"
  if ($LASTEXITCODE -ne 0) {
    Write-Host "docker compose cp failed. Start Mongo: cd `"$guthiRoot`"; docker compose up -d" -ForegroundColor Red
    exit 1
  }
  docker compose exec -T mongo mongorestore --drop $remote
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
  docker compose exec -T mongo sh -c "rm -rf '$remote'" 2>$null
} finally {
  Pop-Location
}
Write-Host "Done."
