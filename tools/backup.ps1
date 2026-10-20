# Saves the package lists, getprop, all settings, HOME handlers and a screenshot to backup\<timestamp>.
# Run this BEFORE changing anything; tools\restore.ps1 uses it to know what was enabled originally.
# Usage: powershell -ExecutionPolicy Bypass -File tools\backup.ps1 -Tv <tv-ip>
param(
    [Parameter(Mandatory = $true)][string]$Tv,
    [string]$Adb = "C:\temp\platform-tools\adb.exe"
)
$root = Split-Path -Parent $PSScriptRoot
$s = "${Tv}:5555"
& $Adb connect $s | Out-Null
$b = "$root\backup\$(Get-Date -Format yyyyMMdd-HHmmss)"
New-Item -ItemType Directory -Force $b | Out-Null

& $Adb -s $s shell pm list packages -f -u | Out-File -Encoding utf8 "$b\packages-all.txt"
& $Adb -s $s shell pm list packages -d | Out-File -Encoding utf8 "$b\packages-disabled.txt"
& $Adb -s $s shell pm list packages -e | Out-File -Encoding utf8 "$b\packages-enabled.txt"
& $Adb -s $s shell pm list packages -s | Out-File -Encoding utf8 "$b\packages-system.txt"
& $Adb -s $s shell pm list packages -3 | Out-File -Encoding utf8 "$b\packages-thirdparty.txt"
& $Adb -s $s shell getprop | Out-File -Encoding utf8 "$b\getprop.txt"
foreach ($ns in 'global', 'secure', 'system') {
    & $Adb -s $s shell settings list $ns | Out-File -Encoding utf8 "$b\settings-$ns.txt"
}
& $Adb -s $s shell "cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME" | Out-File -Encoding utf8 "$b\home-activities.txt"
& $Adb -s $s shell "cmd wifi list-networks" | Out-File -Encoding utf8 "$b\wifi-networks.txt"
& $Adb -s $s shell screencap -p /sdcard/shot.png
& $Adb -s $s pull /sdcard/shot.png "$b\before.png" | Out-Null
Write-Host "Backup saved to $b"
