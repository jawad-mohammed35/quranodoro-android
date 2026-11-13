# QA record

## Version 1.1.1 follow-up

Local API 26 validation passed all 25 instrumentation tests with real network audio enabled, including the legacy OLED dialog capture and three consecutive Pause/Resume cycles. Unit tests, debug/release lint and release assembly also passed. The API 35 suite passed all 25 tests for the redesign before this follow-up.

CI exposed a race where a repeated service start could overwrite the observer's updated Pause/Resume notification with the startup placeholder. The placeholder now posts only for a newly created service. Paused sessions retain an idle foreground service with no ticker, polling or wake lock; reset/stop/completion still end it. Auto-start now checks that the app is still foregrounded after asynchronous completion. Arabic reciter/Surah search text is retained in the compact selectors.

CI's software emulator emitted invalid graphics-buffer errors around dialog captures. Its Vulkan backend is disabled explicitly, matching the stable local legacy setup. CI also retains device logs and screenshot artifacts. Signed 1.1.1 publication and follow-up upgrade results are recorded below once complete.

## Version 1.1.0 (November 13, 2026)

Executed locally on the Android 15 / API 35 emulator:

- 11 domain unit tests passed, including a known Umm al-Qura Ramadan date, day adjustment bounds and dates across timezones.
- 25 instrumentation tests passed with real network audio enabled. Coverage includes all primary routes, filled active-tab semantics, horizontal glide/cancellation, Back/recreation, OLED pixel sampling, pickers/confirmations, dashboard drag/hide/resize/reset, concurrent Tasbih increments, custom target validation and count preservation, Settings subpages, Hijri preference persistence, session/reward idempotency and stale notification actions.
- Ongoing timer notification posted with a system countdown chronometer; Pause/Resume/Stop, return-to-Focus, background/locked-screen elapsed deadlines and background session completion/removal/reminder delivery passed.
- Actual streamed MP3 playback, background/screen-off progress, pause, seek and media selection passed. This is an emulator audio test, not physical-device speaker/Bluetooth verification.
- A separate compact 720 × 1600 / 320dpi configuration (360dp wide) at font scale 1.6 passed the important-action/confirmation/custom-target checks in Light, Dark and OLED. Default testing used 1080 × 2400 / 420dpi (411dp) at font scale 1.0. The emulator settings were restored afterward.
- Debug and release lint completed without errors; unit tests and release assembly passed. Local release assembly is unsigned when signing environment variables are absent; only the certificate-verified GitHub Actions APK is published.
- Screenshots were visually reviewed for the main screens and large-font Focus/target layouts. Full-window target captures verified numeric keyboard placement below the input and actions. Screenshots are under `docs/screenshots`.

Testing found and fixed controls obscured by the dock, notification intent recreation, and a service-context crash in background completion. The completion test now exercises the service/background path directly. Room entities/schema version and existing preference keys were preserved.

