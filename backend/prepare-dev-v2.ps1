param([string]$PostgresBin = 'E:\PostgreSQL\bin')
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$settings = @{}
foreach ($line in [IO.File]::ReadAllLines((Join-Path $root '.env.local'))) {
    if ($line.Trim() -eq '' -or $line.Trim().StartsWith('#')) { continue }
    $pair = $line.Split('=', 2)
    if ($pair.Length -eq 2) { $settings[$pair[0].Trim()] = $pair[1] }
}
if ($settings.DB_HOST -notin @('localhost','127.0.0.1','::1')) { throw 'Only local loopback PostgreSQL is allowed.' }
if ($settings.DB_NAME -cne 'vietnam_explorer_dev_v2' -or $settings.DB_SOURCE_NAME -cne 'bcv') { throw 'Expected source bcv and target vietnam_explorer_dev_v2.' }
if ([string]::IsNullOrEmpty($settings.DB_PASSWORD)) { throw 'Configure DB_PASSWORD in .env.local.' }
$psql = Join-Path $PostgresBin 'psql.exe'
$dump = Join-Path $PostgresBin 'pg_dump.exe'
if (!(Test-Path -LiteralPath $psql) -or !(Test-Path -LiteralPath $dump)) { throw 'PostgreSQL client tools are missing.' }
$output = Join-Path $root 'docs/technical'
$schemaFile = Join-Path $output 'database_schema_before.sql'
$targetFile = Join-Path $output 'database_schema_after.sql'
$common = @('-X','-w','-h',$settings.DB_HOST,'-p',$settings.DB_PORT,'-U',$settings.DB_USER,'-v','ON_ERROR_STOP=1')
$previousPassword = $env:PGPASSWORD
$previousOptions = $env:PGOPTIONS
Push-Location -LiteralPath $output
try {
    $env:PGPASSWORD = $settings.DB_PASSWORD
    $env:PGOPTIONS = '-c default_transaction_read_only=on'
    $identity = & $psql @common -d bcv -Atc 'SELECT current_database()'
    if ($LASTEXITCODE -ne 0 -or $identity -cne 'bcv') { throw 'Source identity could not be verified.' }
    $exists = & $psql @common -d postgres -Atc "SELECT count(*) FROM pg_database WHERE datname='vietnam_explorer_dev_v2'"
    if ($LASTEXITCODE -ne 0 -or $exists -ne '0') { throw 'Target already exists or cannot be checked. No database was overwritten.' }
    & $dump -w -h $settings.DB_HOST -p $settings.DB_PORT -U $settings.DB_USER -d bcv --schema-only --no-owner --file=database_schema_before.sql
    if ($LASTEXITCODE -ne 0) { throw 'Read-only schema dump failed.' }
    # Only this constant CREATE DATABASE is executed against the maintenance database.
    $env:PGOPTIONS = ''
    & $psql @common -d postgres -c 'CREATE DATABASE vietnam_explorer_dev_v2 TEMPLATE template0'
    if ($LASTEXITCODE -ne 0) { throw 'Target creation failed. No source mutation was executed.' }
    $targetIdentity = & $psql @common -d vietnam_explorer_dev_v2 -Atc 'SELECT current_database()'
    if ($LASTEXITCODE -ne 0 -or $targetIdentity -cne 'vietnam_explorer_dev_v2') { throw 'Target identity mismatch; restore stopped.' }
    # A single transaction restores the existing structure, with no user records or new feature schema.
    & $psql @common -d vietnam_explorer_dev_v2 --single-transaction -f database_schema_before.sql
    if ($LASTEXITCODE -ne 0) { throw 'Restore failed; schema transaction rolled back. Target is retained for inspection.' }
    $env:PGOPTIONS = '-c default_transaction_read_only=on'
    & $dump -w -h $settings.DB_HOST -p $settings.DB_PORT -U $settings.DB_USER -d vietnam_explorer_dev_v2 --schema-only --no-owner --file=database_schema_after.sql
    if ($LASTEXITCODE -ne 0) { throw 'Target schema dump failed.' }
    function Get-CanonicalSchema([string]$path) {
        # pg_dump generates fresh psql safety tokens for each invocation.
        $text=([IO.File]::ReadAllLines($path) | Where-Object { $_ -notmatch '^\\(un)?restrict ' }) -join "`n"
        # Same known PostgreSQL array-cast deparser round trip documented in schema_comparison_results.md.
        return $text.Replace("ANY ((ARRAY['IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'ABANDONED'::character varying])::text[])","ANY (ARRAY[('IN_PROGRESS'::character varying)::text, ('COMPLETED'::character varying)::text, ('ABANDONED'::character varying)::text])")
    }
    if ((Get-CanonicalSchema $schemaFile) -cne (Get-CanonicalSchema $targetFile)) { throw 'Schema dump comparison differs; inspect before proceeding.' }
    Write-Output 'Created vietnam_explorer_dev_v2 with the unchanged source schema. Schema-only dump comparison matches. No business data copied.'
}
finally {
    Pop-Location
    $env:PGPASSWORD = $previousPassword
    $env:PGOPTIONS = $previousOptions
    $settings.DB_PASSWORD = $null
}
