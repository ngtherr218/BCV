param([string]$Psql = '')
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'local-config.ps1')
$config = Get-BcvLocalConfig
$psqlPath = Resolve-BcvPsql $config $Psql
$tools = Split-Path $psqlPath
$root = Split-Path $PSScriptRoot
$output = Join-Path $root 'output/step-tests'
New-Item -ItemType Directory -Force $output | Out-Null
$buildOutput = Join-Path $root 'backend-build-out/step-flow-checks'
& dotnet build (Join-Path $PSScriptRoot 'Bcv.StepChecks/Bcv.StepChecks.csproj') --no-restore -p:UseAppHost=false -o $buildOutput --verbosity quiet
if ($LASTEXITCODE -ne 0) { throw 'Build failed.' }
$database = 'bcv_step_checks_' + [guid]::NewGuid().ToString('N').Substring(0, 12)
$created = $false
$previousPassword = $env:PGPASSWORD
$previousCheck = $env:BCV_STEP_CHECK_CONNECTION
$env:PGPASSWORD = Get-BcvEnvironment 'BCV_DB_PASSWORD'
if (!$env:PGPASSWORD) { throw 'Set BCV_DB_PASSWORD before running step checks.' }
$connectionArguments = @('-h', $config.DbHost, '-p', $config.DbPort, '-U', $config.DbUser)
Push-Location $root
try {
    & (Join-Path $tools 'pg_dump.exe') @connectionArguments -Fc --no-owner --no-privileges -d $config.DbName -f 'output/step-tests/local-fixture.dump'
    if ($LASTEXITCODE -ne 0) { throw 'Local backup failed.' }
    & (Join-Path $tools 'createdb.exe') @connectionArguments $database
    if ($LASTEXITCODE -ne 0) { throw 'Isolated database creation failed.' }
    $created = $true
    & (Join-Path $tools 'pg_restore.exe') @connectionArguments --no-owner --no-privileges --exit-on-error -d $database 'output/step-tests/local-fixture.dump'
    if ($LASTEXITCODE -ne 0) { throw 'Fixture restore failed.' }
    $config.DbName = $database
    $env:BCV_STEP_CHECK_CONNECTION = New-BcvConnectionString $config $env:PGPASSWORD
    & dotnet (Join-Path $buildOutput 'Bcv.StepChecks.dll')
    if ($LASTEXITCODE -ne 0) { throw 'Step checks failed.' }
} finally {
    if ($created -and $database -match '^bcv_step_checks_[0-9a-f]{12}$') {
        & (Join-Path $tools 'dropdb.exe') @connectionArguments $database
        if ($LASTEXITCODE -ne 0) { Write-Warning "Could not remove isolated test database: $database" }
    }
    $env:PGPASSWORD = $previousPassword
    $env:BCV_STEP_CHECK_CONNECTION = $previousCheck
    Pop-Location
}
