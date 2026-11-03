package com.dlbc.sermonstudio.core.bible

/**
 * Canonical book table.
 *
 * [number] is the 1-based canonical order (Genesis = 1 ... Revelation = 66) and is
 * the join key used everywhere in the database. Never persist a book *name* as a
 * key: names are localised and translations abbreviate differently.
 *
 * [chapterCount] is used to validate parsed references cheaply without touching
 * the database. Verse counts per chapter are NOT stored here — they differ
 * between textual traditions (e.g. verse numbering in Psalms superscriptions,
 * 3 John 14/15), so verse-range validation is done against the actual loaded
 * translation instead.
 */
data class Book(
    val number: Int,
    val osisId: String,
    val englishName: String,
    val chapterCount: Int,
    val testament: Testament,
    /** Lower-case match tokens: full names, abbreviations, and localised names. */
    val aliases: List<String>
)

enum class Testament { OLD, NEW }

object BookCatalog {

    /**
     * Aliases include English names and common abbreviations, plus Tagalog and
     * Cebuano book names so that references typed in those languages are also
     * detected. Matching is case- and punctuation-insensitive (see
     * [ReferenceParser.normalizeToken]).
     *
     * NOTE: the Tagalog/Cebuano names here are the common forms. Verify them
     * against the actual Ang Biblia (1905) and Bugna texts you import before
     * release — spellings vary between editions, and I am not fully certain
     * every form below matches the edition you end up shipping.
     */
    val books: List<Book> = listOf(
        b(1, "Gen", "Genesis", 50, Testament.OLD, "gen", "ge", "gn", "genesis", "henesis"),
        b(2, "Exod", "Exodus", 40, Testament.OLD, "exod", "exo", "ex", "exodus", "exodo"),
        b(3, "Lev", "Leviticus", 27, Testament.OLD, "lev", "le", "lv", "leviticus", "levitico"),
        b(4, "Num", "Numbers", 36, Testament.OLD, "num", "nu", "nm", "nb", "numbers", "mga bilang"),
        b(5, "Deut", "Deuteronomy", 34, Testament.OLD, "deut", "dt", "de", "deuteronomy", "deuteronomio"),
        b(6, "Josh", "Joshua", 24, Testament.OLD, "josh", "jos", "jsh", "joshua", "josue"),
        b(7, "Judg", "Judges", 21, Testament.OLD, "judg", "jdg", "jg", "judges", "mga hukom", "mga maghuhukom"),
        b(8, "Ruth", "Ruth", 4, Testament.OLD, "ruth", "rth", "ru", "rut"),
        b(9, "1Sam", "1 Samuel", 31, Testament.OLD, "1sam", "1sa", "1s", "1 samuel", "i samuel", "1 samuel"),
        b(10, "2Sam", "2 Samuel", 24, Testament.OLD, "2sam", "2sa", "2s", "2 samuel", "ii samuel"),
        b(11, "1Kgs", "1 Kings", 22, Testament.OLD, "1kgs", "1ki", "1k", "1 kings", "i kings", "1 hari", "1 mga hari"),
        b(12, "2Kgs", "2 Kings", 25, Testament.OLD, "2kgs", "2ki", "2k", "2 kings", "ii kings", "2 hari", "2 mga hari"),
        b(13, "1Chr", "1 Chronicles", 29, Testament.OLD, "1chr", "1ch", "1 chronicles", "1 cronica", "1 mga cronica"),
        b(14, "2Chr", "2 Chronicles", 36, Testament.OLD, "2chr", "2ch", "2 chronicles", "2 cronica", "2 mga cronica"),
        b(15, "Ezra", "Ezra", 10, Testament.OLD, "ezra", "ezr", "esdras"),
        b(16, "Neh", "Nehemiah", 13, Testament.OLD, "neh", "ne", "nehemiah", "nehemias"),
        b(17, "Esth", "Esther", 10, Testament.OLD, "esth", "est", "es", "esther", "ester"),
        b(18, "Job", "Job", 42, Testament.OLD, "job", "jb"),
        b(19, "Ps", "Psalms", 150, Testament.OLD, "ps", "psa", "psalm", "psalms", "pslm", "awit", "mga awit", "salmo", "mga salmo"),
        b(20, "Prov", "Proverbs", 31, Testament.OLD, "prov", "pro", "pr", "prv", "proverbs", "kawikaan", "mga kawikaan", "panultihon"),
        b(21, "Eccl", "Ecclesiastes", 12, Testament.OLD, "eccl", "ecc", "ec", "qoh", "ecclesiastes", "mangangaral"),
        b(22, "Song", "Song of Solomon", 8, Testament.OLD, "song", "sos", "sng", "canticles", "song of songs", "song of solomon", "awit ni solomon"),
        b(23, "Isa", "Isaiah", 66, Testament.OLD, "isa", "is", "isaiah", "isaias"),
        b(24, "Jer", "Jeremiah", 52, Testament.OLD, "jer", "je", "jeremiah", "jeremias"),
        b(25, "Lam", "Lamentations", 5, Testament.OLD, "lam", "la", "lamentations", "panaghoy", "mga panaghoy"),
        b(26, "Ezek", "Ezekiel", 48, Testament.OLD, "ezek", "eze", "ezk", "ezekiel", "ezequiel"),
        b(27, "Dan", "Daniel", 12, Testament.OLD, "dan", "da", "dn", "daniel"),
        b(28, "Hos", "Hosea", 14, Testament.OLD, "hos", "ho", "hosea", "oseas"),
        b(29, "Joel", "Joel", 3, Testament.OLD, "joel", "jl"),
        b(30, "Amos", "Amos", 9, Testament.OLD, "amos", "am"),
        b(31, "Obad", "Obadiah", 1, Testament.OLD, "obad", "ob", "obadiah", "abdias"),
        b(32, "Jonah", "Jonah", 4, Testament.OLD, "jonah", "jon", "jnh", "jonas"),
        b(33, "Mic", "Micah", 7, Testament.OLD, "mic", "mc", "micah", "mikas", "miqueas"),
        b(34, "Nah", "Nahum", 3, Testament.OLD, "nah", "na", "nahum"),
        b(35, "Hab", "Habakkuk", 3, Testament.OLD, "hab", "hb", "habakkuk", "habacuc"),
        b(36, "Zeph", "Zephaniah", 3, Testament.OLD, "zeph", "zep", "zp", "zephaniah", "sofonias"),
        b(37, "Hag", "Haggai", 2, Testament.OLD, "hag", "hg", "haggai", "ageo"),
        b(38, "Zech", "Zechariah", 14, Testament.OLD, "zech", "zec", "zc", "zechariah", "zacarias"),
        b(39, "Mal", "Malachi", 4, Testament.OLD, "mal", "ml", "malachi", "malakias"),

        b(40, "Matt", "Matthew", 28, Testament.NEW, "matt", "mat", "mt", "matthew", "mateo"),
        b(41, "Mark", "Mark", 16, Testament.NEW, "mark", "mrk", "mk", "mr", "marcos"),
        b(42, "Luke", "Luke", 24, Testament.NEW, "luke", "luk", "lk", "lucas"),
        b(43, "John", "John", 21, Testament.NEW, "john", "jhn", "jn", "joh", "juan"),
        b(44, "Acts", "Acts", 28, Testament.NEW, "acts", "act", "ac", "mga gawa", "mga buhat", "gawa"),
        b(45, "Rom", "Romans", 16, Testament.NEW, "rom", "ro", "rm", "romans", "roma", "mga taga roma"),
        b(46, "1Cor", "1 Corinthians", 16, Testament.NEW, "1cor", "1co", "1 corinthians", "1 corinto", "i corinto"),
        b(47, "2Cor", "2 Corinthians", 13, Testament.NEW, "2cor", "2co", "2 corinthians", "2 corinto", "ii corinto"),
        b(48, "Gal", "Galatians", 6, Testament.NEW, "gal", "ga", "galatians", "galacia", "mga taga galacia"),
        b(49, "Eph", "Ephesians", 6, Testament.NEW, "eph", "ep", "ephes", "ephesians", "efeso", "mga taga efeso"),
        b(50, "Phil", "Philippians", 4, Testament.NEW, "phil", "php", "philippians", "filipos", "mga taga filipos"),
        b(51, "Col", "Colossians", 4, Testament.NEW, "col", "co", "colossians", "colosas", "mga taga colosas"),
        b(52, "1Thess", "1 Thessalonians", 5, Testament.NEW, "1thess", "1th", "1 thessalonians", "1 tesalonica"),
        b(53, "2Thess", "2 Thessalonians", 3, Testament.NEW, "2thess", "2th", "2 thessalonians", "2 tesalonica"),
        b(54, "1Tim", "1 Timothy", 6, Testament.NEW, "1tim", "1ti", "1 timothy", "1 timoteo"),
        b(55, "2Tim", "2 Timothy", 4, Testament.NEW, "2tim", "2ti", "2 timothy", "2 timoteo"),
        b(56, "Titus", "Titus", 3, Testament.NEW, "titus", "tit", "ti", "tito"),
        b(57, "Phlm", "Philemon", 1, Testament.NEW, "phlm", "phm", "pm", "philemon", "filemon"),
        b(58, "Heb", "Hebrews", 13, Testament.NEW, "heb", "hbr", "hebrews", "hebreo", "mga hebreo"),
        b(59, "Jas", "James", 5, Testament.NEW, "jas", "jm", "james", "santiago"),
        b(60, "1Pet", "1 Peter", 5, Testament.NEW, "1pet", "1pe", "1p", "1 peter", "1 pedro"),
        b(61, "2Pet", "2 Peter", 3, Testament.NEW, "2pet", "2pe", "2p", "2 peter", "2 pedro"),
        b(62, "1John", "1 John", 5, Testament.NEW, "1john", "1jn", "1jo", "1j", "1 john", "1 juan"),
        b(63, "2John", "2 John", 1, Testament.NEW, "2john", "2jn", "2jo", "2j", "2 john", "2 juan"),
        b(64, "3John", "3 John", 1, Testament.NEW, "3john", "3jn", "3jo", "3j", "3 john", "3 juan"),
        b(65, "Jude", "Jude", 1, Testament.NEW, "jude", "jud", "jd", "judas"),
        b(66, "Rev", "Revelation", 22, Testament.NEW, "rev", "re", "rv", "apoc", "revelation", "apocalypse", "pahayag", "gipadayag")
    )

