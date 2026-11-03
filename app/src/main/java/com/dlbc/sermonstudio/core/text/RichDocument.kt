package com.dlbc.sermonstudio.core.text

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Storage format for outline bodies.
 *
 * Why not HTML or Android's Spanned?
 *  - Spanned is not serialisable in a version-stable way and is tied to the
 *    View system, which this app does not use.
 *  - HTML invites arbitrary markup into a document the app must round-trip to
 *    DOCX and PDF; constraining the format up front keeps export honest.
 *
 * So: a flat list of blocks, each with a list of inline spans. This maps
 * cleanly onto Compose AnnotatedString for display, onto DOCX paragraphs/runs
 * for export, and onto Markdown for the TXT export, with no lossy middle step.
 *
 * Forward compatibility: unknown block or span types decode to [BlockType.PARAGRAPH]
 * / plain text rather than failing, so an outline written by a newer build
 * still opens in an older one with formatting degraded but text intact.
 */
@Serializable
data class RichDocument(
    val version: Int = 1,
    val blocks: List<Block> = emptyList()
) {
    /** Plain-text projection used for FTS, previews and word count. */
    fun toPlainText(): String = blocks.joinToString("\n") { block ->
        block.spans.joinToString("") { it.text }
    }

    fun wordCount(): Int =
        toPlainText().split(Regex("\\s+")).count { it.isNotBlank() }

    /**
     * Estimated speaking time.
     *
     * 130 wpm is a commonly cited conversational preaching rate, but real rates
     * vary widely (roughly 100-160) by preacher, language and delivery. Treat
     * this as a rough gauge, and note the app shows it as a range for that
     * reason rather than a single confident number.
     */
    fun estimatedMinutesRange(): IntRange {
        val words = wordCount()
        val slow = words / 160
        val fast = words / 100
        return slow..maxOf(fast, slow)
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun encode(doc: RichDocument): String = json.encodeToString(doc)

        /** Never throws: a corrupt body becomes a single plain paragraph. */
        fun decode(raw: String): RichDocument = runCatching {
            json.decodeFromString<RichDocument>(raw)
        }.getOrElse {
            RichDocument(blocks = listOf(Block(BlockType.PARAGRAPH, listOf(Span(raw)))))
        }

        fun fromPlainText(text: String) = RichDocument(
            blocks = text.split("\n").map { line ->
                Block(BlockType.PARAGRAPH, listOf(Span(line)))
            }
        )
    }
}

@Serializable
data class Block(
    val type: BlockType = BlockType.PARAGRAPH,
    val spans: List<Span> = emptyList(),
    /** Nesting depth for list items. Ignored for other block types. */
    val indent: Int = 0
)

@Serializable
enum class BlockType {
    PARAGRAPH, HEADING1, HEADING2, HEADING3,
    BULLET, NUMBERED, BLOCKQUOTE,
    /** An inserted scripture quotation, rendered and exported distinctly. */
    SCRIPTURE
}

@Serializable
data class Span(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    /** ARGB, null = inherit. */
    val color: Int? = null,
    val highlight: Int? = null,
    /**
     * Set when this span is a live scripture link. Holds a BibleReference key
     * so the link survives translation changes and locale changes — the
     * displayed text may say "Jn 3:16" while this says "43.3.16-43.3.16".
     */
    val referenceKey: String? = null
)
