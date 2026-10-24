# Builds the launcher (debug APK), installs it on the TV and makes it the default home.
# Usage: powershell -ExecutionPolicy Bypass -File tools\build-install.ps1 -Tv <tv-ip> [-NoInstall]
param(
    [Parameter(Mandatory = $true)][string]$Tv,
    [string]$Adb = "C:\temp\platform-tools\adb.exe",
    [string]$Tools = "C:\temp\build-tools",
    [switch]$NoInstall
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$proj = "$root\launcher"

$env:JAVA_HOME = (Get-ChildItem "$Tools\jdk17" -Directory | Select-Object -First 1).FullName
$env:ANDROID_HOME = "$Tools\android-sdk"
$gradle = (Get-ChildItem "$Tools\gradle" -Directory | Select-Object -First 1).FullName + "\bin\gradle.bat"
"sdk.dir=$($env:ANDROID_HOME -replace '\\', '\\')" | Out-File -Encoding ascii "$proj\local.properties"

# Build output and Gradle's project cache go outside the project: a synced folder (OneDrive)
# locks files while Gradle rewrites them and the build fails.
$out = "$Tools\atvhome-build"
$env:ATVHOME_BUILD_DIR = $out

Push-Location $proj
try { & $gradle --no-daemon -q --project-cache-dir "$out\.gradle" assembleDebug; if ($LASTEXITCODE -ne 0) { throw "Gradle build failed" } }
finally { Pop-Location }
$apk = "$out\app\outputs\apk\debug\app-debug.apk"
Write-Host "Built $apk ($([math]::Round((Get-Item $apk).Length / 1KB)) KB)"
if ($NoInstall) { return }

$s = "${Tv}:5555"
& $Adb connect $s | Out-Null
# adb install can hang on Android 14 waiting for an on-screen prompt: push + pm install instead
& $Adb -s $s push $apk /data/local/tmp/atvhome.apk | Out-Null
$out = & $Adb -s $s shell pm install -r /data/local/tmp/atvhome.apk
Write-Host "pm install: $out"
if ($out -notmatch "Success") { throw "Install failed: $out" }
& $Adb -s $s shell rm /data/local/tmp/atvhome.apk
# lets Android reveal the Wi-Fi network name to the Network pill
& $Adb -s $s shell pm grant com.local.atvhome android.permission.ACCESS_FINE_LOCATION
& $Adb -s $s shell cmd package set-home-activity com.local.atvhome/.MainActivity
& $Adb -s $s shell am force-stop com.local.atvhome
& $Adb -s $s shell input keyevent KEYCODE_HOME
Write-Host "Installed and set as home."
