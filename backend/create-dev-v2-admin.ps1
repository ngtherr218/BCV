$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$settings=@{}
foreach($line in [IO.File]::ReadAllLines((Join-Path $root '.env.local'))){
    if($line.Trim() -eq '' -or $line.Trim().StartsWith('#')){continue}
    $pair=$line.Split('=',2);if($pair.Length -eq 2){$settings[$pair[0].Trim()]=$pair[1]}
}
if($settings.DB_HOST -notin @('localhost','127.0.0.1','::1') -or $settings.DB_NAME -cne 'vietnam_explorer_dev_v2'){throw 'Only local v2 is allowed.'}
$credentials=Join-Path $root 'output/dev-v2/admin-credentials.json'
if(Test-Path -LiteralPath $credentials){Write-Output 'Local admin credentials already exist; no account changes made.';exit}
. (Join-Path $PSScriptRoot 'local-config.ps1')
$config=[pscustomobject]@{DbHost=$settings.DB_HOST;DbPort=[int]$settings.DB_PORT;DbName=$settings.DB_NAME;DbUser=$settings.DB_USER}
$previousConnection=$env:ConnectionStrings__Bcv
$previousPassword=$env:BCV_ADMIN_PASSWORD
try{
    $env:ConnectionStrings__Bcv=New-BcvConnectionString $config $settings.DB_PASSWORD
    $password='V2Admin-'+[Guid]::NewGuid().ToString('N')+'!'
    $env:BCV_ADMIN_PASSWORD=$password
    & dotnet (Join-Path $PSScriptRoot 'Bcv.Api/bin/Debug/net10.0/Bcv.Api.dll') --create-admin v2-admin@example.invalid
    if($LASTEXITCODE -ne 0){throw 'Admin creation failed; existing account was not changed.'}
    [IO.File]::WriteAllText($credentials,(@{email='v2-admin@example.invalid';password=$password;api='http://127.0.0.1:5082'}|ConvertTo-Json))
    Write-Output 'Created local v2 administrator. Credentials saved in ignored output/dev-v2/admin-credentials.json; no password logged.'
}finally{
    $env:ConnectionStrings__Bcv=$previousConnection;$env:BCV_ADMIN_PASSWORD=$previousPassword
    $password=$null;$settings.DB_PASSWORD=$null
}
