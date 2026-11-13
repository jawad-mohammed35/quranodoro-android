# Quranodoro for Android

A standalone native Islamic productivity app built with Kotlin and Jetpack Compose. No WebView, accounts, ads, tracking SDKs, or backend dependency. The original [web application](https://github.com/jawad-mohammed35/quranodoro) is a separate project and is not modified by this repository.

Initial testing build: [Quranodoro v1.0.0-debug.1](https://github.com/jawad-mohammed35/quranodoro-android/releases/tag/v1.0.0-debug.1). This is a debug-signed prerelease. Production updates use a separate release certificate and cannot replace this debug build in place.

## Features

- Pastel sage and olive palette, warm light theme, charcoal dark and true-black OLED themes, system appearance, and a floating navigation bar with tap and horizontal glide controls, a spring-animated glass lens and emphasized Focus/Farm tabs.
- Home dashboard: nine widgets, long-press drag handles, accessible move controls, compact/full-width sizing, hide/add widgets and confirmed reset. Layout saves automatically.
- Pomodoro: five original presets, saved custom presets, focus/short/long breaks, pause/resume/reset/skip, cycle tracking, history and growth rewards.
- Quran: all 12 web reciters and 114 Surahs, search, favorites, volume, seeking, previous/next, loading/retry states. Media3 service supports background streaming, media notifications and external media controls.
- Tasbih: preset dhikr selection, 33/99/100 and custom targets, optional touch feedback, completion history. Switching targets preserves the count.
- Date Farm: six varieties, seven stages, original growth calculations, active-tree selection, persistent orchard and shaded native palm illustrations.
- Analytics: weekly focus, 12 monthly summaries, 30-day activity history, current streak, average duration, session history and dhikr/farm totals.
- Settings overview with dedicated Appearance, Focus & Notifications, Tasbih, Quran & Audio, Local Progress and About pages; developer links and local JSON backup export.
- Offline Umm al-Qura Hijri date on Home, based on local time with a configurable ±2-day moon-sighting adjustment.
- Shared frosted cards and dialogs, refined type and animated transitions. Hardware backdrop blur on Android 12+ with a low-RAM fallback; true-black OLED surfaces. English interface with Arabic Quran and dhikr content.

## Open in Android Studio

Requires Android Studio supporting AGP 9.3, JDK 17, SDK platform 37.0 and Build Tools 36.0.0. Minimum runtime Android 8.0 / API 26. Application ID `com.jawadmohammed.quranodoro`, version `1.1.1` / code `4`.

1. Clone this repository and open its root directory in Android Studio.
2. Install SDK Platform 37.0 and Build Tools 36.0.0 in SDK Manager. Accept the SDK license.
3. Let Android Studio create `local.properties`, or set `ANDROID_HOME` to your SDK.
4. Sync and run the `app` configuration on an emulator or connected device.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Windows: use `gradlew.bat`. The Gradle wrapper is included. Build outputs and machine-specific paths are ignored.

## Data and background behavior

Room schema version 1 stores timer state, focus sessions, trees, Tasbih progress and dhikr history. DataStore stores preferences, favorites, custom presets and dashboard configuration. Exported Room schemas are committed; future schema changes must supply explicit migrations. Destructive fallback migration is deliberately not enabled.

The timer stores both monotonic and wall-clock deadlines plus the device boot count. The monotonic deadline wins within a boot, with wall-clock fallback after reboot. Completion and farm rewards run in a single Room transaction keyed by a session UUID, so reopening and competing completion jobs cannot award twice. A session's tree is captured when it starts.

UI polling stops when the activity stops. A user-started `specialUse` foreground service keeps an ongoing countdown notification during active Pomodoro phases. Android's chronometer updates the countdown without per-second notification posting. Pause, Resume and Stop actions are token-guarded Room transactions; tapping the notification opens Focus. Paused sessions retain their service and a resumable notification without a ticker, polling or wake lock, avoiding races during quick Pause/Resume actions. Reset, skip, preset change and completion remove the active notification. WorkManager remains the persistent completion fallback. No continuous wake lock is held; Doze and OEM restrictions can delay the completion alert, while the saved deadline remains accurate. Auto-start applies while the app is open. Force-stopping an app blocks background execution until it is reopened.

The audio service separately uses a media-playback foreground service while playing recitation. Audio focus, noisy-output handling, media sessions, headset controls and notification lifecycle are provided by Media3. Streaming needs internet; all productivity features remain local and work offline. Uninstalling clears local data. Backup export is JSON; automatic import is not offered in the original web app or this initial Android version.

## Permissions and privacy

- `INTERNET`: HTTPS Quran audio streaming and provider responses.
- `ACCESS_NETWORK_STATE`: Android WorkManager network awareness.
- `POST_NOTIFICATIONS`: requested on the first timer start, or when enabling completion reminders on Android 13+. Denial leaves the timer usable; Android controls notification visibility.
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: ongoing user-requested audio, media notification and lock-screen controls.
- `FOREGROUND_SERVICE_SPECIAL_USE`: user-started Pomodoro countdown and its controls; the service stops when the session is reset, stopped or finished.
- `WAKE_LOCK` and `RECEIVE_BOOT_COMPLETED`: added by WorkManager to finish short scheduled jobs reliably and restore scheduling after restart. The timer service does not acquire a continuous wake lock.
- `com.jawadmohammed.quranodoro.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`: AndroidX's app-private signature permission for internal receiver compatibility; it grants no access to personal data.

No location, contacts, microphone, camera or storage permission is requested. Android's document picker handles export. Android cloud backup is disabled; the app does not transmit progress. External developer links open only when tapped. See [PRIVACY.md](PRIVACY.md).

## Production signing and releases

Never commit a keystore, its password, or `local.properties`. Keep a secure, backed-up copy of the original signing key: Android updates must use the same key.

Use the original Quranodoro production keystore for every update. Its public certificate fingerprint is pinned in the release workflow. Back up the keystore outside this checkout on encrypted storage, and keep its alias and both passwords in a password manager. The workflow reads four repository Actions secrets:

- `KEYSTORE_BASE64`: Base64 encoding of the entire production `.jks` file.
- `KEYSTORE_PASSWORD`: keystore password.
- `KEY_ALIAS`: alias selected when creating the key.
- `KEY_PASSWORD`: private-key password.

For Windows, encode the key in memory and send it directly to GitHub CLI (substitute your private local path; do not paste the output into chat):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\private\quranodoro-release.jks')) | gh secret set KEYSTORE_BASE64 --repo jawad-mohammed35/quranodoro-android
gh secret set KEYSTORE_PASSWORD --repo jawad-mohammed35/quranodoro-android
gh secret set KEY_ALIAS --repo jawad-mohammed35/quranodoro-android
gh secret set KEY_PASSWORD --repo jawad-mohammed35/quranodoro-android
```

The three latter commands securely prompt for their values. Do not put passwords in command-line arguments.

For a release:

1. Update `versionName` and increment `versionCode` in `app/build.gradle.kts`.
2. Run all checks and device QA. Commit and push.
3. Push the matching tag, for example `git tag v1.1.1` and `git push origin v1.1.1`.
4. The release workflow requires all four secrets, checks version/tag agreement, runs unit tests and lint, builds and verifies the production APK, and publishes `Quranodoro-v1.1.1.apk` plus its SHA-256 checksum.

Missing signing secrets fail the workflow before publication. No debug APK is substituted. Ordinary CI uploads a clearly named debug artifact. Any initial testing prerelease is explicitly labeled debug and is not a production release. A production-signed APK cannot update a debug-signed installation; export progress before removing a test installation.

## QA and feature differences

See [docs/QA.md](docs/QA.md) for executed checks and unverified device behaviors and [docs/FEATURE_PARITY.md](docs/FEATURE_PARITY.md) for the web-to-native mapping. A successful build is not a claim that every OEM, Bluetooth device or tablet has been tested.

The native layout uses mobile spans rather than desktop pixel resizing. Palm art uses shaded vector paths rather than web illustrations. The interface is English-only. Arabic Quran and dhikr content is preserved. No authentication or synchronization is present by design.

Screenshots: [Home](docs/screenshots/home.png), [Dark theme](docs/screenshots/home-dark.png), [Focus](docs/screenshots/focus.png), [Quran](docs/screenshots/quran.png), [Tasbih](docs/screenshots/tasbih.png), [Farm](docs/screenshots/farm.png).

Audio metadata/endpoints are adapted from the owner's web catalogue; streams remain on mp3quran.net and no recordings are redistributed. `scripts/verify-audio.ps1` checks ranged responses for all reciters; add `-FullCatalogue` for all 1,368 combinations. This checks HTTP/media availability, not scholarly identity, recording completeness or legal ownership. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## License

New native implementation: MIT, copyright Jawad Mohammed. The source web repository did not include a license at the inspected revision; its branding/catalogues and translations are attributed to that project and its owner. Third-party libraries and audio retain their own terms; this repository's license does not relicense recordings.
