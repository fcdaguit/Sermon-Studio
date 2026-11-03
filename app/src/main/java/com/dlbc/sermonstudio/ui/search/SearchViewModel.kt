package com.dlbc.sermonstudio.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlbc.sermonstudio.core.bible.BibleReference
import com.dlbc.sermonstudio.core.bible.BookCatalog
import com.dlbc.sermonstudio.core.bible.ReferenceParser
import com.dlbc.sermonstudio.data.prefs.SettingsStore
import com.dlbc.sermonstudio.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchResult(
    val key: String,
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val reference: String,
    val text: String,
    val translationCode: String
)

data class SearchState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val searching: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val bibleRepository: BibleRepository,
    private val settings: SettingsStore
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    private val queries = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queries.debounce(300).collect { runSearch(it) }
        }
    }

    fun onQueryChanged(query: String) {
        _state.update { it.copy(query = query) }
        queries.value = query
    }

    private suspend fun runSearch(query: String) {
        if (query.isBlank()) {
            _state.update { it.copy(results = emptyList(), searching = false) }
            return
        }
        _state.update { it.copy(searching = true) }

        val code = settings.primaryTranslation.first()
        val provider = bibleRepository.provider(code)
        if (provider == null) {
            _state.update { it.copy(results = emptyList(), searching = false) }
            return
        }

        // A query that parses as a reference is treated as a jump, not a
        // keyword search — typing "John 3:16" should show John 3:16, not every
        // verse containing the word "John".
        val asReference: BibleReference? = ReferenceParser.parseSingle(query)

        val verses = if (asReference != null) {
            provider.passage(asReference)
        } else {
            runCatching { provider.search(query) }.getOrElse { emptyList() }
        }

        _state.update {
            it.copy(
                searching = false,
                results = verses.map { verse ->
                    SearchResult(
                        key = "${verse.book}.${verse.chapter}.${verse.verse}",
                        book = verse.book,
                        chapter = verse.chapter,
                        verse = verse.verse,
                        reference = "${BookCatalog.name(verse.book)} " +
                            "${verse.chapter}:${verse.verse}",
                        text = verse.text,
                        translationCode = code
                    )
                }
            )
        }
    }
}
