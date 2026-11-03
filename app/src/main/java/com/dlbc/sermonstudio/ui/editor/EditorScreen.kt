package com.dlbc.sermonstudio.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dlbc.sermonstudio.R

/**
 * Sermon editor.
 *
 * SCOPE NOTE — read before filing a bug about missing formatting buttons.
 *
 * This screen is a *plain-text* editor with live reference detection. The
 * spec's WYSIWYG toolbar (bold/italic/headings/colour) is not implemented here,
 * and that is a deliberate scaffold decision rather than an oversight: Compose
 * has no built-in rich text field, so a real implementation means writing a
 * custom VisualTransformation plus selection-aware span editing plus an
 * undo/redo stack over [com.dlbc.sermonstudio.core.text.RichDocument] — on the
 * order of a thousand lines of fiddly, test-heavy code on its own.
 *
 * The storage format (RichDocument) already models every span type the toolbar
 * will need, so adding the toolbar later does not require a data migration.
 * See docs/ARCHITECTURE.md -> "Editor roadmap".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    outlineId: Long,
    onBack: () -> Unit,
    onOpenChapter: (book: Int, chapter: Int) -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    LaunchedEffect(outlineId) { viewModel.load(outlineId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.title.ifBlank { stringResource(R.string.editor_untitled) },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.editor_word_count, state.wordCount),
                    style = MaterialTheme.typography.labelLarge
                )
                // Shown as a range, not a single number: speaking rates vary
                // widely and a confident "14 min" would be false precision.
                Text(
                    stringResource(
                        R.string.editor_estimated_minutes,
                        state.estimatedMinutes.first,
                        state.estimatedMinutes.last
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChanged,
                label = { Text(stringResource(R.string.editor_title_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Detected references surface as chips under the title rather than
            // as inline tappable spans. Inline spans need the same custom
            // VisualTransformation work as the formatting toolbar; chips give
            // the same reach-the-passage affordance today without pretending
            // the rich editor exists.
            if (state.references.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        state.references.distinctBy { it.reference.key() },
                        key = { it.reference.key() }
                    ) { detected ->
                        AssistChip(
                            onClick = { viewModel.openReference(detected.reference) },
                            label = { Text(detected.reference.display()) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.text,
                onValueChange = viewModel::onTextChanged,
                label = { Text(stringResource(R.string.editor_body_label)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                minLines = 16,
                textStyle = MaterialTheme.typography.bodyLarge
            )
        }
    }

    state.popup?.let { popup ->
        ScripturePopup(
            state = popup,
            onDismiss = viewModel::dismissPopup,
            onInsert = { viewModel.insertPopupPassage() },
            onZoomIn = { viewModel.adjustPopupFont(0.1f) },
            onZoomOut = { viewModel.adjustPopupFont(-0.1f) },
            onSwitchTranslation = viewModel::switchPopupTranslation,
            onOpenFullChapter = {
                onOpenChapter(popup.reference.bookNumber, popup.reference.startChapter)
                viewModel.dismissPopup()
            }
        )
    }
}
