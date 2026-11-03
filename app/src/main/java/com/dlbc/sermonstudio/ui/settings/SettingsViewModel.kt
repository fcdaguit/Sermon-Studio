package com.dlbc.sermonstudio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlbc.sermonstudio.data.prefs.SettingsStore
import com.dlbc.sermonstudio.data.prefs.ThemeMode
import com.dlbc.sermonstudio.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModuleRow(
    val code: String,
    val name: String,
    val copyright: String,
    val installed: Boolean
)

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val primaryTranslation: String = "KJV",
    val autoBackup: Boolean = true,
    val modules: List<ModuleRow> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsStore,
    private val bibleRepository: BibleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settings.themeMode,
                settings.dynamicColor,
                settings.primaryTranslation,
                settings.autoBackup
            ) { theme, dynamic, primary, backup ->
                SettingsState(
                    themeMode = theme,
                    dynamicColor = dynamic,
                    primaryTranslation = primary,
                    autoBackup = backup,
                    modules = bibleRepository.catalog.map { entry ->
                        ModuleRow(
                            code = entry.descriptor.code,
                            name = entry.descriptor.name,
                            copyright = entry.descriptor.copyright,
                            installed = bibleRepository.isInstalled(entry.descriptor.code)
                        )
                    }
                )
            }.collect { newState -> _state.value = newState }
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) =
        viewModelScope.launch { settings.setDynamicColor(enabled) }
    fun setPrimary(code: String) =
        viewModelScope.launch { settings.setPrimaryTranslation(code) }
    fun setAutoBackup(enabled: Boolean) =
        viewModelScope.launch { settings.setAutoBackup(enabled) }
    fun update(block: (SettingsState) -> SettingsState) = _state.update(block)
}
