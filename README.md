# Sermon Studio

An offline-first Android app for sermon preparation and Bible study — outlines,
scripture reference detection, a chapter reader, and local search. Kotlin,
Jetpack Compose, Room, Hilt.

---

## ⚠️ Read this first: what is and is not built

This repository is a **working scaffold**, not a finished application. The
distinction matters, so here it is in full.

### Implemented (real code, not stubs)

| Area | State |
|---|---|
| Scripture reference parser | **Complete.** Handles `John 3:16`, `Jn 3:16`, `Matt 5:1-12`, `Psalm 23`, `Rom 8:28-30`, `Gen 1:1, 3`, cross-chapter `Matt 5:1-6:4`, numbered books, abbreviations with periods, Tagalog/Cebuano book names. Rejects matches inside words (`Johnson`). 14 unit tests. |
| Room schema | Complete for outlines, folders, notes, bookmarks, cross-entity tags, and Bible modules, with FTS indexes. |
| Bible module system | Per-translation SQLite modules, install/remove, provider abstraction. |
| Sermon editor | Plain-text with live reference detection, debounced autosave, word count, speaking-time estimate. |
| Scripture popup | Modal bottom sheet: scroll, copy, insert, open chapter, font zoom. |
| Chapter reader | Single and verse-aligned dual-translation view. |
| Search | Keyword (FTS) + reference jump. |
| Outline assistant | Deterministic template scaffolding, 6 homiletical styles. |
| Theming | Material 3, dynamic colour, light/dark/system. |
| Backup rules, CI, asset DB build script | Complete. |

### Not implemented

| Spec item | Why, and what it would take |
|---|---|
| **WYSIWYG rich-text toolbar** | Compose has no rich text field. Needs a custom `VisualTransformation`, selection-aware span editing, and an undo/redo stack over `RichDocument` — roughly 1,000 lines on its own. **The storage format already models every span type**, so this is additive, not a migration. |
| **Inline tappable reference links** | Same dependency as above. References currently surface as tappable chips under the title, which reaches the same passage. |
| **On-device LLM** | Deferred by design — see `docs/AI_AND_SEARCH.md`. The `SermonAssistant` interface is the seam. |
| **Semantic / vector search** | Needs an embedding model and an on-device ANN index. The search screen states plainly that it is keyword-only rather than silently returning poor matches. |
| **PDF / DOCX export and import** | Dependencies (PDFBox-Android, Apache POI) are wired into the build and ProGuard rules; the converters are not written. POI on Android is heavy — budget for APK size and a method-count check. |
| **Bible text** | **No Bible text ships in this repo.** See `docs/BIBLE_DATA.md`. |
| **Tablet dual-pane** | Single-pane only. `material3-window-size-class` is already a dependency. |

### It has never been compiled

I wrote this without an Android SDK available, so **no part of it has been
built, run, or installed.** The parser algorithm was executed and validated
against every spec example in a port, but the Kotlin itself is unverified.

Expect to fix errors on the first build. Likely candidates: dependency version
mismatches, Room KSP complaints about the FTS entities, and missing imports.
This is normal for a scaffold of this size — budget an hour or two.

---

## First build

```bash
git clone <your-repo-url> && cd sermon-studio
```

**Android Studio** (easiest): open the project folder. Studio generates the
Gradle wrapper and syncs. Then Run.

**Command line:**

```bash
gradle wrapper --gradle-version 8.9   # one-time; wrapper JAR is not committed
./gradlew test                        # run the parser tests first
./gradlew assembleDebug
```

**GitHub Actions:** push to `main`. `.github/workflows/android.yml` bootstraps
the wrapper, runs tests, builds, and uploads the APK as an artifact. This is the
path to use if you have no local Android SDK.

### Adding Bible text

The app builds and runs with no Bible module — the reader shows "module not
installed". To add KJV:

```bash
./gradlew assembleDebug   # generates app/schemas/<variant>/
# read "identityHash" from
#   app/schemas/debug/com.dlbc...BibleModuleDatabase/1.json
# (CI also uploads these as the "room-schemas" artifact, so you can skip
#  the local build and download them from the Actions run instead)
python3 scripts/build_bible_db.py \
  --input kjv.xml --format zefania \
  --code KJV --name "King James Version" --language en \
  --copyright "Public domain." \
  --identity-hash <paste it> \
  --output app/src/main/assets/bibles/kjv.db
```

The identity hash is not optional — Room refuses to open an asset database
whose hash does not match the compiled schema.

---

## Architecture

```
┌──────────────────── Presentation (Compose + ViewModels) ────────────────────┐
│  Library    Editor ── ScripturePopup    Reader    Search    Settings        │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │ StateFlow
┌──────────────────────────────────┴──────────────────────────────────────────┐
│ Domain                                                                      │
│   ReferenceParser ·  SermonAssistant ──> TemplateAssistant                  │
│   RichDocument    ·  (LLM impl would bind here)                             │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
┌──────────────────────────────────┴──────────────────────────────────────────┐
│ Data                                                                        │
│   SermonRepository ──> SermonDatabase   (user data — never destructive)     │
│   BibleRepository  ──> TranslationProvider                                  │
│                          ├── LocalModuleProvider   (offline SQLite)         │
│                          ├── ApiTranslationProvider (licensed, not impl.)   │
│                          └── ModuleImporter        (Zefania/OpenSong/USFX)  │
│   SettingsStore    ──> DataStore                                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

Two separate databases, deliberately. `sermon_studio.db` holds user data and
**never** gets `fallbackToDestructiveMigration` — a missing migration should
crash in development rather than quietly erase a pastor's outlines. Bible
modules do allow destructive fallback, because verse text is re-downloadable.

Further reading:

- `docs/ARCHITECTURE.md` — layer rules, editor roadmap, export stack
- `docs/AI_AND_SEARCH.md` — what adding the on-device LLM actually costs
- `docs/BIBLE_DATA.md` — translations, licensing, sourcing
- `docs/USER_MANUAL.md` — end-user guide

## Stack

Kotlin · Compose + Material 3 · MVVM / Clean · Hilt · Room (FTS4) · Coroutines
+ Flow · DataStore · type-safe Navigation Compose · minSdk 26, targetSdk 34

## Licence

Application code: add your chosen licence. Bible texts are **not** covered by
it and are not distributed here — see `docs/BIBLE_DATA.md`.
