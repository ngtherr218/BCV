param([switch]$ReadOnlyFilters,[switch]$UiPreview)
$ErrorActionPreference='Stop'
$settings=@{}
foreach($line in [IO.File]::ReadAllLines((Join-Path $PSScriptRoot '../.env.local'))) {
 if($line.Trim() -eq '' -or $line.Trim().StartsWith('#')){continue}
 $pair=$line.Split('=',2);if($pair.Length -eq 2){$settings[$pair[0].Trim()]=$pair[1]}
}
if($settings.DB_HOST -notin @('localhost','127.0.0.1','::1') -or $settings.DB_NAME -cne 'vietnam_explorer_dev_v2'){throw 'Only local v2 database allowed; never bcv.'}
. (Join-Path $PSScriptRoot 'local-config.ps1')
$previous=$env:BCV_CHECK_CONNECTION
try{
 $config=[pscustomobject]@{DbHost=$settings.DB_HOST;DbPort=[int]$settings.DB_PORT;DbName=$settings.DB_NAME;DbUser=$settings.DB_USER}
 $env:BCV_CHECK_CONNECTION=New-BcvConnectionString $config $settings.DB_PASSWORD
 Push-Location (Split-Path $PSScriptRoot -Parent)
 try { dotnet build backend/Bcv.QuizChecks --no-restore -o output/culture-quiz-checks; if($LASTEXITCODE -ne 0){throw 'Quiz build failed.'}; $checkArgs=@();if($UiPreview){$checkArgs+='--ui-preview'};if($ReadOnlyFilters){$checkArgs+= '--live-question-filters'}; dotnet output/culture-quiz-checks/Bcv.QuizChecks.dll @checkArgs; if($LASTEXITCODE -ne 0){throw 'Quiz checks failed.'} } finally {Pop-Location}
}finally{$env:BCV_CHECK_CONNECTION=$previous;$settings.DB_PASSWORD=$null}
