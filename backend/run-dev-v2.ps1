param([int]$Port = 5082,[string]$BuildOutput)
$ErrorActionPreference = 'Stop'
if ($Port -lt 1 -or $Port -gt 65535) { throw 'Invalid API port.' }
$root = Split-Path $PSScriptRoot -Parent
$settings = @{}
foreach ($line in [IO.File]::ReadAllLines((Join-Path $root '.env.local'))) {
    if ($line.Trim() -eq '' -or $line.Trim().StartsWith('#')) { continue }
    $pair = $line.Split('=',2)
    if ($pair.Length -eq 2) { $settings[$pair[0].Trim()] = $pair[1] }
}
if ($settings.DB_HOST -notin @('localhost','127.0.0.1','::1') -or $settings.DB_NAME -cne 'vietnam_explorer_dev_v2') {
    throw 'This launcher only allows the new local vietnam_explorer_dev_v2 database.'
}
if ([string]::IsNullOrEmpty($settings.DB_PASSWORD)) { throw 'Configure DB_PASSWORD in .env.local.' }
. (Join-Path $PSScriptRoot 'local-config.ps1')
$config = [pscustomobject]@{DbHost=$settings.DB_HOST;DbPort=[int]$settings.DB_PORT;DbName=$settings.DB_NAME;DbUser=$settings.DB_USER}
$buildArgs=@('build',(Join-Path $PSScriptRoot 'Bcv.Api/Bcv.Api.csproj'),'--no-restore','--verbosity','quiet','-p:UseAppHost=false')
$apiAssembly=Join-Path $PSScriptRoot 'Bcv.Api/bin/Debug/net10.0/Bcv.Api.dll'
if($BuildOutput){
    $buildPath=[IO.Path]::GetFullPath((Join-Path $root $BuildOutput))
    $allowed=[IO.Path]::GetFullPath((Join-Path $root 'output'))+[IO.Path]::DirectorySeparatorChar
    if(!$buildPath.StartsWith($allowed,[StringComparison]::OrdinalIgnoreCase)){throw 'BuildOutput must stay inside workspace output.'}
    $buildArgs+=@('-o',$buildPath);$apiAssembly=Join-Path $buildPath 'Bcv.Api.dll'
}
& dotnet @buildArgs
if ($LASTEXITCODE -ne 0) { throw 'API build failed.' }
$overrides = @{
    ConnectionStrings__Bcv = (New-BcvConnectionString $config $settings.DB_PASSWORD)
    Database__ApplyStartupMigrations = 'false'
    Media__MaintenanceEnabled = 'false'
    Sessions__FilePath = (Join-Path $root 'output/dev-v2/sessions.json')
    Smtp__Host = ''
    R2__AccountId = ''; R2__Bucket = ''; R2__AccessKeyId = ''; R2__SecretAccessKey = ''; R2__PublicBaseUrl = ''
    PORT = ''
    ASPNETCORE_ENVIRONMENT = 'Development'
}
$previous = @{}
$androidPortJob = $null
try {
    foreach ($name in $overrides.Keys) {
        $previous[$name] = [Environment]::GetEnvironmentVariable($name,'Process')
        [Environment]::SetEnvironmentVariable($name,$overrides[$name],'Process')
    }
    Write-Output "Local API v2: http://127.0.0.1:$Port; database vietnam_explorer_dev_v2; migrations/cloud storage/mail disabled."
    Write-Output 'Startup migrations remain disabled; schema is managed only through approved SQL. Device/Health Connect verification is separate from API health.'
    $adb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
    if (Test-Path -LiteralPath $adb) {
        # localhost in the development APK reaches this API through adb reverse.
        # Emulator restarts discard mappings, so maintain them for this API session.
        $androidPortJob = Start-Job -ArgumentList $adb,$Port -ScriptBlock {
            param($adbPath, $apiPort)
            while ($true) {
                $devices = & $adbPath devices 2>$null
                foreach ($line in $devices) {
                    if ($line -match '^(emulator-\d+)\s+device$') {
                        $serial = $Matches[1]
                        $mapping = & $adbPath -s $serial reverse --list 2>$null
                        if (-not ($mapping -match "tcp:$apiPort\s+tcp:$apiPort(?:\s|$)")) {
                            & $adbPath -s $serial reverse "tcp:$apiPort" "tcp:$apiPort" 2>$null | Out-Null
                        }
                    }
                }
                Start-Sleep -Seconds 3
            }
        }
        Write-Output "Android emulator port forwarding is maintained on tcp:$Port while this launcher runs."
    }
    & dotnet $apiAssembly --urls "http://0.0.0.0:$Port"
}
finally {
    if ($null -ne $androidPortJob) {
        Stop-Job -Job $androidPortJob
        Remove-Job -Job $androidPortJob
    }
    foreach ($name in $overrides.Keys) { [Environment]::SetEnvironmentVariable($name,$previous[$name],'Process') }
    $settings.DB_PASSWORD = $null
    $overrides.ConnectionStrings__Bcv = $null
}
