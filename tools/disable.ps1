# Disables packages one pm call per shell invocation, then goes Home and saves a screenshot.
# Usage: tools\disable.ps1 -Name batch1 -Packages pkg1,pkg2,...
[CmdletBinding(PositionalBinding = $false)]
param(
    [Parameter(Mandatory = $true)][string[]]$Packages,
    [string]$Name = "batch",
    [Parameter(Mandatory = $true)][string]$Tv,
    [string]$Adb = "C:\temp\platform-tools\adb.exe"
)
$s = "${Tv}:5555"
$root = Split-Path -Parent $PSScriptRoot
New-Item -ItemType Directory -Force "$root\shots" | Out-Null
foreach ($pkg in $Packages) {
    $out = & $Adb -s $s shell pm disable-user --user 0 $pkg
    Write-Host "$pkg -> $out"
}
& $Adb -s $s shell input keyevent KEYCODE_HOME
Start-Sleep -Seconds 4
& $Adb -s $s shell screencap -p /sdcard/shot.png
& $Adb -s $s pull /sdcard/shot.png "$root\shots\$Name.png" | Out-Null
& $Adb -s $s shell "dumpsys activity activities | grep topResumedActivity"