    private fun b(
        number: Int,
        osisId: String,
        englishName: String,
        chapters: Int,
        testament: Testament,
        vararg aliases: String
    ) = Book(number, osisId, englishName, chapters, testament, aliases.toList() + englishName.lowercase())

    private val byAlias: Map<String, Book> = buildMap {
        books.forEach { book ->
            book.aliases.forEach { alias ->
                val key = ReferenceParser.normalizeToken(alias)
                // First definition wins. Ordering of [books] is canonical, so an
                // ambiguous short form resolves to the earlier book — which is
                // why genuinely ambiguous forms (e.g. "Jo") are left out above
                // rather than being silently mapped to Joshua.
                if (key.isNotEmpty()) putIfAbsent(key, book)
            }
        }
    }

    val byNumber: Map<Int, Book> = books.associateBy { it.number }

    fun find(token: String): Book? = byAlias[ReferenceParser.normalizeToken(token)]

    fun name(bookNumber: Int): String = byNumber[bookNumber]?.englishName ?: "Book $bookNumber"

    fun chapterCount(bookNumber: Int): Int = byNumber[bookNumber]?.chapterCount ?: 0

    /** Longest book phrase in words, used to bound the parser's lookbehind. */
    val maxBookWordCount: Int = books
        .flatMap { it.aliases }
        .maxOf { it.trim().split(' ').size }
}
