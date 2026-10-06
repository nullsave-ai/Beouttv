# BEOUT TV

Premium Android mobile streaming app — Kotlin + Jetpack Compose + Material 3.

## Open the project

1. Extract the ZIP.
2. Open the folder in Android Studio (Koala/Ladybug or newer) — "Open" → select the `BeoutTV` folder.
3. `gradlew` / `gradlew.bat` and `gradle/wrapper/gradle-wrapper.properties` are
   included (pointing at Gradle 8.7), but `gradle-wrapper.jar` itself is a
   binary that couldn't be generated in this offline environment. Android
   Studio's "Sync Project with Gradle Files" will fetch it automatically on
   first sync; if you'd rather do it from the command line first, run
   `gradle wrapper` once with any local Gradle install to generate the jar.
4. Run on a phone or emulator (min SDK 24, target/compile SDK 34).

## Placeholder images

All poster/backdrop/logo URLs point to `picsum.photos` seeded placeholders so
every screen renders real imagery immediately on first run. Swap these for
your actual CDN/API URLs in `FakeBeoutRepository` (or your real repository)
whenever you're ready.

## What's implemented

- **Design system** (`ui/theme`): `BeoutColors`, `BeoutTypography`, `BeoutShapes`,
  `BeoutDimensions` — every screen pulls from these tokens, nothing hardcoded.
- **Reusable components** (`ui/components`): top bar, bottom nav, channel/movie/
  series/match cards, section header, search bar, category chips, buttons,
  skeleton loaders, empty state, error state — matching the spec's naming
  (`BeoutChannelCard`, `BeoutMovieCard`, etc).
- **Screens** (`ui/screens`): Home (hero banner + horizontal sections), Live TV
  (2-column grid with category filter chips + search), Sports (live/upcoming
  match cards), Movies & Series (grids), Movie Details and Series Details
  (seasons/episodes) matching the spec's "Home → Movie → Details → Play" flow,
  Search (categorized results + recent searches), Favorites (empty state
  wired up), Settings, and a full-screen Media3 ExoPlayer screen with custom
  auto-hiding controls, PiP support, and landscape lock.
- **Navigation** (`ui/navigation`): Jetpack Navigation Compose with the 5-tab
  bottom bar (Home / Live TV / Sports / Search / More) plus stack navigation
  for Movies, Series, Details, Settings, Favorites.
- **Architecture**: UI → ViewModel (`StateFlow`/`UiState`) → Repository
  interface, with `FakeBeoutRepository` providing mock content so every
  screen renders immediately.
- **Icons**: Material Rounded icons everywhere — zero emojis, per spec.
- **Arabic support**: `values-ar/strings.xml` provided; RTL is handled by
  `android:supportsRtl="true"` and Compose's automatic `LayoutDirection`.
- **App icon**: adaptive icon (dark background + blue broadcast arcs + white
  play triangle) in `res/mipmap-anydpi-v26` / `res/drawable`.

## What you still need to wire up

- Replace `FakeBeoutRepository` with real API/IPTV backend calls (Retrofit,
  your M3U/Xtream Codes source, etc.) — the UI layer only depends on the
  `BeoutRepository` interface, so this is a drop-in swap.
- Populate real `streamUrl`, `logoUrl`, `posterUrl`, `backdropUrl` values.
- Add a proper Arabic-friendly font (e.g. Cairo, IBM Plex Sans Arabic) in
  `ui/theme/BeoutTypography.kt` by replacing `FontFamily.Default`.
- Hook up favorites/search/settings persistence (DataStore/Room) if you need
  it to survive process death — currently favorites live in the in-memory
  fake repository.
- Wire the player's seek bar, volume, and quality/subtitle menus to real
  `ExoPlayer` state (the UI shell is complete; the state bindings are
  simplified placeholders).
