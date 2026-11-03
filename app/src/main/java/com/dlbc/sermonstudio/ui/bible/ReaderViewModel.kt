package com.dlbc.sermonstudio.ui.bible

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlbc.sermonstudio.core.bible.BookCatalog
import com.dlbc.sermonstudio.data.prefs.SettingsStore
import com.dlbc.sermonstudio.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VerseRow(val verse: Int, val primary: String, val secondary: String?)

data class ReaderState(
    val heading: String = "",
    val rows: List<VerseRow> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val bibleRepository: BibleRepository,
    private val settings: SettingsStore
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderState())
    val state = _state.asStateFlow()

    fun load(book: Int, chapter: Int) {
        viewModelScope.launch {
            val primaryCode = settings.primaryTranslation.first()
            val secondaryCode = settings.secondaryTranslation.first()

            val primaryProvider = bibleRepository.provider(primaryCode)
            if (primaryProvider == null) {
                _state.value = ReaderState(
                    heading = "${BookCatalog.name(book)} $chapter",
                    error = "The $primaryCode module is not installed. " +
                        "Install it from Settings to read offline."
                )
                return@launch
            }

            val primary = primaryProvider.chapter(book, chapter)
            val secondary = secondaryCode
                ?.let { bibleRepository.provider(it) }
                ?.chapter(book, chapter)
                ?.associateBy { it.verse }

            _state.value = ReaderState(
                heading = "${BookCatalog.name(book)} $chapter",
                rows = primary.map { verse ->
                    VerseRow(
                        verse = verse.verse,
                        primary = verse.text,
                        secondary = secondary?.get(verse.verse)?.text
                    )
                }
            )
        }
    }
}
