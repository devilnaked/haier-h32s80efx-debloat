# Haier H32S80EFX (Google TV)

| | |
|---|---|
| Model | Haier H32S80EFX, `ro.product.model` = `Haier MatrixTV CE`, device `deto`, product `HRT56EU` |
| Android | 14 (SDK 34), Google TV, firmware `UKNV.260302.001`, `ro.adb.secure=1` |
| SoC / RAM | MediaTek (CVTE board), `armeabi-v7a`, 1.4 GB |
| Screen | 1920×1080 panel, UI at 1280×720, density 213 |
| Sideloading | **Works** – `pm install` accepts debug-signed APKs, including one that declares HOME |
| Custom launcher | **Works** – disable `launcherx` *and* `setupwraith` (both outrank `set-home-activity`); disabling only setupwraith's `RecoveryActivity` is refused |
| Wi-Fi caveat | A Wi-Fi network saved during first setup belongs to setupwraith and is deleted when it is disabled – check `cmd wifi list-networks` first |
| Live TV | Tuner inputs: `TunerInputService/HW0` (digital, DVB-T2), `CableInputService/HW0`, `AnalogInputService/HW1`. Open Live TV on the digital one |
| HDMI / AV | `HDMIInputService/HW5–HW7` = HDMI 1–3, `CompositeInputService/HW2` = AV. Explicitly target `com.mediatek.wwtv.tvcenter/.nav.TurnkeyUiMainActivity` – TrueID also claims channel VIEW intents |
| Screenshots | Empty during Live TV / HDMI (normal) |
| Tested | 2026-10-20, Thailand |

## All packages (151 system + 9 preinstalled apps)

Status of each package as tested. "Leave alone" means it was not checked closely and is not
needed for a debloat; Android/Google framework packages belong here.

