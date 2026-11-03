#!/usr/bin/env python3
"""
Build a Sermon Studio Bible module (.db) from a public-domain source text.

WHAT THIS SCRIPT DOES NOT DO
----------------------------
It does not download anything. You supply the source file. That is deliberate:
Bible text hosting moves around, mirrors go stale, and silently fetching a text
whose licence you have not checked is exactly the mistake this project should
not make. Get the source yourself, confirm its licence, then run this.

Suggested sources for public-domain texts (verify licence yourself — I am not
certain every mirror's claim is accurate):
  - KJV / ASV / WEB : widely mirrored; ebible.org and the Zefania XML project
                      are common starting points.
  - Ang Biblia 1905 : look for Tagalog Zefania or USFX modules.
  - Cebuano Bugna   : harder to find in a clean format; check Philippine Bible
                      Society and open module repositories. Confirm the exact
                      edition's public-domain status before distributing.

INPUT FORMATS
-------------
  --format zefania   Zefania XML (<XMLBIBLE><BIBLEBOOK bnumber><CHAPTER cnumber><VERS vnumber>)
  --format opensong  OpenSong XML (<bible><b n="Genesis"><c n="1"><v n="1">)
  --format tsv       Tab-separated: book<TAB>chapter<TAB>verse<TAB>text
                     (book is the canonical number 1-66)

OUTPUT
------
A SQLite file matching the Room schema in
app/src/main/java/.../data/db/bible/BibleEntities.kt, including the FTS4 index
and the room_master_table identity hash Room requires.

  ⚠ THE IDENTITY HASH IS THE ONE THING YOU MUST GET RIGHT.
  Room refuses to open a createFromAsset database whose identity hash does not
  match the compiled schema. The hash is emitted by Room into
  app/schemas/<DatabaseClass>/1.json when you build. Run a build first, then
  pass --identity-hash with the "identityHash" value from that JSON.
  Without it, the app will throw IllegalStateException on first open.

USAGE
-----
  python3 build_bible_db.py --input kjv.xml --format zefania \\
      --code KJV --name "King James Version" --language en \\
      --copyright "Public domain." \\
      --identity-hash <from app/schemas/.../1.json> \\
      --output ../app/src/main/assets/bibles/kjv.db
"""

from __future__ import annotations

import argparse
import os
import sqlite3
import sys
import xml.etree.ElementTree as ET

SCHEMA_VERSION = 1

