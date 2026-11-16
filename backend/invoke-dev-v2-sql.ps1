param([Parameter(Mandatory)][string]$SqlFile)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$settings = @{}
foreach ($line in [IO.File]::ReadAllLines((Join-Path $root '.env.local'))) {
    if ($line.Trim() -eq '' -or $line.Trim().StartsWith('#')) { continue }
    $pair = $line.Split('=',2)
    if ($pair.Length -eq 2) { $settings[$pair[0].Trim()] = $pair[1] }
}
if ($settings.DB_HOST -notin @('localhost','127.0.0.1','::1') -or $settings.DB_NAME -cne 'vietnam_explorer_dev_v2') { throw 'Only the new local v2 database is allowed.' }
$path = (Resolve-Path -LiteralPath $SqlFile).Path
$previousPassword = $env:PGPASSWORD
Push-Location -LiteralPath (Split-Path $path -Parent)
try {
    $env:PGPASSWORD = $settings.DB_PASSWORD
    $common = @('-X','-w','-q','-h',$settings.DB_HOST,'-p',$settings.DB_PORT,'-U',$settings.DB_USER,'-d','vietnam_explorer_dev_v2','-v','ON_ERROR_STOP=1')
    $identity = & 'E:/PostgreSQL/bin/psql.exe' @common -Atc 'SELECT current_database()'
    if ($LASTEXITCODE -ne 0 -or $identity -cne 'vietnam_explorer_dev_v2') { throw 'Target verification failed.' }
    & 'E:/PostgreSQL/bin/psql.exe' @common -f (Split-Path $path -Leaf)
    if ($LASTEXITCODE -ne 0) { throw 'SQL failed; inspect transaction outcome before retrying.' }
}
finally {
    Pop-Location
    $env:PGPASSWORD = $previousPassword
    $settings.DB_PASSWORD = $null
}
