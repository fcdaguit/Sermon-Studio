package com.dlbc.sermonstudio.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsStore @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val PRIMARY_TRANSLATION = stringPreferencesKey("primary_translation")
        val SECONDARY_TRANSLATION = stringPreferencesKey("secondary_translation")
        val READER_FONT_SCALE = floatPreferencesKey("reader_font_scale")
        val UI_LANGUAGE = stringPreferencesKey("ui_language")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        val BACKUP_URI = stringPreferencesKey("backup_tree_uri")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[Keys.THEME] } ?: ThemeMode.SYSTEM
    }

    val dynamicColor: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: true }

    val primaryTranslation: Flow<String> =
        context.dataStore.data.map { it[Keys.PRIMARY_TRANSLATION] ?: "KJV" }

    val secondaryTranslation: Flow<String?> =
        context.dataStore.data.map { it[Keys.SECONDARY_TRANSLATION] }

    val readerFontScale: Flow<Float> =
        context.dataStore.data.map { (it[Keys.READER_FONT_SCALE] ?: 1.0f).coerceIn(0.7f, 2.5f) }

    /** Null = follow the system locale. */
    val uiLanguage: Flow<String?> = context.dataStore.data.map { it[Keys.UI_LANGUAGE] }

    val autoBackup: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.AUTO_BACKUP] ?: true }

    /** SAF tree URI the user picked for backups. Null = internal storage only. */
    val backupTreeUri: Flow<String?> = context.dataStore.data.map { it[Keys.BACKUP_URI] }

    suspend fun setThemeMode(mode: ThemeMode) = put(Keys.THEME, mode.name)
    suspend fun setDynamicColor(enabled: Boolean) = put(Keys.DYNAMIC_COLOR, enabled)
    suspend fun setPrimaryTranslation(code: String) = put(Keys.PRIMARY_TRANSLATION, code)
    suspend fun setSecondaryTranslation(code: String?) =
        if (code == null) remove(Keys.SECONDARY_TRANSLATION) else put(Keys.SECONDARY_TRANSLATION, code)
    suspend fun setReaderFontScale(scale: Float) =
        put(Keys.READER_FONT_SCALE, scale.coerceIn(0.7f, 2.5f))
    suspend fun setUiLanguage(tag: String?) =
        if (tag == null) remove(Keys.UI_LANGUAGE) else put(Keys.UI_LANGUAGE, tag)
    suspend fun setAutoBackup(enabled: Boolean) = put(Keys.AUTO_BACKUP, enabled)
    suspend fun setBackupTreeUri(uri: String?) =
        if (uri == null) remove(Keys.BACKUP_URI) else put(Keys.BACKUP_URI, uri)

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    private suspend fun <T> remove(key: Preferences.Key<T>) {
        context.dataStore.edit { it.remove(key) }
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