# Room's generated DDL. These statements must match what Room expects for the
# entities in BibleEntities.kt. If you change an entity, rebuild, open
# app/schemas/.../1.json, and copy the "createSql" values across.
DDL = [
    """CREATE TABLE IF NOT EXISTS `verse` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `book` INTEGER NOT NULL,
        `chapter` INTEGER NOT NULL,
        `verse` INTEGER NOT NULL,
        `text` TEXT NOT NULL)""",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_verse_book_chapter_verse` "
    "ON `verse` (`book`, `chapter`, `verse`)",
    """CREATE VIRTUAL TABLE IF NOT EXISTS `verse_fts` USING FTS4(
        `text` TEXT NOT NULL, content=`verse`)""",
    """CREATE TABLE IF NOT EXISTS `module_info` (
        `id` INTEGER PRIMARY KEY NOT NULL,
        `code` TEXT NOT NULL,
        `name` TEXT NOT NULL,
        `language` TEXT NOT NULL,
        `copyright` TEXT NOT NULL,
        `verseCount` INTEGER NOT NULL,
        `schemaVersion` INTEGER NOT NULL)""",
    """CREATE TABLE IF NOT EXISTS room_master_table (
        id INTEGER PRIMARY KEY, identity_hash TEXT)""",
]

# Zefania uses canonical book numbers already; OpenSong uses names.
OPENSONG_BOOK_ORDER = [
    "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua",
    "Judges", "Ruth", "1 Samuel", "2 Samuel", "1 Kings", "2 Kings",
    "1 Chronicles", "2 Chronicles", "Ezra", "Nehemiah", "Esther", "Job",
    "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon", "Isaiah",
    "Jeremiah", "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel", "Amos",
    "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah", "Haggai",
    "Zechariah", "Malachi", "Matthew", "Mark", "Luke", "John", "Acts",
    "Romans", "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians",
    "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians",
    "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews", "James",
    "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude", "Revelation",
]
OPENSONG_INDEX = {name.lower(): i + 1 for i, name in enumerate(OPENSONG_BOOK_ORDER)}


def parse_zefania(path):
    """Stream Zefania XML. iterparse keeps memory flat on multi-MB files."""
    book = chapter = None
    for event, elem in ET.iterparse(path, events=("start", "end")):
        tag = elem.tag.split("}")[-1].upper()
        if event == "start":
            if tag == "BIBLEBOOK":
                book = int(elem.get("bnumber"))
            elif tag == "CHAPTER":
                chapter = int(elem.get("cnumber"))
        elif event == "end":
            if tag == "VERS" and book and chapter:
                text = "".join(elem.itertext()).strip()
                if text:
                    yield book, chapter, int(elem.get("vnumber")), text
                elem.clear()
            elif tag == "CHAPTER":
                elem.clear()


def parse_opensong(path):
    book = chapter = None
    for event, elem in ET.iterparse(path, events=("start", "end")):
        tag = elem.tag.split("}")[-1].lower()
        if event == "start":
            if tag == "b":
                name = (elem.get("n") or "").strip().lower()
                book = OPENSONG_INDEX.get(name)
                if book is None:
                    print(f"  ! unrecognised book name: {elem.get('n')!r}", file=sys.stderr)
            elif tag == "c":
                chapter = int(elem.get("n"))
        elif event == "end":
            if tag == "v" and book and chapter:
                text = "".join(elem.itertext()).strip()
                if text:
                    yield book, chapter, int(elem.get("n")), text
                elem.clear()
            elif tag == "c":
                elem.clear()


def parse_tsv(path):
    with open(path, encoding="utf-8") as handle:
        for line_no, line in enumerate(handle, 1):
            line = line.rstrip("\n")
            if not line.strip():
                continue
            parts = line.split("\t")
            if len(parts) < 4:
                print(f"  ! line {line_no}: expected 4 columns", file=sys.stderr)
                continue
            yield int(parts[0]), int(parts[1]), int(parts[2]), "\t".join(parts[3:]).strip()


PARSERS = {"zefania": parse_zefania, "opensong": parse_opensong, "tsv": parse_tsv}


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--input", required=True)
    ap.add_argument("--format", required=True, choices=sorted(PARSERS))
    ap.add_argument("--code", required=True, help="Short code, e.g. KJV")
    ap.add_argument("--name", required=True)
    ap.add_argument("--language", required=True, help="en / tl / ceb")
    ap.add_argument("--copyright", required=True,
                    help="Attribution line shown in the app wherever this text appears")
    ap.add_argument("--identity-hash", default="",
                    help="Room identityHash from app/schemas/.../1.json. "
                         "Omitting it produces a DB the app cannot open.")
    ap.add_argument("--output", required=True)
    args = ap.parse_args()

    if not args.identity_hash:
        print("WARNING: no --identity-hash given. Room will refuse to open this\n"
              "         database. Build the app once, read identityHash from\n"
              "         app/schemas/<db>/1.json, and re-run.\n", file=sys.stderr)

    os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)
    if os.path.exists(args.output):
        os.remove(args.output)

    conn = sqlite3.connect(args.output)
    cur = conn.cursor()
    cur.execute("PRAGMA journal_mode=OFF")
    cur.execute("PRAGMA synchronous=OFF")
    for statement in DDL:
        cur.execute(statement)

    rows = []
    count = 0
    for book, chapter, verse, text in PARSERS[args.format](args.input):
        if not 1 <= book <= 66:
            continue
        rows.append((book, chapter, verse, text))
        count += 1
        # Batch inserts: one transaction per verse turns a 20-second job into
        # a 20-minute one.
        if len(rows) >= 5000:
            cur.executemany(
                "INSERT OR REPLACE INTO verse (book, chapter, verse, text) VALUES (?,?,?,?)",
                rows)
            rows.clear()
            print(f"  {count} verses...", file=sys.stderr)
    if rows:
        cur.executemany(
            "INSERT OR REPLACE INTO verse (book, chapter, verse, text) VALUES (?,?,?,?)", rows)

    # Populate the FTS index from the content table in one pass.
    cur.execute("INSERT INTO verse_fts(verse_fts) VALUES('rebuild')")

    cur.execute(
        "INSERT OR REPLACE INTO module_info "
        "(id, code, name, language, copyright, verseCount, schemaVersion) "
        "VALUES (1,?,?,?,?,?,?)",
        (args.code, args.name, args.language, args.copyright, count, SCHEMA_VERSION))

    cur.execute("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)",
                (args.identity_hash,))

    cur.execute(f"PRAGMA user_version = {SCHEMA_VERSION}")
    conn.commit()
    cur.execute("VACUUM")
    conn.commit()
    conn.close()

    size_mb = os.path.getsize(args.output) / (1024 * 1024)
    print(f"\nWrote {count} verses to {args.output} ({size_mb:.1f} MB)")
    if count < 30000:
        print(f"NOTE: a full Bible is roughly 31,100 verses. {count} looks short — "
              "check the source file covers all 66 books.", file=sys.stderr)


if __name__ == "__main__":
    main()
