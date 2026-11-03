package com.dlbc.sermonstudio.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.dlbc.sermonstudio.ui.bible.ReaderScreen
import com.dlbc.sermonstudio.ui.editor.EditorScreen
import com.dlbc.sermonstudio.ui.home.LibraryScreen
import com.dlbc.sermonstudio.ui.search.SearchScreen
import com.dlbc.sermonstudio.ui.settings.SettingsScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Route.Library) {

        composable<Route.Library> {
            LibraryScreen(
                onOpenOutline = { id -> navController.navigate(Route.Editor(id)) },
                onOpenSearch = { navController.navigate(Route.Search) },
                onOpenSettings = { navController.navigate(Route.Settings) },
                onOpenReader = { navController.navigate(Route.Reader(book = 1, chapter = 1)) }
            )
        }

        composable<Route.Editor> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Editor>()
            EditorScreen(
                outlineId = route.outlineId,
                onBack = { navController.popBackStack() },
                onOpenChapter = { book, chapter ->
                    navController.navigate(Route.Reader(book, chapter))
                }
            )
        }

        composable<Route.Reader> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Reader>()
            ReaderScreen(
                book = route.book,
                chapter = route.chapter,
                scrollToVerse = route.verse,
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Search> {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenVerse = { book, chapter, verse ->
                    navController.navigate(Route.Reader(book, chapter, verse))
                }
            )
        }

        composable<Route.Settings> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
