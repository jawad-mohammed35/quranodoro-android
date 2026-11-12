# QA record

This is a debug testing prerelease. It is not a production-signed release or a claim of universal device compatibility.

## Executed locally

- Native APK compiled with JDK 17, AGP 9.3.3, Gradle 9.5.0 and SDK 37.0, minimum API 26.
- Latest full local run: 10 unit tests and 13 instrumentation tests passed, including the opt-in network media test. No tests were skipped in that run.
- APK installed and app launched on an Android 15 / API 35 x86_64 emulator.
- Unit suite: elapsed-clock accuracy, reboot fallback, paused-state behavior, original farm reward anchors, every variety/stage boundary, bounded growth, catalogue validity and streak rules.
- Room instrumentation: rapid concurrent counting without lost taps, preset/count preservation, milestone extension, exactly-once timer rewards, stale completion rejection, captured tree association and maturation.
- Compose instrumentation: all five bottom destinations, theme persistence, timer restoration/pause, Tasbih recreation, dashboard hide/add/resize/reset and actual long-press drag reorder.
- Android lint completed without errors. Remaining warnings include pinned dependencies with newer releases available and standard exported media-session service guidance.
- Full source-catalogue HTTP verification: 1,368 / 1,368 reciter–Surah combinations returned valid HTTP 206 ranged MP3 responses. See `audio-verification.json`; this is availability verification, not a guarantee of full recording contents or future CDN uptime.
- Light and dark screen captures reviewed; bottom navigation text inheritance was corrected after visual review. Screenshots are under `docs/screenshots`.
- Actual Media3 MP3 decoding, seek, pause, and playback progress while the activity was stopped and the emulator screen was off passed the network integration test.

The emulator lost Android system services once during an early run. It was cold-started without wiping data, and subsequent completed suites passed. The infrastructure failure is not counted as an app test pass.

## Still requiring broader device QA

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
