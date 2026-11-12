# QA record

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
