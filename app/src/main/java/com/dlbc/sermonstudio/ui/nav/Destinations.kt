package com.dlbc.sermonstudio.ui.nav

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes.
 *
 * These are @Serializable objects rather than string routes so the compiler
 * catches argument mistakes. This requires navigation-compose 2.8+; if you
 * downgrade that dependency you will need string routes instead.
 */
sealed interface Route {
    @Serializable data object Library : Route
    @Serializable data object Search : Route
    @Serializable data object Settings : Route
    @Serializable data object Bookmarks : Route

    @Serializable data class Editor(val outlineId: Long) : Route

    @Serializable
    data class Reader(
        val book: Int,
        val chapter: Int,
        /** Scroll target; 0 = top of chapter. */
        val verse: Int = 0
    ) : Route
}
