package com.dlbc.sermonstudio.domain.assistant

import com.dlbc.sermonstudio.core.bible.BibleReference

/**
 * The boundary between the app and whatever generates outline scaffolding.
 *
 * v1 ships exactly one implementation: [TemplateAssistant], which is
 * deterministic and runs anywhere. The on-device LLM implementation is
 * deliberately absent — see docs/AI_AND_SEARCH.md for why, and for what adding
 * it actually costs.
 *
 * The short version: a quantized small model is a 1-3 GB download the user must
 * accept, the model files are not redistributable in your repo, and inference on
 * a mid-range Android phone is slow enough that it needs its own UX (streaming,
 * cancellation, a warm-up screen). None of that is work you want entangled with
 * getting the editor and reader right. Keep this interface stable and the LLM
 * becomes an additive change later, not a rewrite.
 */
interface SermonAssistant {

    val id: String
    val isAvailable: Boolean

    /**
     * Why the assistant is unavailable, for display. Null when available.
     * The UI shows this instead of silently falling back, so the user is never
     * misled about whether a machine wrote something.
     */
    val unavailableReason: String?

    suspend fun outlineFromTopic(topic: String, request: OutlineRequest): SermonScaffold

    suspend fun outlineFromPassage(
        reference: BibleReference,
        request: OutlineRequest
    ): SermonScaffold

    /**
     * Related passages for [reference]. The template implementation returns
     * curated cross-references only — it does not invent links.
     */
    suspend fun relatedPassages(reference: BibleReference): List<CrossReference>
}

data class OutlineRequest(
    val style: SermonStyle = SermonStyle.EXPOSITORY,
    val pointCount: Int = 3,
    val language: String = "en"
)

enum class SermonStyle(val displayKey: String) {
    EXPOSITORY("style_expository"),
    TEXTUAL("style_textual"),
    TOPICAL("style_topical"),
    DEVOTIONAL("style_devotional"),
    YOUTH("style_youth"),
    SMALL_GROUP("style_small_group")
}

/**
 * A scaffold, not a sermon.
 *
 * The naming is deliberate and it matters. What this produces is an empty
 * structure with prompts in it — headings, question stubs, slots for the
 * preacher's own exegesis. It is not content, and the UI labels it as a
 * starting structure so no one mistakes generated scaffolding for study they
 * have not done.
 */
data class SermonScaffold(
    val title: String,
    val centralTheme: String,
    val mainText: BibleReference?,
    val introduction: String,
    val points: List<SermonPoint>,
    val illustrationPrompts: List<String>,
    val applications: List<String>,
    val conclusion: String,
    val callToAction: String,
    /** Set when the scaffold came from a template, so the UI can say so. */
    val generatedBy: String
)

data class SermonPoint(
    val heading: String,
    val supportingTexts: List<BibleReference>,
    val developmentPrompt: String
)

data class CrossReference(
    val reference: BibleReference,
    val relationship: Relationship,
    val note: String? = null
) {
    enum class Relationship {
        PARALLEL_ACCOUNT,
        OT_SOURCE_OF_NT_QUOTE,
        MESSIANIC_PROPHECY,
        THEMATIC
    }
}
