package com.dlbc.sermonstudio.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dlbc.sermonstudio.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenOutline: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReader: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                actions = {
                    IconButton(onClick = onOpenReader) {
                        Icon(Icons.Default.MenuBook, stringResource(R.string.action_read_bible))
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, stringResource(R.string.action_search))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.action_settings))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.createOutline(onCreated = onOpenOutline) }
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.action_new_outline))
            }
        }
    ) { padding ->
        if (state.outlines.isEmpty()) {
            EmptyLibrary(Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.outlines, key = { it.id }) { outline ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenOutline(outline.id) }
                            .semantics {
                                contentDescription = "Sermon outline: ${outline.title}"
                            }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(outline.title, style = MaterialTheme.typography.titleLarge)
                            if (outline.preview.isNotBlank()) {
                                Text(
                                    outline.preview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2
                                )
                            }
                            Text(
                                outline.subtitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.library_empty_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            stringResource(R.string.library_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
