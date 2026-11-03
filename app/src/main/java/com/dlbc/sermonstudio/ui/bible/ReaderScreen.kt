package com.dlbc.sermonstudio.ui.bible

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
 * Chapter reader with optional side-by-side comparison.
 *
 * The split view renders two translations in one row per verse rather than two
 * independent scrolling columns. Independent columns drift out of alignment the
 * moment one translation wraps differently, which is exactly when a reader is
 * trying to compare — so verse-aligned rows are the correct trade even though
 * they waste some vertical space.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: Int,
    chapter: Int,
    scrollToVerse: Int,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    LaunchedEffect(book, chapter) { viewModel.load(book, chapter) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(state.rows.size, scrollToVerse) {
        if (scrollToVerse > 0 && state.rows.isNotEmpty()) {
            val index = state.rows.indexOfFirst { it.verse == scrollToVerse }
            if (index >= 0) listState.scrollToItem(index)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.heading) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (state.error != null) {
            Text(
                state.error!!,
                modifier = Modifier.padding(padding).padding(24.dp),
                color = MaterialTheme.colorScheme.error
            )
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            items(state.rows, key = { it.verse }) { row ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(
                        "${row.verse}  ${row.primary}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    row.secondary?.let { secondary ->
                        Text(
                            secondary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}
