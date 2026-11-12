# DashboardSaver

Your dashboard as the screensaver on Android TV and Google TV: a Home
Assistant dashboard, a Grafana board, a calendar, any web page with a URL.

- **Small.** A few hundred lines of Kotlin, no libraries beyond the Android
  framework, no AndroidX.
- **Private.** No analytics, no ads, no network calls except to the page you
  choose. The only permission is `INTERNET`, and nothing is backed up.
- **Stays put.** Links to other sites are ignored (unless you allow them), and
  if the page fails to load it retries.
- **Sign in once.** "Open page" shows the same page interactively. Cookies and
  local storage are shared with the screensaver, so a login sticks.
- **Works as code.** Every setting can be set over adb, so a setup script can
  own it.

## Install

Download the APK from [Releases](../../releases) and install it:

```sh
adb install dashboardsaver-vX.Y.Z.apk
```

## Settings

Open **DashboardSaver** on the device, or use adb (below). Every setting is in
both places.

| Setting | adb extra | Default | What it does |
| --- | --- | --- | --- |
| Page URL | `--es url` | none | The page to show. Full `http://` or `https://` URL. |
| Other hosts | `--es allowed_hosts` | none | Comma-separated hosts the page may navigate to, such as a sign-in page on another host. `*.example.com` matches subdomains. The page's own host is always allowed. |
| Reload every N minutes | `--ei reload_minutes` | `60` | Reloads the page periodically while the screensaver runs, which clears memory a long-open page builds up and recovers a page that stopped updating. 0 turns it off; up to 1440. |
| Retry after N seconds | `--ei retry_seconds` | `30` | How long to wait before retrying a page that failed to load. 5 to 3600. |
| Zoom, percent | `--ei zoom_percent` | `100` | Scales the page. 100 uses the page's own layout. 25 to 400. |
| Let the screen dim | `--ez dim` | `false` | Lets the screen dim during the screensaver. |
| Shift the page | `--ez shift_pixels` | `false` | Burn-in protection: moves the page to a random offset of up to 6dp every 2 minutes. Screensaver only. |
| Clear sign-in data | `--ez clear_data true` | button | Deletes all cookies and site storage, signing you out. |

The settings screen also has **Open page (sign in)** to use the page
interactively, **Screensaver settings** to open the system's screensaver
picker, and a line saying whether DashboardSaver is the active screensaver.

### From adb

```sh
adb shell am broadcast \
  -n com.lotamaster.dashboardsaver/.ConfigReceiver \
  -a com.lotamaster.dashboardsaver.action.CONFIGURE \
  --es url "https://ha.example.com/lovelace/tv" \
  --es allowed_hosts "auth.example.com,*.example.net" \
  --ei reload_minutes 60 \
  --ei retry_seconds 30 \
  --ei zoom_percent 125 \
  --ez dim false \
  --ez shift_pixels true
```

Pass only the extras you want to change; the others keep their current value.
If any extra is invalid, nothing is saved and the result line says why
(`result=2`). On success it prints `result=1` and the saved settings.

To sign out of the page:

```sh
adb shell am broadcast -n com.lotamaster.dashboardsaver/.ConfigReceiver \
  -a com.lotamaster.dashboardsaver.action.CONFIGURE --ez clear_data true
```

Only the adb shell (and the system) can send this broadcast; other apps on the
device can't change the settings.

## Make it the screensaver

On most Android TV devices: **Settings → Device Preferences → Screen saver**,
pick DashboardSaver.

Google TV (Chromecast with Google TV, Google TV Streamer, and TVs running
Google TV) hides third-party screensavers from Settings. Set it with adb
instead:

```sh
adb shell settings put secure screensaver_components com.lotamaster.dashboardsaver/.DashboardSaverDream
```

To return to Google's Ambient mode:

```sh
adb shell settings put secure screensaver_components com.google.android.apps.tv.dreamx/.service.Backdrop
```

A system update can reset this setting; run the command again if it does.
DashboardSaver's own screen shows whether it is the active screensaver.

### adb on Google TV

Settings → System → About → press **Android TV OS build** seven times. Then
Settings → System → Developer options → **Wireless debugging** → Pair device
with pairing code, and from your computer:

```sh
adb pair <ip>:<pairing-port>   # enter the code shown on the TV
adb connect <ip>:<port>
```

## Home Assistant tips

- Use a non-admin user for the TV, and keep "Keep me logged in" ticked when
  signing in.
- Hide Home Assistant's header for that user (for example with kiosk-mode) so
  the dashboard fills the screen.
- If you sign in through a separate host (an SSO or auth proxy), add it to
  **Other hosts**.
- The screensaver is display-only: any remote button wakes the TV.

## Build

Needs JDK 17 and the Android SDK (platform 35).

```sh
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
```

Build options, passed with `-P` or set in `~/.gradle/gradle.properties`:

| Property | Default | What it does |
| --- | --- | --- |
| `dashboardsaver.applicationId` | `com.lotamaster.dashboardsaver` (in `gradle.properties`) | The package ID. Forks should change it so their build installs alongside this one. The broadcast action, the screensaver's settings link and the on-screen adb help all follow it; replace the package in the adb commands above to match. |
| `dashboardsaver.defaultUrl` | none | A URL the app starts with before one is set, for builds you install on your own devices. Keep it in `~/.gradle/gradle.properties` so it never lands in the repo. |
| `versionName` | `0.1.0` | Set by CI from the `v*` tag. `versionCode` is derived as major×10000 + minor×100 + patch. |

Release builds are signed in CI when a `v*` tag is pushed, from these
repository secrets: `SIGNING_KEYSTORE_B64` (the keystore, base64-encoded),
`SIGNING_KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS` and `SIGNING_KEY_PASSWORD`.
Locally, set `SIGNING_KEYSTORE` (a path) and the same three variables to sign.

## License

MIT