| Package | Type | What to do |
|---|---|---|
| `android` | system | Leave alone |
| `android.auto_generated_rro_product__` | system | Leave alone |
| `android.auto_generated_rro_vendor__` | system | Leave alone |
| `com.ais.mimo.aisplay.tv` | preinstalled app | Your choice – streaming app |
| `com.amazon.amazonvideo.livingroom` | system | Your choice – Prime Video |
| `com.android.adservices.api` | system | Leave alone |
| `com.android.backupconfirm` | system | Leave alone |
| `com.android.bluetooth` | system | Leave alone |
| `com.android.bluetooth.rro` | system | Leave alone |
| `com.android.cameraextensions` | system | Leave alone |
| `com.android.captiveportallogin` | system | Leave alone |
| `com.android.certinstaller` | system | Leave alone |
| `com.android.companiondevicemanager` | system | Leave alone |
| `com.android.connectivity.resources` | system | Leave alone |
| `com.android.cts.ctsshim` | system | Leave alone |
| `com.android.cts.priv.ctsshim` | system | Leave alone |
| `com.android.devicelockcontroller` | system | Leave alone |
| `com.android.dreams.basic` | system | Leave alone |
| `com.android.dynsystem` | system | Leave alone |
| `com.android.externalstorage` | system | Leave alone |
| `com.android.federatedcompute.services` | system | Leave alone |
| `com.android.health.connect.backuprestore` | system | Leave alone |
| `com.android.healthconnect.controller` | system | Leave alone |
| `com.android.hotspot2.osulogin` | system | Leave alone |
| `com.android.htmlviewer` | system | Leave alone |
| `com.android.inputdevices` | system | Leave alone |
| `com.android.intentresolver` | system | Leave alone |
| `com.android.keychain` | system | Leave alone |
| `com.android.localtransport` | system | Leave alone |
| `com.android.location.fused` | system | Leave alone |
| `com.android.managedprovisioning` | system | Leave alone |
| `com.android.nearby.halfsheet` | system | Leave alone |
| `com.android.networkstack` | system | Leave alone |
| `com.android.networkstack.tethering` | system | Leave alone |
| `com.android.networkstack.tethering.inprocess.rro` | system | Leave alone |
| `com.android.networkstack.tethering.rro` | system | Leave alone |
| `com.android.ondevicepersonalization.services` | system | Leave alone |
| `com.android.overlay.tv.settings` | system | Leave alone |
| `com.android.pacprocessor` | system | Leave alone |
| `com.android.providers.calendar` | system | Leave alone |
| `com.android.providers.contacts` | system | Leave alone |
| `com.android.providers.downloads` | system | Leave alone |
| `com.android.providers.media` | system | Leave alone |
| `com.android.providers.media.module` | system | Leave alone |
| `com.android.providers.settings` | system | Leave alone |
| `com.android.providers.settings.auto_generated_rro_product__` | system | Leave alone |
| `com.android.providers.settings.auto_generated_rro_vendor__` | system | Leave alone |
| `com.android.providers.tv` | system | **Keep** – TV channel provider |
| `com.android.providers.userdictionary` | system | Leave alone |
| `com.android.proxyhandler` | system | Leave alone |
| `com.android.rkpdapp` | system | Leave alone |
| `com.android.sdksandbox` | system | Leave alone |
| `com.android.se` | system | Leave alone |
| `com.android.settings.intelligence` | system | Leave alone |
| `com.android.sharedstoragebackup` | system | Leave alone |
| `com.android.shell` | system | Leave alone |
| `com.android.statementservice` | system | Leave alone |
| `com.android.systemui` | system | Leave alone |
| `com.android.systemui.auto_generated_rro_vendor__` | system | Leave alone |
| `com.android.tv.feedbackconsent` | system | Leave alone |
| `com.android.tv.frameworkpackagestubs` | system | Leave alone |
| `com.android.tv.mdnsoffloadmanager` | system | Leave alone |
| `com.android.tv.overlay.framework` | system | Leave alone |
| `com.android.tv.overlay.lowram` | system | Leave alone |
| `com.android.tv.overlay.networkstack` | system | Leave alone |
| `com.android.tv.overlay.settingsprovider` | system | Leave alone |
| `com.android.tv.overlay.wifi.resources` | system | Leave alone |
| `com.android.tv.settings` | system | **Keep** – Settings |
| `com.android.tv.settings.auto_generated_rro_product__` | system | Leave alone |
| `com.android.tv.settings.gms.resoverlay` | system | Leave alone |
| `com.android.tv.settings.google.resoverlay` | system | Leave alone |
| `com.android.tv.settings.mediatek.resoverlay` | system | Leave alone |
| `com.android.tv.settings.mediatek.resoverlay.noffga` | system | Leave alone |
| `com.android.tv.settings.mediatek.tv.resoverlay` | system | Leave alone |
| `com.android.tv.settings.vendor.resoverlay` | system | Leave alone |
| `com.android.uwb.resources` | system | Leave alone |
| `com.android.vending` | system | **Keep** – Play Store |
| `com.android.virtualmachine.res` | system | Leave alone |
| `com.android.vpndialogs` | system | Leave alone |
| `com.android.wallpaperbackup` | system | Leave alone |
| `com.android.wifi.dialog` | system | Leave alone |
| `com.android.wifi.resources` | system | Leave alone |
| `com.android.wifi.resources.rro` | system | Leave alone |
| `com.at.factory.androidtvaidlservice` | system | Keep – factory |
| `com.at.factory.factorymanager` | system | Keep – factory |
| `com.bstar.intl.tv` | preinstalled app | Your choice – streaming app |
| `com.cvte.autobacklight` | system | Keep – auto backlight |
| `com.cvte.tv.at` | system | Keep – factory test (CVTE) |
| `com.disney.disneyplus` | preinstalled app | Your choice – streaming app |
| `com.dolby.android.audio.service` | system | Leave alone |
| `com.google.android.apps.mediashell` | system | Your choice – Chromecast |
| `com.google.android.apps.tv.dreamx` | system | Keep – ambient mode / screensaver |
| `com.google.android.apps.tv.launcherx` | system | **Disable after installing a launcher** – Google TV home (ads) |
| `com.google.android.ext.services` | system | Leave alone |
| `com.google.android.ext.shared` | system | Leave alone |
| `com.google.android.feedback` | system | **Disable** – Google feedback |
| `com.google.android.gms` | system | **Keep** – Play Services |
| `com.google.android.gsf` | system | **Keep** – Google services framework |
| `com.google.android.inputmethod.latin` | system | **Keep** – keyboard |
| `com.google.android.katniss` | system | Your choice – Google Assistant / voice |
| `com.google.android.marvin.talkback` | system | Already disabled from the factory |
| `com.google.android.modulemetadata` | system | Leave alone |
| `com.google.android.overlay.googlewebview` | system | Leave alone |
| `com.google.android.overlay.gtvsconfigx` | system | Leave alone |
| `com.google.android.overlay.gtvssettingsprovider` | system | Leave alone |
| `com.google.android.overlay.modules.ext.services` | system | Leave alone |
| `com.google.android.overlay.modules.modulemetadata.forframework` | system | Leave alone |
| `com.google.android.overlay.modules.permissioncontroller` | system | Leave alone |
| `com.google.android.overlay.modules.permissioncontroller.forframework` | system | Leave alone |
| `com.google.android.packageinstaller` | system | Leave alone |
| `com.google.android.partnersetup` | system | Leave alone |
| `com.google.android.permissioncontroller` | system | Leave alone |
| `com.google.android.play.games` | system | **Disable** – Play Games |
| `com.google.android.safetycenter.resources` | system | Leave alone |
| `com.google.android.syncadapters.calendar` | system | Leave alone |
| `com.google.android.tts` | system | Leave alone |
| `com.google.android.tungsten.setupwraith` | system | **Disable after installing a launcher** – claims HOME; check Wi-Fi first |
| `com.google.android.tv.inputplayer` | system | **Keep** – input player |
| `com.google.android.tv.remote.service` | system | **Keep** – Google TV remote service |
| `com.google.android.tv.settings.energymodes.resoverlay` | system | Leave alone |
| `com.google.android.videos` | preinstalled app | Your choice – streaming app |
| `com.google.android.webview` | system | Leave alone |
| `com.google.android.youtube.tv` | system | Your choice – YouTube |
| `com.google.android.youtube.tvmusic` | system | Your choice – YouTube Music |
| `com.haier.appmanager.dispatcher` | system | **Keep** – remote hot keys (HotKeyService) |
| `com.haier.apps` | system | **Disable** – Kinopoisk promo |
| `com.haier.bt_pair_tips` | system | **Keep** – remote pairing tips |
| `com.haier.emanual` | system | **Disable** – e-manual |
| `com.haier.hkcfac` | system | **Keep** – global keys / factory |
| `com.haier.launcher.haiermatrix` | system | Your choice – "Haier TV+" content app (not checked in depth) |
| `com.haier.oemtabprovider` | system | **Disable** – "Haier" tab on Google TV home |
| `com.haier.tv.hmagicboard` | system | **Keep** – global keys, sound settings |
| `com.haier.tvhousekeeper` | system | **Disable** – cleaner |
| `com.haierubic.usmartdevice` | system | **Disable** – Haier smart-device service |
| `com.iqiyi.i18n.tv` | preinstalled app | Your choice – streaming app |
| `com.mediatek.android.leanbacklauncher.partnercustomizer` | system | **Disable** – partner recommendation rows |
| `com.mediatek.android.tv.globalkeyoverlay` | system | Leave alone |
| `com.mediatek.android.tv.mdns.offload.overlay` | system | Leave alone |
| `com.mediatek.androidbox` | system | Your choice – Miracast screen mirroring |
| `com.mediatek.autopair` | system | **Keep** – remote auto-pairing |
| `com.mediatek.overlay.tvcenter` | system | Leave alone |
| `com.mediatek.tv.inputplayeroverlay` | system | Leave alone |
| `com.mediatek.tv.oneworld.tvcenteroverlay` | system | Leave alone |
| `com.mediatek.tv.service` | system | **Keep** – TV middleware |
| `com.mediatek.tvinput` | system | **Keep** – HDMI, AV and tuner inputs |
| `com.mediatek.wwtv.mediaplayer` | system | Keep – USB media player |
| `com.mediatek.wwtv.setupwizard` | system | **Keep** – TV setup / channel scan |
| `com.mediatek.wwtv.tvcenter` | system | **Keep** – Live TV / HDMI app |
| `com.mediatek.wwtv.webview` | system | Leave alone |
| `com.mstar.android.tv.disclaimercustomization` | system | Leave alone |
| `com.netflix.ninja` | system | Your choice – Netflix |
| `com.telink.lt.ota` | system | **Keep** – remote firmware |
| `com.tencent.qqlivei18n` | preinstalled app | Your choice – streaming app |
| `com.truedmp.idtv` | preinstalled app | Your choice – streaming app |
| `com.truevisions.android` | preinstalled app | Your choice – streaming app |
| `com.wbd.stream` | preinstalled app | Your choice – streaming app |
| `fusion.android.tv.demo` | system | **Disable** – retail demo mode |
| `it.hon.onboarding` | system | Already disabled from the factory |
| `it.hon.service` | system | **Disable** – Haier hOn IoT/telemetry |
| `mediatek.factorymenu.ui` | system | Leave alone |