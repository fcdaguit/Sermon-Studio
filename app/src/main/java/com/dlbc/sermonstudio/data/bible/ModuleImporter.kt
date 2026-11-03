package com.dlbc.sermonstudio.data.bible

import java.io.InputStream

/**
 * Converts a user-supplied Bible module file into the app's SQLite format.
 *
 * This exists so a user who legitimately owns a module in a common interchange
 * format can use it offline. The app converts a file the user already has; it
 * does not fetch, host, or distribute licensed text.
 *
 * Formats, in rough order of how often you will meet them:
 *  - Zefania XML  (<XMLBIBLE><BIBLEBOOK bnumber=".."><CHAPTER cnumber=".."><VERS vnumber="..">)
 *  - OpenSong XML (<bible><b n="Genesis"><c n="1"><v n="1">)
 *  - USFX XML     (OSIS-adjacent; more complex, markers interleaved with text)
 *
 * IMPLEMENTATION NOTE: parse with a *streaming* pull parser (XmlPullParser),
 * not DOM. A whole Bible is 31,000+ verses and several MB of XML; a DOM parse
 * will OOM on low-RAM devices, which are exactly the devices this app targets.
 * Insert in batches inside one transaction — per-verse transactions take
 * minutes instead of seconds.
 */
interface ModuleImporter {
    val format: ModuleFormat
    fun canHandle(firstBytes: ByteArray): Boolean
    suspend fun import(
        input: InputStream,
        descriptor: TranslationDescriptor,
        onProgress: (versesWritten: Int) -> Unit
    ): ImportResult
}

enum class ModuleFormat { ZEFANIA_XML, OPENSONG_XML, USFX_XML }

sealed interface ImportResult {
    data class Success(val verseCount: Int, val databaseFileName: String) : ImportResult
    data class Failure(val reason: String, val cause: Throwable? = null) : ImportResult
}