The published [v1.1.0 production APK](https://github.com/jawad-mohammed35/quranodoro-android/releases/tag/v1.1.0) was downloaded and verified with apksigner. Package ID and version 1.1.0/code 3 were checked; it is not debuggable. Both v1.0.1 and v1.1.0 have certificate SHA-256 `49dedb9ec26f0998ad9c570360a1a06ff88beddde5bffc313bdfbc94682f4c45`. The published APK SHA-256 is `8b6814a497ad15387f7c6c08154c5ab21974f3274bd8ac65c5f825b2ce9ab3b7` and matches the downloaded checksum file.

A real signed `adb install -r` upgrade from v1.0.1 succeeded on the API 35 emulator without uninstalling or clearing data. Seven Tasbih taps, target 99, a planted Deglet Noor palm and Dark theme survived. Count/target also survived force-stop/reopen. Documentation screenshots were refreshed from the installed signed APK. The original [signed-release workflow](https://github.com/jawad-mohammed35/quranodoro-android/actions/runs/43475817963) passed; Android 35 CI passed as well. The Android 26 OLED dialog screenshot assertion was changed from a single center pixel to a rendered region to handle full-screen capture coordinates and asynchronous window presentation on API 26/27; the same strict black-pixel condition is retained across that region. This local QA does not claim physical-device testing, comprehensive TalkBack testing, leak profiling, frame-time benchmarking, or long-duration OEM/Doze behavior. System window blur depends on OS/device support; hardware Compose blur has an older-Android/low-RAM fallback, and OLED intentionally uses black surfaces. Haptic delivery uses Android's view feedback API and respects the preference/system setting; the feel of vibration still needs a physical phone.

Reproduce the complete local network-enabled suite:

```sh
./gradlew testDebugUnitTest lintDebug lintRelease assembleRelease connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.networkAudio=true
```

## Version 1.0.1

The Android 15 emulator passed all 19 instrumentation tests at each of three phone configurations with gesture navigation enabled:

- 720 × 1280 at 360 dpi, 320 dp wide, font scale 1.3.
- 1080 × 2400 at 420 dpi, 411 dp wide, font scale 1.0.
- 1440 × 3120 at 480 dpi, 480 dp wide, font scale 1.1.

The runs included real streamed playback with the screen off, notification delivery, Room persistence, timer restoration, dashboard drag and resize, tap and hold-to-slide navigation, cancellation, Back and activity recreation. All ten domain unit tests passed. The OLED tests sampled rendered background pixels on nine pages and the theme picker. Additional screenshots covered twelve nested pickers, editors and confirmations. Compact-layout testing caught a navigation bar overlapping scroll targets and a nested back-stack restoration problem. Both have regression coverage.

An Arabic (Egypt) Android locale override retained the English interface. System Default followed Android day/night changes, and a cold launch restored OLED Black. After a force-stop, the timer advanced 12 seconds over 12.10 seconds of wall time.

The [final CI run](https://github.com/jawad-mohammed35/quranodoro-android/actions/runs/43240727158) passed on API 26 and 35. The [release workflow](https://github.com/jawad-mohammed35/quranodoro-android/actions/runs/43241648795) signed the APK and checked its certificate. An anonymous download matched the published SHA-256 checksum. On a fresh API 35 emulator, installing the downloaded APK, selecting OLED, counting a Tasbih tap and reinstalling the same APK with `adb install -r` preserved both the theme and count. The emulator reported 10 janky frames among 305 frames during 20 rapid tab taps (95th percentile 44 ms). No app crash or ANR appeared in that run's exit history.

Physical OLED appearance, OEM Doze, Bluetooth interruptions, refresh-rate behavior, long-duration battery use and memory leaks require further device testing.

## Version 1.0.0 debug prerelease

### Executed locally

- Native APK compiled with JDK 17, AGP 9.3.3, Gradle 9.5.0 and SDK 37.0, minimum API 26.
- Full local run: 10 unit tests and 16 instrumentation tests passed, including the opt-in network media test and notification delivery. No tests were skipped in that run. Debug and unsigned release builds and both lint variants passed.
- APK installed and app launched on an Android 15 / API 35 x86_64 emulator.
- Unit suite: elapsed-clock accuracy, reboot fallback, paused-state behavior, original farm reward anchors, every variety/stage boundary, bounded growth, catalogue validity and streak rules.
- Room instrumentation: rapid concurrent counting without lost taps, preset/count preservation, milestone extension, exactly-once timer rewards, stale completion rejection, captured tree association and maturation.
- Compose instrumentation: all five bottom destinations, theme persistence, timer restoration/pause, Tasbih recreation, dashboard hide/add/resize/reset and actual long-press drag reorder.
- Android lint completed without errors. Remaining warnings include pinned dependencies with newer releases available and standard exported media-session service guidance.
- Full source-catalogue HTTP verification: 1,368 / 1,368 reciter–Surah combinations returned valid HTTP 206 ranged MP3 responses. See `audio-verification.json`; this is availability verification, not a guarantee of full recording contents or future CDN uptime.
- Light and dark screen captures reviewed; bottom navigation text inheritance was corrected after visual review. Screenshots are under `docs/screenshots`.
- Actual Media3 MP3 decoding, seek, pause, and playback progress while the activity was stopped and the emulator screen was off passed the network integration test.
- A tablet-sized 1067dp-wide / 1600×2560 emulator configuration passed all eight navigation, theme, dashboard and timer UI checks after replacing the lazy widget layout with a stable animated flow layout. This resolved a reproducible span/visibility placement crash.
- A 320dp-wide / 720×1280 phone configuration with font scale 1.3 passed all eight UI checks. These are emulator display overrides, not physical foldable or tablet tests.
- A row alignment regression checks that compact widgets share a row without overlapping on phone/tablet widths. Physical-pixel rounding prevents fractional widths from inadvertently wrapping a card.
- Background/force-stop/reopen verification: timer advanced 10 seconds over 10.27 seconds of measured wall time. The timer was then paused and reset through its UI.
- Paused Surah changes update media metadata/queue and reset progress without automatically starting playback, verified against the real Media3 session.

The emulator lost Android system services once during an early run. It was cold-started without wiping data, and subsequent completed suites passed. The infrastructure failure is not counted as an app test pass.

The final implementation commit passed both [GitHub Actions build and emulator jobs](https://github.com/jawad-mohammed35/quranodoro-android/actions/runs/43135110033). CI intentionally skips the opt-in network audio test; the full local run included it.

### Outstanding at the time of the prerelease

- Physical-device Bluetooth controls, calls/audio-focus interruption and different OEM background policies.
- Long-duration Doze notification latency and notification-permission denial/revocation across OS versions.
- Physical foldable posture changes, tablet split-screen and refresh-rate/performance profiling.
- Accessibility review with TalkBack and very large font settings, complete translation of new native copy and developer/recording licensing confirmation before production distribution.
- Production signing and upgrade-path validation with the final permanent key. The owner selected a debug testing prerelease for now.

## Reproduce

```sh
./gradlew testDebugUnitTest lintDebug connectedDebugAndroidTest
```

Opt into the real-network media test on an internet-connected test emulator:

```sh
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.networkAudio=true
```

Quote dotted `-P...` options in PowerShell. The network integration test is skipped during ordinary CI; it should not make offline regression checks flaky.
