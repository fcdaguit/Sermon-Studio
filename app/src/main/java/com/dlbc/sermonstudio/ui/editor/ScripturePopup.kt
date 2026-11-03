package com.dlbc.sermonstudio.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlbc.sermonstudio.R

/**
 * The floating scripture popup from the spec: a modal bottom sheet that shows
 * a passage without leaving the outline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScripturePopup(
    state: PopupState,
    onDismiss: () -> Unit,
    onInsert: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onSwitchTranslation: (String) -> Unit,
    onOpenFullChapter: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val clipboard = LocalClipboardManager.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        state.reference.display(),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        state.translationCode,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onZoomOut) {
                        Icon(Icons.Default.ZoomOut, stringResource(R.string.action_zoom_out))
                    }
                    IconButton(onClick = onZoomIn) {
                        Icon(Icons.Default.ZoomIn, stringResource(R.string.action_zoom_in))
                    }
                }
            }

            when {
                state.loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalArrangement = Arrangement.Center
                    ) { CircularProgressIndicator() }
                }

                state.error != null -> {
                    Text(
                        state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 12.dp)
                    ) {
                        state.verses.forEach { verse ->
                            Text(
                                text = "${verse.verse}  ${verse.text}",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize *
                                        state.fontScale,
                                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight *
                                        state.fontScale
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = {
                        clipboard.setText(
                            AnnotatedString(
                                state.verses.joinToString(" ") { "${it.verse} ${it.text}" } +
                                    " (${state.reference.display()}, ${state.translationCode})"
                            )
                        )
                    },
                    enabled = state.verses.isNotEmpty()
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Text(stringResource(R.string.action_copy), Modifier.padding(start = 6.dp))
                }

                TextButton(onClick = onInsert, enabled = state.verses.isNotEmpty()) {
                    Text(stringResource(R.string.action_insert))
                }

                TextButton(onClick = onOpenFullChapter) {
                    Icon(Icons.Default.MenuBook, null)
                    Text(
                        stringResource(R.string.action_open_chapter),
                        Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}
