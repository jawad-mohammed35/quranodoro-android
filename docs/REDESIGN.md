# Quranodoro 1.1 design and behavior

The redesign keeps Quranodoro's sage, olive and cream palette. Mihraya's local glass component and theme implementation were inspected as a quality reference; Quranodoro remains a separate native Kotlin/Compose app.

## Shared surfaces and navigation

- Directional highlights, fine gradient rims, softly rounded glass surfaces and restrained depth replace flat opaque cards. The header and dock sample a separately recorded graphics layer and blur that background on Android 12+. Content and controls stay sharp. Low-RAM devices and older Android versions use tinted highlights without blur. OLED uses black fills and accent outlines without an offscreen blur pass.
- Dialogs share compact title/body/button typography and use Android compositor blur where the OS supports it. System window blur may be unavailable or disabled; readable surfaces remain available. See [Android window blur](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_BLUR_BEHIND) and [Compose graphics layers](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers).
- The floating dock orders Home, Quran, Focus, Farm and Tasbih. Focus and Farm have larger central icons. Active icons are filled and inactive icons outlined. The selection lens follows horizontal movement with spring motion and expands during a glide; nearby active icons magnify. A normal tap remains accessible through Tab semantics. Horizontal touch slop prevents a small unintended movement from activating glide; a vertical exit or additional finger ends tracking.
- Navigation uses short fade/slide transitions. Settings and Analytics shortcuts hide on their own destinations. Nested pages use Back. Content remains clear of status/cutout and navigation insets, including the optional audio mini-player.

## Main pages

- Home: local Gregorian and offline Umm al-Qura dates, a compact focus summary, consistent dashboard cards and preserved layout editing, resizing, hiding, restoring and drag controls. The date refreshes while visible and immediately after returning to the foreground. Appearance settings offer a -2 to +2 day Hijri adjustment.
- Focus: a prominent animated timer ring, clearer session hierarchy, large primary action, compact confirmations, preserved presets and recitation controls.
- Quran: compact side-by-side searchable reciter/Surah selectors, a smaller player with Arabic titles, seek and playback controls, and expandable volume/favorite-reciter preferences. Media3 background playback stays intact.
- Tasbih: a responsive counting surface with spring press feedback, animated progress and system-respecting clock-tick haptics. Custom phrase creation is removed; previously saved phrases/history remain readable. Targets, counts and history are preserved. Numeric custom targets use a focused, keyboard-aware dialog with Done/Save/Cancel and a 1–9,999 range.
- Date Farm: the active palm is the centerpiece, with an illuminated scene, seven-stage progress and a clear next-growth milestone. Other palms appear in a compact horizontal orchard. Existing shaded native art and growth rules are preserved. Planting and active-tree selection remain available.
- Settings: overview rows lead to Appearance, Focus & Notifications, Tasbih, Quran & Audio, Local Progress and About. Existing preferences, backup export and developer links remain.

## Timer notifications

A non-exported `specialUse` foreground service represents a user-started Pomodoro countdown. The notification uses the system countdown chronometer; no per-second notification posting or continuous wake lock is needed. It shows the phase, opens Focus, and offers Pause/Resume/Stop. Pausing keeps a resumable notification and an idle foreground service; no timer ticker or wake lock runs during the pause. Finishing, stopping, resetting, skipping or changing presets clears the active notification. Runtime notification permission is requested on the first start on Android 13+; denial keeps the timer functional.

Notification commands are serialized as Room transactions and checked against the current session token. Deadlines use elapsed time within the current boot and wall time across reboot. The existing WorkManager completion fallback and exactly-once reward transaction remain. Doze/OEM restrictions may delay the completion alert; elapsed timer accuracy does not depend on a ticking UI. Auto-start applies while the app is open. The foreground service type is declared with its countdown purpose, per [Android foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use).

## Data compatibility

Application ID remains `com.jawadmohammed.quranodoro`. Room schema version remains 1, with no entity/schema changes or destructive migration. Existing DataStore keys remain; only Hijri adjustment and notification-prompt bookkeeping are added. Release signing stays in the original GitHub Actions secret configuration, with the original public certificate pinned by the workflow.

See [QA.md](QA.md) for executed validation and remaining device limitations.
