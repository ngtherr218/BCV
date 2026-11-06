param([string]$ConfigPath = '')
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'local-config.ps1')
$config = Get-BcvLocalConfig $ConfigPath
if ($config.DbHost -notin @('127.0.0.1','localhost','::1')) { throw 'These checks require local PostgreSQL.' }
$password = Get-BcvEnvironment 'BCV_DB_PASSWORD'
if (!$password) { throw 'Set BCV_DB_PASSWORD before running local checks.' }
$previous = $env:BCV_CHECK_CONNECTION
try {
    $env:BCV_CHECK_CONNECTION = New-BcvConnectionString $config $password
    dotnet run --project (Join-Path $PSScriptRoot 'Bcv.AdminDeleteChecks') --no-restore
    if ($LASTEXITCODE -ne 0) { throw 'Admin deletion integration checks failed.' }
} finally {
    $env:BCV_CHECK_CONNECTION = $previous
    $password = $null
}
