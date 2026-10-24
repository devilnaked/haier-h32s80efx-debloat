# Haier H32S80EFX (Google TV) — debloat + Apple TV style launcher

[ภาษาไทย](README.th.md)

A tested, reversible way to remove the ads and manufacturer bloat from a **Haier H32S80EFX**
(Google TV 14, `Haier MatrixTV CE`, firmware `UKNV.260302.001`, 1.4 GB RAM) and replace the
Google TV home screen with a small, fast launcher in the style of Apple TV.

Nothing is uninstalled: packages are only disabled with `pm disable-user`, and `tools/restore.ps1`
puts everything back.

| Home | App grid | Inputs |
|---|---|---|
| ![Home](docs/screenshots/home-dock.png) | ![Grid](docs/screenshots/app-grid.png) | ![Inputs](docs/screenshots/inputs-menu.png) |

## What this TV accepts (tested)

Full device notes and **all 160 packages with what to do with each**:
[examples/haier-h32s80efx.md](examples/haier-h32s80efx.md).

- Sideloaded APKs install fine with `pm install`, including one that declares `HOME`.
- `cmd package set-home-activity` alone is **not enough**: the Google TV launcher
  (`com.google.android.apps.tv.launcherx`, priority 2) and the setup wizard
  (`com.google.android.tungsten.setupwraith`, priority 1) both outrank it. Both must be disabled.
  Disabling only setupwraith's `RecoveryActivity` component is refused for the shell user.
- **Before disabling setupwraith**, run `adb shell cmd wifi list-networks`. A Wi-Fi network saved
  during first setup belongs to setupwraith, and Android deletes it when setupwraith is disabled,
  which drops the TV off Wi-Fi and ADB. If you are on Wi-Fi, forget the network and re-join it in
  Settings first. (The TV this was tested on uses Ethernet.)
- Live TV and HDMI screenshots come back empty; that's the TV, not a broken debloat. Use
  `adb shell "dumpsys activity activities | grep topResumedActivity"` instead.
- Thai free-to-air TV is DVB-T2: the TV has three tuner inputs (`TunerInputService`,
  `CableInputService`, `AnalogInputService`) and Live TV must open the digital one.

## Requirements

- Windows with PowerShell. Paths default to `C:\temp\platform-tools` (Google
  [SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools)) and
  `C:\temp\build-tools` (JDK 17, Android SDK platform 34 + build-tools 34, Gradle 8.7). Override with `-Adb` / `-Tools`.
- On the TV: Settings → System → About → press OK on **Android TV OS build** 7 times, then
  Developer options → **USB debugging** on. Accept the "Allow USB debugging?" prompt on first connect.

## Steps

```bash
C:\temp\platform-tools\adb.exe connect <tv-ip>:5555
```

1. **Back up first** — package lists, `getprop`, all settings:
   ```bash
   powershell -ExecutionPolicy Bypass -File tools\backup.ps1 -Tv <tv-ip>
   ```
