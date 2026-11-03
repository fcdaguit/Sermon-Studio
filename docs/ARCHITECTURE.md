# Architecture notes

## Layer rules

- **Presentation** depends on Domain. Never on Room entities directly.
- **Domain** depends on nothing Android-specific. `ReferenceParser`,
  `RichDocument` and `SermonAssistant` are plain Kotlin and unit-testable on
  the JVM — which is why the parser has real tests and the UI does not.
- **Data** implements interfaces the Domain layer declares.

One pragmatic exception: ViewModels expose Room entities for list rendering
rather than mapping through a domain model. Mapping 1:1 for the sake of purity
costs more than it returns at this size. If a mapping ever becomes non-trivial,
add the domain model then.

## Two databases

`sermon_studio.db` (user data) and one file per Bible module.

| | User DB | Bible modules |
|---|---|---|
| Destructive migration | **Never** | Allowed |
| Backed up | Yes | No (re-downloadable, blows the quota) |
| Writes | Constant | Build-time only |

Separation means installing or removing a translation is a file operation that
cannot touch user data, and the FTS index stays per-translation, which is what
you want for ranking.

## FTS4, not FTS5 — read this before "fixing" it

The spec asks for FTS5. The code uses FTS4, for two reasons:

1. **Room has no `@Fts5` annotation.** It ships `@Fts3` and `@Fts4` only. FTS5
   is reachable only by hand-written virtual-table DDL in a migration or
   callback.
2. **FTS5 availability varies by device** across the minSdk 26 → 34 range,
   since it depends on the bundled SQLite build.

FTS4 gives the prefix and phrase queries the search screen needs. If you later
want `bm25()` ranking specifically, build the FTS5 table by hand in
`scripts/build_bible_db.py` rather than through Room, and keep FTS4 for the
user database.

FTS `MATCH` input is a query language, not a literal string. `FtsQuery.sanitize`
quotes every token; unescaped input either throws or silently changes meaning.

## Rich text format

`RichDocument` — a flat list of blocks, each with inline spans. Not HTML, not
`Spanned`.

- `Spanned` is not version-stably serialisable and is tied to the View system.
- HTML invites arbitrary markup into something that must round-trip to DOCX
  and PDF.

Blocks map cleanly onto Compose `AnnotatedString`, DOCX paragraphs/runs, and
Markdown, with no lossy middle step. Unknown block types decode to plain
paragraphs, so a document written by a newer build still opens in an older one.

Scripture links store a `BibleReference` key (`43.3.16-43.3.16`), not display
text, so a link survives translation and locale changes.

## Editor roadmap

Current editor is plain text with chip-based reference links. To reach the
spec's WYSIWYG editor:

1. Replace `OutlinedTextField`'s value with a `TextFieldValue` and keep the
   `RichDocument` as the source of truth, projecting to `AnnotatedString`.
2. Write a `VisualTransformation` that renders spans and reference links.
3. Add selection-aware span toggling (bold applies to the selection, splitting
   spans at boundaries) — this is the fiddly part.
4. Undo/redo as a bounded stack of `RichDocument` snapshots. Snapshots, not
   diffs: outlines are small enough that 50 snapshots cost little, and diff
   logic is a bug farm.
5. Replace the chip row with inline `ClickableText` on link spans.

No data migration is needed at any step — the format already models all of it.

## Export stack

Dependencies are wired; converters are not written.

- **DOCX** — Apache POI. Works on Android but is heavy: watch APK size and
  method count, and keep the ProGuard keeps in `proguard-rules.pro`.
- **PDF** — two options. PDFBox-Android (already a dependency) is Apache 2.0
  and fine. **iText 7 is AGPL** — using it in a closed-source app requires a
  commercial licence. It is listed in the version catalog but deliberately not
  added to `dependencies`. Android's own `PdfDocument` is a third option if
  your layout needs are simple.
- **TXT** — Markdown projection from `RichDocument`. Trivial; do this one first.

Import (DOCX/TXT/PDF → editor) is lossy by nature. Prefer dropping formatting
you cannot represent over guessing at it.

## Performance decisions worth keeping

- Reference detection is debounced 250 ms, autosave 1.5 s. Running the parser
  per keystroke makes typing feel sticky on low-end devices.
- Bible module handles are opened lazily and held. Room handles are cheap to
  hold, expensive to reopen, and the reader switches translations often.
- Asset databases are in `noCompress` — SQLite cannot mmap a compressed asset.
- Dual-translation reading is verse-aligned rows, not two scrolling columns.
  Independent columns drift apart exactly when the reader is comparing.

## Accessibility

- No hard-coded font family, so the user's accessibility font and scale apply.
- `bodyLarge` line height is raised to ~1.6× for verse text.
- Speaking time is shown as a **range**. Rates vary roughly 100–160 wpm; a
  single confident number would be false precision.
