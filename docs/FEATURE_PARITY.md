# Web-to-native review

Reference web revision: `c966a75084a48700527375687780155d4618c790`. Read-only inspection covered route files, layout/navigation, dashboard model/components, all stores and data catalogues, settings/backups, analytics, branding and styles, timer/audio behavior, and existing regression/readiness documents.

## Preserved

- Dashboard: all nine panels (today's focus, streak, total Pomodoros, total minutes, orchard, weekly summary, active tree, player, reminder). Personalized `Assalamu Alaikum` greeting, persisted widget order, visibility, sizing and reset.
- Pomodoro: all five built-in rhythms and saved custom rhythms; focus/short/long phases, cycles, pause/resume/reset/skip, optional auto-start, optional recitation and completion reminders.
- Quran: stable reciter IDs and source URLs for Alafasy, Sudais, Muaiqly, Ghamdi, Dosari, Husary, Minshawi, Abdulbasit, Shuraim, Shatri, Hudhaifi and Ajmy. All 114 Surahs with Arabic names, meanings, ayah counts and revelation metadata. Searchable selection, favorites, volume and transport controls.
- Tasbih: 33/99/100 and custom targets; count persists across target changes, phrase selection/custom phrase, optional feedback, confirmed reset, saved completion history.
- Farm: all six varieties and seven growth stages, exact web multipliers, diminishing-return focus rewards, one active growing tree and maturation. In-flight sessions retain their tree association.
- Analytics: weekly daily minutes, monthly focus totals, activity/streak history, overall session counts/minutes/average, current streak and session history.
- Reminders: all fifteen original verses, hadith and duas, daily deterministic choice plus refresh.
- Settings: appearance, display name, shared language catalogues, timer/audio/Tasbih/notification controls, favorites, progress export, About Date Farm and separate About the Developer with original external links and information.

## Native adaptations

- Five bottom destinations; Analytics and Settings are top-bar actions. About pages are nested routes with Android Back behavior.
- Mobile grid spans replace desktop coordinates and free pixel resizing. Narrow widgets show compact content. Dragging previews the moving card and drop target; keyed grid placement animates the reorder. Move-up controls offer a non-gesture alternative. Resize is a tap between compact and expanded spans.
- Native rounded, translucent surfaces and fine gradient borders provide a restrained glass appearance. No expensive live backdrop blur is applied. Shaded native Canvas paths render palm stages without image downloads or perpetual animation.
- Timer deadlines survive UI recreation/process loss, rewards are transactional, and completion jobs use Android WorkManager. Reminders can be delayed by Android battery restrictions; this version does not request exact-alarm access. Auto-start operates in the foreground rather than running unlimited unattended cycles.
- Media3 replaces browser HTML audio with a media session/service, focus handling, noisy-output pause and system playback controls.
- Room and DataStore replace browser/Supabase state. JSON backup export uses the Android document picker. No cloud/authentication components or login UI exist.
- The shared six-language catalogues and Arabic RTL layout are included. Newly written Android-specific descriptive text currently uses English fallback; full native-copy translation remains a localization follow-up.
- Native analytics uses compact bar summaries/activity cells and numerical monthly summaries rather than duplicating every Recharts tooltip/chart type. The recorded session data remains available locally and in export.
- Phone orientation is portrait; larger devices can use Android's adaptive/window behavior. Home uses one, two or three columns based on width. Other pages remain vertically scrollable.

## Deliberately excluded

Authentication, account/profile login, Supabase synchronization and their popups are excluded as requested. The web backup exporter does not implement import; neither does this version. There are no ads, analytics or tracking SDKs. No audio recordings are bundled or downloaded for offline playback.
