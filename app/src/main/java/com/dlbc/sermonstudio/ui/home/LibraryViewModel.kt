package com.dlbc.sermonstudio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlbc.sermonstudio.data.repository.SermonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

data class OutlineCard(
    val id: Long,
    val title: String,
    val preview: String,
    val subtitle: String
)

data class LibraryState(val outlines: List<OutlineCard> = emptyList())

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: SermonRepository
) : ViewModel() {

    private val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

    val state: StateFlow<LibraryState> = repository.observeActive()
        .map { entities ->
            LibraryState(
                entities.map { entity ->
                    OutlineCard(
                        id = entity.id,
                        title = entity.title.ifBlank { "Untitled outline" },
                        preview = entity.plainText.take(140),
                        subtitle = dateFormat.format(Date(entity.updatedAt))
                    )
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState())

    fun createOutline(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(repository.create(title = "Untitled outline"))
        }
    }
}
