# Resolves mongodump / mongorestore when not on PATH.
#
# Checked in order:
#   1) PATH
#   2) MONGODB_TOOLS_BIN - Database Tools only (e.g. C:\Program Files\MongoDB\Tools\100\bin)
#   3) MONGODB_BIN - any folder that contains the exe (e.g. D:\MongoDB\bin for a custom server install)
#   4) MONGODB_HOME\bin - if you set MONGODB_HOME to the server root (e.g. D:\MongoDB)
#   5) C:\Program Files\MongoDB\Tools\**\bin (MSI Database Tools)

function Get-MongoDatabaseToolPath {
  param(
    [Parameter(Mandatory)]
    [ValidateSet("mongodump", "mongorestore")]
    [string]$Name
  )

  $exe = "$Name.exe"

  $fromPath = Get-Command $Name -ErrorAction SilentlyContinue
  if ($fromPath) {
    return $fromPath.Source
  }

  function Try-BinDir {
    param([string]$BinDir)
    if ([string]::IsNullOrWhiteSpace($BinDir)) {
      return $null
    }
    $d = $BinDir.TrimEnd("\")
    $withExe = Join-Path $d $exe
    if (Test-Path -LiteralPath $withExe) {
      return $withExe
    }
    return $null
  }

  $hit = Try-BinDir $env:MONGODB_TOOLS_BIN
  if ($hit) {
    return $hit
  }

  $hit = Try-BinDir $env:MONGODB_BIN
  if ($hit) {
    return $hit
  }

  if ($env:MONGODB_HOME) {
    $hit = Try-BinDir (Join-Path $env:MONGODB_HOME.TrimEnd("\") "bin")
    if ($hit) {
      return $hit
    }
  }

  # Common custom installs (no env var)
  foreach ($guess in @("D:\MongoDB\bin", "C:\MongoDB\bin")) {
    $hit = Try-BinDir $guess
    if ($hit) {
      return $hit
    }
  }

  $toolsRoot = Join-Path $env:ProgramFiles "MongoDB\Tools"
  if (Test-Path -LiteralPath $toolsRoot) {
    $hits = @(
      Get-ChildItem -Path $toolsRoot -Recurse -Filter $exe -ErrorAction SilentlyContinue |
        Where-Object { (Split-Path -Parent $_.FullName | Split-Path -Leaf) -eq "bin" }
    )
    if ($hits.Count -gt 0) {
      return ($hits | Sort-Object { $_.FullName } -Descending | Select-Object -First 1).FullName
    }
  }

  return $null
}