2. **Debloat** in three small batches, each followed by a screenshot in `shots\`:
   ```bash
   powershell -ExecutionPolicy Bypass -File tools\debloat.ps1 -Tv <tv-ip>
   ```
   Then disable streaming apps you don't use, e.g.
   ```bash
   powershell -ExecutionPolicy Bypass -File tools\disable.ps1 -Tv <tv-ip> -Name extra -Packages com.google.android.youtube.tvmusic,com.google.android.videos
   ```
3. **Edit the dock** in `launcher/app/src/main/res/values/config.xml` (`dock_packages`, left to right).
4. **Build and install the launcher**:
   ```bash
   powershell -ExecutionPolicy Bypass -File tools\build-install.ps1 -Tv <tv-ip>
   ```
5. **Make it the home screen** (read the setupwraith warning above first):
   ```bash
   C:\temp\platform-tools\adb.exe -s <tv-ip>:5555 shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
   ```
   ```bash
   C:\temp\platform-tools\adb.exe -s <tv-ip>:5555 shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
   ```
   ```bash
   C:\temp\platform-tools\adb.exe -s <tv-ip>:5555 shell cmd package set-home-activity com.local.atvhome/.MainActivity
   ```
6. **Close ADB when done**: Developer options → Revoke USB debugging authorisations, turn USB
   debugging off, restart the TV, and check that port 5555 is closed
   (`Test-NetConnection <tv-ip> -Port 5555` should report `TcpTestSucceeded : False`).

## What gets disabled

| Package | What it is |
|---|---|
| `it.hon.service` | Haier hOn IoT / telemetry service |
| `com.haierubic.usmartdevice` | Haier smart-device service |
| `fusion.android.tv.demo` | Retail demo mode |
| `com.google.android.feedback` | Google feedback reporter |
| `com.haier.oemtabprovider` | The "Haier" tab on the Google TV home |
| `com.mediatek.android.leanbacklauncher.partnercustomizer` | Partner recommendation rows |
| `com.haier.apps` | Kinopoisk promo |
| `com.haier.tvhousekeeper` | "Cleaner" |
| `com.haier.emanual` | E-manual |
| `com.google.android.play.games` | Play Games |
| `com.google.android.apps.tv.launcherx` | Google TV home (with "Sponsored" ads) — step 5 |
| `com.google.android.tungsten.setupwraith` | Setup wizard, claims HOME — step 5 |

**Do not disable** (checked with `dumpsys package`): `com.mediatek.wwtv.*` and `com.mediatek.tvinput`
(Live TV, HDMI, tuner), `com.haier.appmanager.dispatcher`, `com.haier.hkcfac`, `com.haier.tv.hmagicboard`
(remote hot keys), `com.mediatek.autopair`, `com.haier.bt_pair_tips`, `com.telink.lt.ota` (remote pairing
and firmware), the keyboard, Play Store and Play Services.

## The launcher

- Pitch-black background, dock of four apps in a lighter rounded band, 4-column grid below it
  (press Down; Back returns to the dock).
- One translucent glass highlight that springs between tiles; the focused tile scales up and shows its name.
- The focused dock app's name above the dock, with a glow in a colour sampled from its banner.
- Top bar: **Inputs** (HDMI 1–3, AV, Live TV through the TV input framework), **Network** (Wi-Fi name or
  "Ethernet"), **Settings**. Greeting, thin clock and date in [Outfit](https://fonts.google.com/specimen/Outfit) (SIL OFL). Icons: [Material Symbols](https://fonts.google.com/icons) (Apache 2.0).
- Deterministic D-pad: Up/Down move exactly one row, Up from the dock goes to the top bar, the ends of the
  top bar don't fall through, and the last focused tile is remembered.
- No blur, video or polling; about 48 MB of RAM.
- Java, AndroidX RecyclerView, `minSdk`/`targetSdk` 34, package `com.local.atvhome`.

**Wallpaper:** put an image at `launcher/app/src/main/res/drawable-nodpi/wallpaper.jpg`, set `use_wallpaper`
to `true` in `config.xml` (`wallpaper_dim` 0–255 darkens it), rebuild.

**Other TVs:** the Inputs menu opens inputs in the MediaTek TV app, set in `values/config_tv.xml`.
Change it if your TV's Live TV app is different.

## Undo

```bash
powershell -ExecutionPolicy Bypass -File tools\restore.ps1 -Tv <tv-ip>
```

This re-enables every package disabled since the backup and makes the Google TV launcher home again.
One package at a time: `adb shell pm enable --user 0 <package>`. Remove the launcher: `adb uninstall com.local.atvhome`.

Optional ad-blocking DNS (doesn't block YouTube ads):
`settings put global private_dns_specifier dns.adguard-dns.com` then
`settings put global private_dns_mode hostname`. Undo: `settings put global private_dns_mode opportunistic`.

## Disclaimer

Not affiliated with Haier, Google or MediaTek. Use at your own risk; everything here is reversible, but
check each step on your own TV.
