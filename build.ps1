param([switch]$DebugApk)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$taskStudioJdk = Join-Path $env:ProgramFiles 'Android/Android Studio/jbr'
if (Test-Path -LiteralPath $taskStudioJdk) { $env:JAVA_HOME = $taskStudioJdk }
if (-not (Test-Path -LiteralPath 'local.properties')) {
    $taskSdk = Join-Path $env:LOCALAPPDATA 'Android/Sdk'
    if (-not (Test-Path -LiteralPath $taskSdk)) { throw 'Android SDK not found. Open this project in Android Studio first.' }
    $taskSdkProperty = $taskSdk.Replace('\','/').Replace(':','\:')
    [IO.File]::WriteAllText((Join-Path $PSScriptRoot 'local.properties'), "sdk.dir=$taskSdkProperty`n")
}
# A full Windows path avoids JDK local socket failures with 8.3 TEMP aliases.
$taskJavaTemp = (Join-Path $env:LOCALAPPDATA 'Temp').Replace('\','/')
$taskOldJavaOptions = $env:JAVA_TOOL_OPTIONS
try {
    $env:JAVA_TOOL_OPTIONS = "$taskOldJavaOptions -Djdk.net.unixdomain.tmpdir=$taskJavaTemp".Trim()
    $taskBuild = if ($DebugApk) { ':app:assembleDebug' } else { ':app:assembleRelease' }
    & "$PSScriptRoot/gradlew.bat" --no-daemon $taskBuild ':app:lintDebug'
    if ($LASTEXITCODE -ne 0) { throw 'Android build failed. See the errors above.' }
    Write-Host 'Build complete. APK: app/build/outputs/apk/'
} finally { $env:JAVA_TOOL_OPTIONS = $taskOldJavaOptions }
