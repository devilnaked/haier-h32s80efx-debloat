# Re-enables every package that is disabled now but was enabled in the backup,
# and puts the stock Google TV launcher back as home.
# Usage: powershell -ExecutionPolicy Bypass -File tools\restore.ps1 -Tv <tv-ip> [-Backup backup\20261020-095205]
param(
    [Parameter(Mandatory = $true)][string]$Tv,
    [string]$Backup = "",
    [string]$Adb = "C:\temp\platform-tools\adb.exe"
)
$root = Split-Path -Parent $PSScriptRoot
if (-not $Backup) { $Backup = (Get-ChildItem "$root\backup" -Directory | Sort-Object Name | Select-Object -First 1).FullName }
if (-not $Backup -or -not (Test-Path "$Backup\packages-disabled.txt")) { throw "No backup found: run tools\backup.ps1 first, or pass -Backup" }
$s = "${Tv}:5555"
& $Adb connect $s | Out-Null

$wasDisabled = Get-Content "$Backup\packages-disabled.txt" | ForEach-Object { $_ -replace '^package:', '' } | Where-Object { $_ }
$nowDisabled = & $Adb -s $s shell pm list packages -d | ForEach-Object { $_.Trim() -replace '^package:', '' } | Where-Object { $_ }

foreach ($pkg in $nowDisabled) {
    if ($wasDisabled -contains $pkg) { continue }
    # one pm call per shell invocation: chained pm commands can stop after the first
    $out = & $Adb -s $s shell pm enable --user 0 $pkg
    Write-Host "$pkg -> $out"
}

# Private DNS back to the Android default
& $Adb -s $s shell settings put global private_dns_mode opportunistic

# Stock launcher back as home
& $Adb -s $s shell cmd package set-home-activity com.google.android.apps.tv.launcherx/.home.HomeActivity
Write-Host "Done. To remove the custom launcher too: adb -s $s uninstall com.local.atvhome"
