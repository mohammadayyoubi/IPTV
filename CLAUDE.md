# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

TechWrap TV: an Android IPTV player in Java (single `:app` module). See @README.md for features, data flow, DB schema and the design system.

## Commands
- Build: `./gradlew assembleDebug`. Release: `./gradlew assembleRelease` / `bundleRelease` (R8 minify + shrinkResources on).
- Unit tests (JVM): `./gradlew testDebugUnitTest`. Single class: `./gradlew testDebugUnitTest --tests com.example.iptv.ProbeRunnerTest`.
- `unitTests.isReturnDefaultValues = true`: `android.util.*` calls return defaults in JVM tests. Keep testable logic free of Android types, or inject them (see `ProbeRunner.Connectivity`).
- No lint/formatter config: follow Android Studio defaults and the surrounding code.

## Constraints that break the build or the app
- **Java 8 source level, minSdk 24**: no `String.join`, `List.of`, `var`, or API 26+ calls without a version guard (use `TextUtils.join` etc.).
- **Media3 and FFmpeg move together**: `org.jellyfin.media3:media3-ffmpeg-decoder:X+N` must match the Media3 version in `gradle/libs.versions.toml`. Check that a Jellyfin build exists before bumping Media3.
- **Package id**: `namespace` stays `com.example.iptv`. `applicationId` becomes `lb.techwrap.iptv` only when `app/google-services.json` contains it; otherwise the build falls back to `com.example.iptv`. Don't rename Java packages.
- **R8**: `app/proguard-rules.pro` keeps the FFmpeg renderer (loaded by reflection) and Serializable `OOP.*` fields. Update it when adding reflection or Serializable models.
- `UiUtils.categoryLabel` looks up `cat_*` strings by name; `res/raw/keep.xml` stops resource shrinking from removing them.

## Database (`database/DBHelper.java`)
- One shared instance: always `DBHelper.getInstance(context)`, never `new DBHelper` (WAL + one connection pool avoids "database is locked").
- Schema change: bump `DATABASE_VERSION`, add an `if (oldVersion < N)` block in `onUpgrade` (ALTER/CREATE IF NOT EXISTS, never drop user data), and update `onCreate`.
- Lists must use the paged SQL path (`ChannelDAO.queryPage` / `countByAvailability` through `ui/ChannelPager`), never load all channels: full imports reach ~30k channels.
- Bulk writes go in chunked transactions (`runInTransaction`, `SAVE_CHUNK`) so UI writes aren't blocked for long.

## UI and strings
- Every activity extends `ui/BaseActivity` (edge-to-edge). Apply insets with `UiUtils.applyInsets`.
- Use the tokens/styles in `res/values/` (colors, dimens, styles); don't hard-code colors or sizes in layouts. Wide screens use `layout-w600dp` / `values-w600dp` / `values-w840dp`.
- **All user-visible text goes in `res/values/strings.xml` AND `res/values-ar/strings.xml`** (Arabic, RTL), with matching keys and placeholders. Use start/end, not left/right. Stored data stays English and is localized for display (`UiUtils.categoryLabel`, `countryLabel`, `serverLabel`).
- Network checks that judge servers must go through `ProbeRunner` / `NetworkMonitor`, which pause instead of marking servers broken while offline.

## Naming quirks (keep as is)
- Packages `OOP`, `Fragments` and `Interfaces` are capitalized; `getFromInternet` is a class. Several activities are snake_case and match their layouts (`activity_add_channel` ...). Don't rename them in passing.

## Git
- Small fixes go straight to `main`; bigger work goes on branches with PRs. Two developers push to `main`.
- After any pull or merge, check that no file was replaced wholesale by an older version (this happened to `getFromInternet.java` in merge `b19d61c`). Use `/check-merge`.
- Don't commit unless asked. Commit messages are short and informal.
