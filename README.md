# 📺 IPTV Android Player (Java)

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com)
[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![SQLite](https://img.shields.io/badge/SQLite-07405E?style=for-the-badge&logo=sqlite&logoColor=white)](https://www.sqlite.org/index.html)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=white)](https://firebase.google.com/docs/auth)
[![Media3](https://img.shields.io/badge/Media3%20ExoPlayer-4DB6AC?style=for-the-badge&logoColor=black)](https://developer.android.com/media/media3)

An Android IPTV streaming app written in Java. Channels, countries and categories are stored locally in **SQLite**. They are imported from the public [iptv-org API](https://github.com/iptv-org/api) (or added by hand) through a **Firebase-protected admin panel**. Viewers browse channels by country or category, switch between several stream servers per channel, keep a local favorites list, and watch `.m3u8` (HLS) streams with **Media3 ExoPlayer**.

> Status: work in progress.

---

## ✨ Features

| Area | What it does |
| --- | --- |
| 📡 Streaming | Plays HLS (`.m3u8`) streams with Media3 ExoPlayer, with fullscreen in landscape. It sends each stream's required `User-Agent` / `Referer`. |
| 🩺 Stream health | Every server is *working*, *broken* or *unchecked*. The player records the result whenever a server plays or fails, and the admin **Check all servers** action probes every URL. |
| 🔎 Availability filter | Channel lists filter by **Watchable** (default) / **Not working** (all servers broken) / **No servers** / **All**, with counts. A filter button beside the search field opens and closes the options, and turns red while a non-default filter is applied. |
| 🏠 My country first | Home, category lists and favorites list the viewer's country first (`HomeCountry`: IP country from ipinfo.io, cached weekly, then SIM/network country, then region setting). |
| 🎚️ Video quality | Player button (on the video and under Favorites): **Auto**, **Up to N p** (from the stream's own qualities) or **Data saver** (lowest bitrate). Saved as a cap that applies to every channel. |
| ⚡ Scales to the world catalogue | Every channel list (home, category/country, favorites, admin) is **paged from SQLite** (60 rows at a time) with SQL search and filters, so ~30k channels stay smooth. |
| 🔄 Update database | One action adds new categories, countries, channels, servers and changed logos, for *my countries* or *all countries*. |
| 🔁 Multi-server | Each channel can have several stream URLs ("Server 1", "Server 2", …) and the viewer picks one. |
| 🌍 Browse | All channels, by category, by country, with live search on every list. |
| ⭐ Favorites | Favorites are stored locally on the device. No account is needed. |
| 🔐 Admin | Firebase email/password login opens the admin panel (dashboard counts, CRUD, imports). |
| ⬇️ Import | One-tap import from **iptv-org + Free-TV**: channels for *my country* (detected by IP) or *all countries*, with an optional limit per country. Re-importing merges new servers into existing channels instead of duplicating them. |

---

## 🔄 How it works

```
                ┌──────────────── Admin (Firebase Auth) ────────────────┐
                │                                                       │
 iptv-org API ──┼─► getFromInternet ──► DAO layer ──► SQLite (iptv_db) ◄─┼── Manual CRUD screens
 ipinfo.io   ───┘   (background threads)                   │            │
                                                           ▼
                                   Viewer screens (browse / search / favorites)
                                                           │
                                                           ▼
                                      ChannelDetailActivity ─► ExoPlayer (HLS)
```

1. **First admin login.** `AdminPanelActivity` seeds the database automatically when it's empty:
   - **Countries** come from `Locale.getISOCountries()`, with flags from `https://flagcdn.com/w80/<iso>.png`.
   - **Categories** come from `https://iptv-org.github.io/api/categories.json`.
2. **Channel import** (admin panel, `Catalog` + `getFromInternet.importChannels`):
   - *My Country Channels*: `ipinfo.io/json` detects the country code (fallback is `LB`), then only that country's channels are imported.
   - *All Countries Channels*: the import optionally keeps only the first **N** channels per country, taking channels that have streams first.
   - **Sources**:
     | Source | What it provides |
     | --- | --- |
     | [iptv-org](https://github.com/iptv-org/api) `channels.json` | Channel id, name, country, categories (closed and NSFW channels are skipped) |
     | iptv-org `logos.json` | Logos. `channels.json` no longer has a `logo` field, which is why logos were empty. The import prefers in-use, channel-wide PNG/JPEG over SVG (Glide can't decode SVG). |
     | iptv-org `streams.json` | Stream URLs, quality, and required `user_agent` / `referrer`. Streams with `"channel": null` are skipped because they can't be placed. |
     | [Free-TV/IPTV](https://github.com/Free-TV/IPTV) `playlist.m3u8` | A curated list of channels broadcasters provide **for free**. Its `tvg-id` values are iptv-org ids, so its URLs become extra servers of the same channel. Channels that only Free-TV lists are added under their country and *General*. YouTube/Twitch page links are ignored. |
   - Channels are matched by iptv-org id (`Channel.sourceId`), so importing again only adds missing servers and fills in missing logos.
   - A channel is skipped when its country isn't in the local DB. Unknown categories fall back to *General*.
   - **Import options** (dialog): *All* channels, only those *With servers*, or only *Working* ones (streams are tested during the import with `StreamChecker.checkStreams`). Plus a per-country cap for *All countries*.
   - **Update database**: adds missing categories (`categories.json`) and countries, then imports new channels and servers and changed logos for *My countries* (countries that already have channels) or *All countries*.
   - Saving uses an in-memory index of the channel table and stored URLs (two queries), plus one transaction, instead of several queries per channel.
   - **Refresh logos** fills in missing logos and replaces import-managed logos that changed. It only downloads `channels.json` when old rows need name matching; otherwise it needs just `logos.json`.
   - **Check all servers**: 32 parallel probes with short timeouts and `Range` requests. Each unique URL is probed once, and results are written in batched transactions. It can check only servers not checked in the last 24 h.
   - **Offline-safe checks** (`ProbeRunner` + `NetworkMonitor`). The server check and the "Working only" import never blame a stream for the phone's own connection:
     - They refuse to start unless Android reports a *validated* internet connection.
     - A success is final. A failure is only *tentative*: it becomes "broken" only after a later health probe succeeds (`generate_204` endpoints from Google and Cloudflare, under 3 s) **and** no outage happened since that stream's probe started.
     - If the network is lost (`ConnectivityManager` callback), or the health probe fails or is slow, the run **pauses**. Tentative failures are re-queued, the dashboard shows the pause, and the run **resumes automatically** when the connection is good again.
     - Timeouts and DNS errors get one retry. Real HTTP answers (4xx/5xx, non-stream content) count as failures immediately, but are still confirmed by the health probe.
     - A **Stop** button ends a check early and keeps the results so far.
     - The player also never marks a server broken while the phone is offline.
     - Covered by `app/src/test/.../ProbeRunnerTest.java` (simulated outages mid-run).
   - **Progress**: imports, *Update database*, *Refresh logos* and *Check all servers* show an overall percentage.
     - Downloads count the bytes received against `Content-Length`. `Catalog` asks for gzip itself, so the compressed size is known, and each file is weighted.
     - Each phase fills its own share of the bar: download 0–40 % and save 40–100 %; with *Working only*, download 0–20 %, stream testing 20–80 % and save 80–100 %.
   - **Load all**: the *Categories* page adds every iptv-org category, and the *Countries* page adds every ISO country with its flag. Both keep existing rows. *Update database* also does both.
   - Only one long task runs at a time (`AdminTasks`). Its rows on the dashboard are locked until it finishes, even if you leave and reopen the dashboard.
3. **Viewers** open the app on `MainUserActivity`, which has four tabs (Channels, Categories, Countries, Favorites). Tapping a channel opens `ChannelDetailActivity`. The first server starts playing automatically, and the viewer can switch servers with the chips under the player. `http://` URLs are upgraded to `https://` before playback. If a stream fails, the player shows an error with a Retry button.

---

## 🧭 Screens

### Viewer side (`activities/user`, `Fragments/user`)
| Screen | Class | Layout |
| --- | --- | --- |
| Home shell + bottom nav | `MainUserActivity` (launcher) | `activity_main_user.xml` |
| All channels grid + search | `AllChannelsFragment` | `fragment_all_channels.xml` |
| Categories grid + search | `CategoriesFragment` | `fragment_categories.xml` |
| Countries grid + search | `CountriesFragment` | `fragment_countries.xml` |
| Favorites grid | `FavoritesFragment` | `fragment_favorites.xml` |
| Channels filtered by country/category | `activity_user_filtered_channels` | `activity_user_filtered_channels.xml` |
| Player / channel details | `ChannelDetailActivity` | `activity_channel_detail.xml` |

### Admin side (`activities/admin`)
| Screen | Class | Layout |
| --- | --- | --- |
| Login | `LoginActivity` | `activity_login.xml` |
| Forgot password | `activity_forgot_password` | `activity_forgot_password.xml` |
| Dashboard + imports | `AdminPanelActivity` | `activity_admin_panel.xml` |
| Channels list | `activity_channel_management` | `activity_channel_management.xml` |
| Add / edit channel | `activity_add_channel` (extra `channel_id` = edit mode) | `activity_add_channel.xml` |
| Categories CRUD | `activity_category_management` | `activity_category_management.xml` |
| Countries CRUD | `activity_country_management` | `activity_country_management.xml` |

> ⚠️ **Admin users** is hidden from the dashboard for now (the activity is still in the code).
> It was only a demo. It lists, adds and edits documents in the Firestore `adminUsers` collection, but it doesn't create or delete Firebase Auth accounts. Doing that needs the Firebase Admin SDK running on a backend (for example, Cloud Functions).

---

## 🎨 UI design system

The app always uses a **dark "cinema" theme**: layered near-black surfaces and one refined red accent. The accent is reserved for primary actions, the selected tab and the LIVE badge. Use these rules when adding screens.

### Tokens (`res/values/`)
| File | What's in it |
| --- | --- |
| `colors.xml` | Surfaces `bg` → `surface` → `surface_high` → `surface_highest`, `outline`/`outline_strong`, `accent` (#E5484D) + `accent_container`, text `text_primary/secondary/tertiary`, `danger`, and `tint_1..8` for avatars |
| `dimens.xml` (+ `values-w600dp`, `values-w840dp`) | Spacing scale (`space_2xs`…`space_xl`), `page_gutter`, `grid_gap`, corner radii, **minimum tile widths** for responsive grids, `form_max_width` / `content_max_width` |
| `themes.xml` | `Theme.IPTV` (Material 3 Dark), which maps every M3 color role and sets default component styles. `Theme.IPTV.Player` has a black window for the player. |
| `styles.xml` | Text appearances (`TextAppearance.IPTV.Display/Title/Subtitle/Body/Caption/Overline/Stat`) and widgets (`Widget.IPTV.Card`, `.TextField`, `.SearchField`, `.Button`, `.Button.Tonal`, `.IconButton[.Row/.Danger/.OnVideo]`, `.Chip.Filter`, `.Chip.Meta`, `.ExtendedFab`, `IPTV.LiveBadge`, `IPTV.SectionLabel`) |
| `strings.xml` | All UI copy, including plurals (`count_channels`, `count_servers`, …) |

Don't hard-code colors or sizes in layouts. Use the tokens and styles above.

### Shared UI code (`com.example.iptv.ui`)
| Class | Use it for |
| --- | --- |
| `BaseActivity` | Every activity extends it. It turns on edge-to-edge (required for targetSdk 35) with light bar icons. |
| `UiUtils.applyInsets(view, top, bottom[, ime])` | Adds status/navigation bar (and optionally keyboard) insets as padding |
| `UiUtils.setupGrid(recycler, R.dimen.tile_min_*)` | Responsive grid: the column count comes from the window width |
| `UiUtils.showEmptyState(...)` | Fills the shared `view_empty_state.xml` include |
| `UiUtils.applyAvatar(textView, name)` | Letter avatar with a stable tint from the name |
| `GridSpacingItemDecoration` | Even gaps between grid tiles for any span count |
| `InputDialog` | Material add/edit dialogs (`dialog_input.xml`) and `InputDialog.confirm(...)`, where destructive prompts get a red action |
| `SearchableAdapter` | Type-to-search dropdowns (`MaterialAutoCompleteTextView`) |

### Responsive behaviour
- **Phones**: bottom navigation. **≥ 600dp wide** (tablets, phones in landscape): `layout-w600dp/activity_main_user.xml` switches to a **navigation rail**.
- Grids pick their column count from the width: channel tiles are at least `tile_min_channel` wide, and category/country rows at least `tile_min_row`. Admin lists turn into two or more columns on tablets.
- Forms and the dashboard are centered and capped at `form_max_width` / `content_max_width`.
- Player: 16:9 in portrait (capped at 60% of the screen height) and fullscreen in landscape. The player's fullscreen button switches orientation, and Back leaves fullscreen.

---

## 🗂️ Project structure

```
app/src/main/java/com/example/iptv/
├── OOP/                 # Data models: Channel, ChannelServer, Country, Category, Favorite
├── database/            # DBHelper (SQLiteOpenHelper) + one DAO per table
├── Catalog.java         # Downloads + merges iptv-org (channels/streams/logos) and the Free-TV playlist
├── getFromInternet.java # Countries, categories, IP country lookup, importChannels(), refreshLogos()
├── StreamChecker.java   # Probes stream URLs (bulk, de-duplicated, batched writes) and stores working / broken
├── ImportOptions.java   # Import scope, server filter, cap; Result summary
├── AdminTasks.java      # The single running admin task + its progress (locks dashboard rows)
├── Interfaces/          # Callbacks used by getFromInternet (main-thread delivery)
├── activities/
│   ├── user/            # MainUserActivity, ChannelDetailActivity, filtered channels
│   └── admin/           # Login, panel, CRUD screens
├── Fragments/user/      # Bottom-nav / rail tab fragments
├── ui/                  # BaseActivity, UiUtils, InputDialog, SearchableAdapter, GridSpacingItemDecoration,
│                        # ChannelPager (paged lists), AvailabilityFilter (collapsible filter)
├── adapters/
│   ├── user/            # Grid adapters for viewers
│   └── admin/           # List adapters with edit/delete actions
└── MainActivity.java    # Unused template activity
```

---

## 🗄️ Database (SQLite, `iptv_db`, version 3)

`DBHelper.onUpgrade` migrates in place with `ALTER TABLE`: v2 added `sourceId` and the server header and health columns, and v3 added `logoAuto` and a name index. Updating the app keeps imported channels and favorites.

Channel lists come from `ChannelDAO.queryPage(ChannelQuery, limit, offset)`, which joins per-channel server totals to compute availability in SQL, and from `countByAvailability()` for the filter counts. Both take about 5–15 ms on the full ~31k-channel catalogue.

Foreign keys are enabled (`PRAGMA foreign_keys=ON`) and every relation uses `ON DELETE CASCADE`, so deleting a country or category also deletes its channels, their servers and their favorites.

| Table | Columns |
| --- | --- |
| `Country` | `id` PK, `name`, `flagUrl` |
| `Category` | `id` PK, `name` |
| `Channel` | `id` PK, `name` (indexed, NOCASE), `logoUrl`, `countryId` → Country, `categoryId` → Category, `sourceId` (iptv-org id, indexed), `logoAuto` (1 = logo came from an import, so updates may replace it; editing the logo by hand sets 0) |
| `ChannelServer` | `id` PK, `channelId` → Channel, `serverName`, `streamUrl`, `userAgent`, `referrer`, `status` (0 unknown / 1 working / 2 broken), `lastChecked` |
| `Favorite` | `id` PK, `channel_id` → Channel |

`Channel` is `Serializable` and travels between screens as the `"channel"` intent extra, with its `servers` list loaded.

---

## 🛠️ Tech stack

- **Java 8**, Android SDK 24 → 35, AGP 8.5, Gradle 8.7
- **Material Components 3** (`Theme.Material3.DayNight.NoActionBar`)
- **AndroidX Media3** (ExoPlayer + HLS + UI)
- **Glide** for logos and flags
- **Firebase Auth** (admin login) and **Firestore** (admin-users demo)
- **SQLite** through `SQLiteOpenHelper` with the DAO pattern

---

## 🚀 Getting started

1. Clone the repo and open it in **Android Studio** (JDK 17).
2. Firebase:
   - Create a project in the [Firebase Console](https://console.firebase.google.com/) and enable **Email/Password** authentication.
   - Add an admin user under *Authentication → Users*.
   - Put your own `google-services.json` in `app/`.
3. Build and run: `./gradlew assembleDebug`, or **Run ▶** in Android Studio.
4. In the app, tap the **shield icon** at the top of the Channels tab and sign in. The countries and categories fill in automatically. Then use **My Country Channels** or **All Countries Channels** to import streams.

---

## 🧩 Known limitations

- Stream support is HLS only, and many public streams are geo-blocked or go offline. Use *Check all servers* to flag them.
- Cleartext HTTP is allowed (`res/xml/network_security_config.xml`) because many streams and logos are only served over `http://`. The player used to rewrite them to `https://`, which broke them.
- The bulk import runs on a plain background `Thread` and parses the whole `channels.json` and `streams.json` in memory. It has no progress UI and can take a while.
- Database access runs on the main thread in most screens.
- "Clear Database" drops every table, so countries and categories are only re-seeded the next time the admin panel opens.

- The hidden *Admin users* screen's delete action calls `getCurrentUser().delete()`, which deletes the **signed-in** admin's own account. Fix it before turning the screen back on.

## 🌐 Ideas for the future

- More formats (DASH, MP4, YouTube)
- Move DB/network work to a repository with `ExecutorService` or WorkManager
- Firebase Remote Config, or a backend, for the channel catalogue
- A real admin-user management backend

---

## 🤝 Authors

- Mohammad Al Ayoubi
- Mahmoud Al Natour
