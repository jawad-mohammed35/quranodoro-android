# Quranodoro 1.0.0 — Debug testing prerelease 1

This is an installable **debug-signed testing build**, not a production release. Production signing is deliberately deferred at the owner's request.

- Native Kotlin / Jetpack Compose app, minimum Android 8.0 / API 26, target API 37. No WebView or authentication.
- Sage/olive light and charcoal dark/system themes, adaptive bottom navigation and local preferences.
- Nine customizable dashboard widgets, touch drag/reorder with edge scrolling, compact/full-width sizing, hide/add and confirmed reset.
- Original Pomodoro presets plus custom rhythms, restored elapsed-time timer, breaks, focus history, farm rewards and optional notifications.
- All 12 reciters and 114 Surahs, favorites, seeking, volume, background playback, native media session and lock-screen controls.
- Tasbih targets/custom dhikr, preserved count when switching presets, haptics and saved completion statistics.
- Six Date Farm varieties, seven shaded growth stages, analytics, daily reminders, settings, developer links and JSON backup export.
- Progress stays locally in Room/DataStore. No ads, trackers or account dependency.

Validation: 10 domain unit tests and 16 Android instrumentation tests; successful debug and unsigned release builds and lint; small-phone/enlarged-font and tablet-size UI suites; actual audio playback with screen off; notification posting; timer force-stop restoration. All 1,368 reciter/Surah URLs returned valid ranged MP3 responses.

Known limits: some new Android-specific copy falls back to English within the six shared language catalogues. Notification timing is subject to Android battery restrictions. Auto-start is foreground-only. Physical Bluetooth, OEM Doze behavior and physical foldables need broader testing. Backup import is not available. Desktop pixel resizing and chart presentations are adapted to native mobile layouts.

Download `Quranodoro-v1.0.0-debug.1.apk`, open it on Android 8.0 or later, allow installation from the chosen browser/file manager when Android asks, and install. A SHA-256 checksum is attached. Production-signed builds will use a different signing key and cannot directly update this debug installation.

Production signing instructions and the complete setup/build process are in the repository README. Do not upload signing keys or passwords to issues or chat.
