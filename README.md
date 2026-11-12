# Dreampage

Show one web page as the screensaver on Android TV and Google TV: a Home
Assistant dashboard, a calendar, a status board, anything with a URL.

Android calls screensavers "dreams"; Dreampage is a dream that is a page.

- **Small.** A few hundred lines of Kotlin, no libraries beyond the Android
  framework, no AndroidX.
- **Private.** No analytics, no ads, no network calls except to the page you
  choose. The only permission is `INTERNET`.
- **Stays put.** Links to other sites are ignored, and if the page fails to
  load it retries every 30 seconds.
- **Sign in once.** "Open page" shows the same page interactively. Cookies and
  local storage are shared with the screensaver, so a login sticks.
- **Works as code.** The URL can be set over adb, so a setup script can own it.

## Install

Download the APK from [Releases](../../releases) and install it:

```sh
adb install dreampage-vX.Y.Z.apk
```

## Set the page

On the device, open **Dreampage**, enter the URL, press **Save**, then
**Open page (sign in)** if the site needs a login.

Or from a computer:

```sh
adb shell am broadcast -n com.lotamaster.dreampage/.ConfigReceiver \
  -a com.lotamaster.dreampage.action.CONFIGURE --es url https://example.com/
```

Only the adb shell (and the system) can send that broadcast; other apps on the
device can't change the URL.

## Make it the screensaver

On most Android TV devices: **Settings → Device Preferences → Screen saver**,
pick Dreampage.

Google TV (Chromecast with Google TV, Google TV Streamer, and TVs running
Google TV) hides third-party screensavers from Settings. Set it with adb
instead:

```sh
adb shell settings put secure screensaver_components com.lotamaster.dreampage/.DreampageDream
```

To return to Google's Ambient mode:

```sh
adb shell settings put secure screensaver_components com.google.android.apps.tv.dreamx/.service.Backdrop
```

A system update can reset this setting; run the command again if it does.
Dreampage's own screen shows whether it is the active screensaver.

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
- The screensaver is display-only: any remote button wakes the TV.

## Build

```sh
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
```

Release builds are signed in CI from repository secrets
(`DREAMPAGE_KEYSTORE_B64`, `DREAMPAGE_KEYSTORE_PASSWORD`,
`DREAMPAGE_KEY_ALIAS`, `DREAMPAGE_KEY_PASSWORD`) when a `v*` tag is pushed.
Locally, set `DREAMPAGE_KEYSTORE` and the same variables to sign.

## License

MIT
