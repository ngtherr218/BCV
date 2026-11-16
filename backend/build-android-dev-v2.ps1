param(
    [string]$Jdk='E:/AndroidStudio/jbr',
    [string]$BuildRoot=(Join-Path ([IO.Path]::GetTempPath()) 'bcv-dev-v2-android-build'),
    [string]$Api='http://127.0.0.1:5082/'
)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$uri=[Uri]$Api
if($uri.Scheme -ne 'http' -or $uri.Port -ne 5082 -or $uri.Host -notmatch '^(localhost|127\.0\.0\.1|10\.[0-9.]+|192\.168\.[0-9.]+|172\.(1[6-9]|2[0-9]|3[01])\.[0-9.]+)$') { throw 'Use a local emulator/LAN API on port 5082.' }
$java=Join-Path $Jdk 'bin/java.exe'
if(!(Test-Path -LiteralPath $java)){throw 'Pass -Jdk with the installed Android Studio JDK path.'}
$BuildRoot=[IO.Path]::GetFullPath($BuildRoot).Replace('\','/')
if($BuildRoot -match '[^\x00-\x7F]' -or $BuildRoot.Contains("'")){throw 'BuildRoot must be an ASCII directory without single quotes.'}
$output=Join-Path $root 'output/dev-v2'
New-Item -ItemType Directory -Path $output -Force|Out-Null
$init="gradle.beforeProject { project -> project.layout.buildDirectory.set(new File('$BuildRoot', project.name)) }`n"
[IO.File]::WriteAllText((Join-Path $output 'android-v2.init.gradle'),$init)
$previous=$env:JAVA_HOME
Push-Location (Join-Path $root 'android')
try{
    $env:JAVA_HOME=$Jdk
    & $java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:testDebugUnitTest :app:assembleDebug "-PbcvApiBaseUrl=$Api" --offline -I ../output/dev-v2/android-v2.init.gradle
    if($LASTEXITCODE -ne 0){throw 'Android build/test failed. Offline mode requires installed SDK and cached dependencies.'}
    Copy-Item -LiteralPath (Join-Path $BuildRoot 'app/outputs/apk/debug/app-debug.apk') -Destination (Join-Path $output 'bcv-local-v2-debug.apk')
    Write-Output 'Local APK: output/dev-v2/bcv-local-v2-debug.apk'
}finally{Pop-Location;$env:JAVA_HOME=$previous}
