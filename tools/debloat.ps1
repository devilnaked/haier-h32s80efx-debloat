# Applies the debloat used on a Haier H32S80EFX (Google TV 14, firmware UKNV.260302.001).
# Only manufacturer telemetry, content/ad providers and a few unused services; streaming apps
# are left alone (disable the ones you don't use yourself with tools\disable.ps1).
# Each batch goes Home and saves a screenshot to shots\ so you can check nothing broke.
# Usage: powershell -ExecutionPolicy Bypass -File tools\debloat.ps1 -Tv <tv-ip>
param(
    [Parameter(Mandatory = $true)][string]$Tv
)
$d = "$PSScriptRoot\disable.ps1"

# 1. Telemetry / IoT
& $d -Tv $Tv -Name batch1-telemetry -Packages it.hon.service, com.haierubic.usmartdevice, fusion.android.tv.demo, com.google.android.feedback
# 2. Manufacturer content and ads (Haier tab, partner rows, Kinopoisk promo)
& $d -Tv $Tv -Name batch2-content -Packages com.haier.oemtabprovider, com.mediatek.android.leanbacklauncher.partnercustomizer, com.haier.apps
# 3. Cleaner, e-manual, Play Games
& $d -Tv $Tv -Name batch3-apps -Packages com.haier.tvhousekeeper, com.haier.emanual, com.google.android.play.games

Write-Host "Check shots\batch*.png, then open Live TV and an app before going further."
