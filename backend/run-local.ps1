param([string]$ConfigPath = '')
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'local-config.ps1')
$config = Get-BcvLocalConfig $ConfigPath
$dotnet = Join-Path $env:USERPROFILE '.dotnet\dotnet.exe'
if (-not (Test-Path -LiteralPath $dotnet)) {
    $dotnet = (Get-Command dotnet -ErrorAction Stop).Source
}

& $dotnet build (Join-Path $PSScriptRoot 'Bcv.Api\Bcv.Api.csproj') --verbosity quiet -p:UseAppHost=false
if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }

# Secrets prefer process environment variables, then user-level environment variables (see the SMTP note below),
# otherwise they are asked for. Nothing is written to the repository.
function Get-Secret([string]$name, [string]$prompt) {
    $value = Get-BcvEnvironment $name
    if ($value) { return $value }
    $secure = Read-Host $prompt -AsSecureString
    return [System.Net.NetworkCredential]::new('', $secure).Password
}

$plain = Get-Secret 'BCV_DB_PASSWORD' 'PostgreSQL BCV password'
$env:ConnectionStrings__Bcv = New-BcvConnectionString $config $plain
$plain = $null

# Reverse geocoding stays on the backend; reuse the local Android REST key for development.
$originalGoongKey = $env:Goong__ApiKey
$goongKey = Get-BcvEnvironment 'Goong__ApiKey'
if (!$goongKey) { $goongKey = Get-BcvEnvironment 'BCV_GOONG_API_KEY' }
$goongDevFile = Join-Path $PSScriptRoot '../android/dev.properties'
if (!$goongKey -and (Test-Path -LiteralPath $goongDevFile)) {
    $goongMatch = [regex]::Match([IO.File]::ReadAllText($goongDevFile), '(?m)^bcvGoongApiKey=(.*)$')
    if ($goongMatch.Success) { $goongKey = $goongMatch.Groups[1].Value.Trim() }
}
if ($goongKey) { $env:Goong__ApiKey = $goongKey }
$goongKey = $null

# Password reset codes go out through Gmail SMTP. Leave the App Password empty (press Enter)
# and the API logs the code instead.
#
# To stop being asked, set the secrets once for your Windows account:
#   [Environment]::SetEnvironmentVariable('BCV_DB_PASSWORD', (Read-Host 'PostgreSQL password'), 'User')
#   [Environment]::SetEnvironmentVariable('BCV_SMTP_PASSWORD', (Read-Host 'Gmail App Password'), 'User')
$smtpUser = $config.SmtpUser
$smtpPassword = if ($smtpUser) { (Get-Secret 'BCV_SMTP_PASSWORD' "SMTP password for $smtpUser (Enter to log codes instead)") -replace '\s', '' } else { '' }
if ($smtpPassword) {
    $env:Smtp__Host = $config.SmtpHost
    $env:Smtp__Port = [string]$config.SmtpPort
    $env:Smtp__EnableSsl = ([string]$config.SmtpEnableSsl).ToLowerInvariant()
    $env:Smtp__User = $smtpUser
    $env:Smtp__From = if ($config.SmtpFrom) { $config.SmtpFrom } else { $smtpUser }
    $env:Smtp__Password = $smtpPassword
    Write-Host "Password reset codes will be emailed from $smtpUser."
}
else {
    Write-Host 'SMTP skipped: password reset codes will be written to this log.'
}
$smtpPassword = $null

# Images (avatars/, places/, cards/) are uploaded to Cloudflare R2 when all five BCV_R2_* values are set (backend\set-local-secrets.ps1);
# without them the API keeps images inline in the database. Never asked for here.
$r2 = @{
    AccountId = 'BCV_R2_ACCOUNT_ID'; Bucket = 'BCV_R2_BUCKET'; PublicBaseUrl = 'BCV_R2_PUBLIC_URL'
    AccessKeyId = 'BCV_R2_ACCESS_KEY_ID'; SecretAccessKey = 'BCV_R2_SECRET_ACCESS_KEY'
}
$r2Values = @{}
foreach ($entry in $r2.GetEnumerator()) {
    $value = Get-BcvEnvironment $entry.Value
    if ($value) { $r2Values[$entry.Key] = $value }
}
if ($r2Values.PublicBaseUrl -like '*.r2.cloudflarestorage.com*') {
    # That is the private S3 API endpoint; phones can only load images from the bucket's public URL.
    Write-Host 'BCV_R2_PUBLIC_URL is the S3 API endpoint, not the public r2.dev URL: images stay in the database.' -ForegroundColor Yellow
}
elseif ($r2Values.Count -eq $r2.Count) {
    foreach ($entry in $r2Values.GetEnumerator()) { Set-Item "Env:R2__$($entry.Key)" $entry.Value }
    Write-Host "Images will be stored in Cloudflare R2 bucket $($r2Values.Bucket) (avatars/, places/, cards/)."
}
else {
    Write-Host 'Cloudflare R2 not configured: images stay in the database.'
}
$r2Values = $null
try {
    Write-Host "Database: $($config.DbHost):$($config.DbPort)/$($config.DbName); API: $($config.ApiUrls)"
    & $dotnet (Join-Path $PSScriptRoot 'Bcv.Api\bin\Debug\net10.0\Bcv.Api.dll') --urls $config.ApiUrls
}
finally {
    if ($null -eq $originalGoongKey) { Remove-Item Env:Goong__ApiKey -ErrorAction SilentlyContinue }
    else { $env:Goong__ApiKey = $originalGoongKey }
    Remove-Item Env:ConnectionStrings__Bcv -ErrorAction SilentlyContinue
    'Host', 'Port', 'EnableSsl', 'User', 'From', 'Password' | ForEach-Object {
        Remove-Item "Env:Smtp__$_" -ErrorAction SilentlyContinue
    }
    $r2.Keys | ForEach-Object { Remove-Item "Env:R2__$_" -ErrorAction SilentlyContinue }
}
