package com.dlbc.sermonstudio.data.bible

import com.dlbc.sermonstudio.core.bible.BibleReference

/**
 * The adapter boundary the spec asks for.
 *
 * Everything above this interface (editor, popup, search UI) is translation-
 * agnostic. Below it there are three kinds of implementation:
 *
 *  1. [LocalModuleProvider]  — an offline SQLite module. Public-domain texts
 *                              ship this way; it is the only fully offline path.
 *  2. ApiTranslationProvider — a network adapter for a licensed translation the
 *                              publisher exposes via API (ESV is the realistic
 *                              example). Requires the user's own API key.
 *  3. ImportedModuleProvider — a module the user supplied themselves from a
 *                              Zefania/OpenSong/USFX file they are licensed to
 *                              possess. The app converts, it does not supply.
 *
 * LICENSING — read before adding translations
 * -------------------------------------------
 * Only (1) is distributable in your APK, and only for texts actually in the
 * public domain. For (2) and (3) the app must never bundle the text and never
 * ship a key. My understanding, which you should verify with each publisher
 * before release:
 *
 *  - ESV (Crossway): has an official API requiring a free key, with usage
 *    limits and attribution requirements. Check current terms directly.
 *  - NIV (Biblica/Zondervan): I am not aware of a generally available public
 *    API for app developers. Assume a commercial licence is required.
 *  - NKJV (Thomas Nelson/HarperCollins): same — assume commercial licence.
 *
 * I may be wrong on the current state of any of these; they change. Confirm
 * before you ship, and do not treat the presence of an adapter stub in this
 * codebase as evidence that a translation is cleared for use.
 */
interface TranslationProvider {

    val descriptor: TranslationDescriptor

    /** True when the provider can serve text with no network. */
    val isOffline: Boolean

    /** Returns the verses in [reference], in order. Empty if out of range. */
    suspend fun passage(reference: BibleReference): List<Verse>

    /** Whole chapter, for the reader screen. */
    suspend fun chapter(book: Int, chapter: Int): List<Verse>

    /**
     * Keyword/phrase search. Offline providers use FTS; API providers may not
     * support search at all, in which case they return [SearchSupport.NONE]
     * from [searchSupport] and this throws [UnsupportedOperationException].
     */
    suspend fun search(query: String, limit: Int = 200): List<Verse>

    val searchSupport: SearchSupport get() = SearchSupport.FULL
}

enum class SearchSupport { FULL, LIMITED, NONE }

data class Verse(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val text: String
)

data class TranslationDescriptor(
    /** Stable short code used in settings and bookmark rows, e.g. "KJV". */
    val code: String,
    val name: String,
    /** BCP-47-ish tag: "en", "tl", "ceb". */
    val language: String,
    val license: LicenseKind,
    /** Attribution line the UI must display wherever the text is shown. */
    val copyright: String,
    val sizeBytes: Long? = null
)

enum class LicenseKind {
    /** Verified public domain — may be bundled or hosted by this project. */
    PUBLIC_DOMAIN,

    /** Open licence with conditions (e.g. attribution). Check before bundling. */
    OPEN_LICENSED,

    /** Licensed text reached through the publisher's API with the user's key. */
    API_LICENSED,

    /** Licensed text the user imported themselves. Never redistributed. */
    USER_SUPPLIED
}
