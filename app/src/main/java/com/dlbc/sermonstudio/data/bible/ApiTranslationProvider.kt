package com.dlbc.sermonstudio.data.bible

import com.dlbc.sermonstudio.core.bible.BibleReference

/**
 * Network adapter for a licensed translation served by its publisher's API.
 *
 * DELIBERATELY NOT IMPLEMENTED in this scaffold.
 *
 * Writing a concrete ESV/NIV/NKJV client here would imply those translations
 * are cleared for use in your app, and they are not — that depends on terms you
 * have to read and accept yourself, with your own key, under your own account.
 * What this file gives you is the shape the rest of the app expects, plus the
 * caching policy you will need, so adding a cleared translation later is a
 * contained change.
 *
 * To implement one:
 *  1. Confirm the publisher's terms allow an offline cache at all. Several
 *     forbid persisting verse text beyond a session. If yours does, respect it:
 *     set [cachePolicy] to NONE and the app will not write text to disk.
 *  2. Supply the key from SettingsStore (user-entered). Never hard-code it,
 *     never commit it, never ship it in the APK.
 *  3. Implement passage/chapter. Search is usually not offered — leave
 *     [searchSupport] at NONE and the search screen will exclude this provider
 *     rather than showing an empty result.
 */
abstract class ApiTranslationProvider(
    override val descriptor: TranslationDescriptor
) : TranslationProvider {

    override val isOffline: Boolean = false

    override val searchSupport: SearchSupport = SearchSupport.NONE

    /** What the app is permitted to keep on disk. Defaults to the safe option. */
    open val cachePolicy: CachePolicy = CachePolicy.NONE

    override suspend fun search(query: String, limit: Int): List<Verse> =
        throw UnsupportedOperationException(
            "${descriptor.code} does not support search through its API adapter."
        )

    abstract override suspend fun passage(reference: BibleReference): List<Verse>
    abstract override suspend fun chapter(book: Int, chapter: Int): List<Verse>

    enum class CachePolicy {
        /** Nothing written to disk; verses held in memory for the session only. */
        NONE,
        /** Recently viewed passages cached, evicted by LRU. Check terms first. */
        SESSION_LRU,
        /** Full offline mirror. Only if the licence explicitly permits it. */
        PERSISTENT
    }
}
