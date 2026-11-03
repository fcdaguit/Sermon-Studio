package com.dlbc.sermonstudio.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlbc.sermonstudio.core.bible.BibleReference
import com.dlbc.sermonstudio.core.bible.DetectedReference
import com.dlbc.sermonstudio.core.bible.ReferenceParser
import com.dlbc.sermonstudio.core.text.RichDocument
import com.dlbc.sermonstudio.data.bible.Verse
import com.dlbc.sermonstudio.data.prefs.SettingsStore
import com.dlbc.sermonstudio.data.repository.BibleRepository
import com.dlbc.sermonstudio.data.repository.SermonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorState(
    val outlineId: Long = 0,
    val title: String = "",
    val text: String = "",
    val references: List<DetectedReference> = emptyList(),
    val wordCount: Int = 0,
    val estimatedMinutes: IntRange = 0..0,
    val saving: Boolean = false,
    val popup: PopupState? = null
)

data class PopupState(
    val reference: BibleReference,
    val translationCode: String,
    val verses: List<Verse>,
    val loading: Boolean = false,
    val fontScale: Float = 1.0f,
    /** Set when the passage could not be loaded — shown instead of blank space. */
    val error: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val sermonRepository: SermonRepository,
    private val bibleRepository: BibleRepository,
    private val settings: SettingsStore
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state = _state.asStateFlow()

    private val textChanges = MutableStateFlow("")

    /** Bumped by edits that need a save but not a re-parse (e.g. the title). */
    private val saveTicks = MutableStateFlow(0)

    init {
        // Reference detection is debounced rather than run per keystroke. On a
        // 3,000-word outline a full scan is cheap but not free, and running it
        // on every character makes typing feel sticky on low-end devices.
        viewModelScope.launch {
            textChanges
                .debounce(250)
                .distinctUntilChanged()
                .collect { text ->
                    val detected = ReferenceParser.detect(text)
                    _state.update { it.copy(references = detected) }
                }
        }

        // Auto-save, also debounced. 1.5s after the user stops typing.
        viewModelScope.launch {
            merge(
                textChanges.map { },
                saveTicks.map { }
            ).debounce(1_500).collect { persist() }
        }
    }

    fun load(outlineId: Long) {
        if (_state.value.outlineId == outlineId) return
        viewModelScope.launch {
            val entity = sermonRepository.load(outlineId) ?: return@launch
            val doc = RichDocument.decode(entity.body)
            val plain = doc.toPlainText()
            _state.value = EditorState(
                outlineId = outlineId,
                title = entity.title,
                text = plain,
                references = ReferenceParser.detect(plain),
                wordCount = doc.wordCount(),
                estimatedMinutes = doc.estimatedMinutesRange()
            )
            textChanges.value = plain
        }
    }

    fun onTextChanged(newText: String) {
        val doc = RichDocument.fromPlainText(newText)
        _state.update {
            it.copy(
                text = newText,
                wordCount = doc.wordCount(),
                estimatedMinutes = doc.estimatedMinutesRange()
            )
        }
        textChanges.value = newText
    }

    fun onTitleChanged(newTitle: String) {
        _state.update { it.copy(title = newTitle) }
        // Title edits do not affect reference detection, so they get their own
        // save tick rather than being pushed through the text channel.
        saveTicks.value = saveTicks.value + 1
    }

    fun openReference(reference: BibleReference) {
        viewModelScope.launch {
            val code = settings.primaryTranslation.first()
            _state.update {
                it.copy(
                    popup = PopupState(reference, code, emptyList(), loading = true)
                )
            }
            val verses = runCatching { bibleRepository.passage(code, reference) }
                .getOrElse { emptyList() }

            _state.update { current ->
                current.copy(
                    popup = current.popup?.copy(
                        verses = verses,
                        loading = false,
                        error = if (verses.isEmpty()) {
                            "No text found for ${reference.display()} in $code. " +
                                "The module may not be installed."
                        } else null
                    )
                )
            }
        }
    }

    fun switchPopupTranslation(code: String) {
        val popup = _state.value.popup ?: return
        viewModelScope.launch {
            _state.update { it.copy(popup = popup.copy(translationCode = code, loading = true)) }
            val verses = runCatching { bibleRepository.passage(code, popup.reference) }
                .getOrElse { emptyList() }
            _state.update { current ->
                current.copy(
                    popup = current.popup?.copy(verses = verses, loading = false)
                )
            }
        }
    }

    fun adjustPopupFont(delta: Float) {
        _state.update { current ->
            val popup = current.popup ?: return@update current
            current.copy(
                popup = popup.copy(fontScale = (popup.fontScale + delta).coerceIn(0.7f, 2.5f))
            )
        }
    }

    fun dismissPopup() = _state.update { it.copy(popup = null) }

    /** Inserts the popup's passage at the end of the current text. */
    fun insertPopupPassage(): String? {
        val popup = _state.value.popup ?: return null
        if (popup.verses.isEmpty()) return null
        val block = buildString {
            append('\n')
            popup.verses.forEach { v -> append(v.verse).append(' ').append(v.text).append(' ') }
            append("(").append(popup.reference.display())
            append(", ").append(popup.translationCode).append(")\n")
        }
        val updated = _state.value.text + block
        onTextChanged(updated)
        dismissPopup()
        return updated
    }

    private suspend fun persist() {
        val current = _state.value
        if (current.outlineId == 0L) return
        _state.update { it.copy(saving = true) }
        sermonRepository.saveBody(
            id = current.outlineId,
            doc = RichDocument.fromPlainText(current.text),
            title = current.title
        )
        _state.update { it.copy(saving = false) }
    }

}
