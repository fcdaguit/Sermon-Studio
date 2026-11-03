package com.dlbc.sermonstudio.data.repository

import android.content.Context
import com.dlbc.sermonstudio.core.bible.BibleReference
import com.dlbc.sermonstudio.data.bible.LicenseKind
import com.dlbc.sermonstudio.data.bible.LocalModuleProvider
import com.dlbc.sermonstudio.data.bible.TranslationDescriptor
import com.dlbc.sermonstudio.data.bible.TranslationProvider
import com.dlbc.sermonstudio.data.bible.Verse
import com.dlbc.sermonstudio.data.db.bible.BibleModuleDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the set of installed translations and hands out providers.
 *
 * Modules are opened lazily and kept open — a Room database handle is cheap to
 * hold and expensive to re-open, and the reader switches translations often.
 */
@Singleton
class BibleRepository @Inject constructor(
    private val context: Context
) {
    private val mutex = Mutex()
    private val open = mutableMapOf<String, TranslationProvider>()

    /**
     * Translations this build knows how to install.
     *
     * Only KJV is marked as bundled below, because a single bundled module
     * already adds roughly 4-5 MB to the APK and four would make the install
     * unreasonable on the connections this app is likely used on. The rest are
     * downloads. See docs/BIBLE_DATA.md for where the source texts come from
     * and what still needs verifying.
     */
    val catalog: List<ModuleCatalogEntry> = listOf(
        ModuleCatalogEntry(
            descriptor = TranslationDescriptor(
                code = "KJV",
                name = "King James Version",
                language = "en",
                license = LicenseKind.PUBLIC_DOMAIN,
                copyright = "Public domain."
            ),
            bundledAssetPath = "bibles/kjv.db"
        ),
        ModuleCatalogEntry(
            descriptor = TranslationDescriptor(
                code = "ASV",
                name = "American Standard Version (1901)",
                language = "en",
                license = LicenseKind.PUBLIC_DOMAIN,
                copyright = "Public domain."
            )
        ),
        ModuleCatalogEntry(
            descriptor = TranslationDescriptor(
                code = "WEB",
                name = "World English Bible",
                language = "en",
                license = LicenseKind.PUBLIC_DOMAIN,
                copyright = "World English Bible, public domain."
            )
        ),
        ModuleCatalogEntry(
            descriptor = TranslationDescriptor(
                code = "TAB",
                name = "Ang Biblia (1905)",
                language = "tl",
                license = LicenseKind.PUBLIC_DOMAIN,
                copyright = "Ang Biblia (1905). Public domain."
            )
        ),
        ModuleCatalogEntry(
            descriptor = TranslationDescriptor(
                code = "CEB",
                name = "Ang Bag-ong Maayong Balita Biblia / Bugna",
                language = "ceb",
                // Marked for verification rather than asserted: see BIBLE_DATA.md.
                license = LicenseKind.PUBLIC_DOMAIN,
                copyright = "Cebuano Bugna (1917). Public domain status to be " +
                    "confirmed for the specific edition you import."
            )
        )
    )

    suspend fun provider(code: String): TranslationProvider? = mutex.withLock {
        open[code]?.let { return it }

        val entry = catalog.firstOrNull { it.descriptor.code == code } ?: return null
        val file = moduleFile(code)

        val db = when {
            file.exists() ->
                BibleModuleDatabase.open(context, "bible_$code.db", externalFile = file)
            entry.bundledAssetPath != null ->
                BibleModuleDatabase.open(context, "bible_$code.db", assetPath = entry.bundledAssetPath)
            else -> return null // not installed
        }

        LocalModuleProvider(entry.descriptor, db.bibleDao()).also { open[code] = it }
    }

    fun isInstalled(code: String): Boolean {
        val entry = catalog.firstOrNull { it.descriptor.code == code } ?: return false
        return entry.bundledAssetPath != null || moduleFile(code).exists()
    }

    suspend fun removeModule(code: String): Boolean = mutex.withLock {
        val entry = catalog.firstOrNull { it.descriptor.code == code } ?: return false
        if (entry.bundledAssetPath != null) return false // bundled modules cannot be removed
        open.remove(code)
        context.deleteDatabase("bible_$code.db")
        moduleFile(code).delete()
    }

    suspend fun passage(code: String, reference: BibleReference): List<Verse> =
        provider(code)?.passage(reference).orEmpty()

    private fun moduleFile(code: String) =
        File(context.filesDir, "bible-modules/${code.lowercase()}.db")
}

data class ModuleCatalogEntry(
    val descriptor: TranslationDescriptor,
    /** Non-null when the module ships inside the APK. */
    val bundledAssetPath: String? = null
)
