package com.dlbc.sermonstudio.domain.assistant

import com.dlbc.sermonstudio.core.bible.BibleReference
import com.dlbc.sermonstudio.core.bible.BookCatalog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic, offline, no-model outline scaffolding.
 *
 * This produces *homiletical structure* — the shape a sermon of a given style
 * takes — and leaves every claim about the text to the preacher. It does not
 * generate exegesis, historical background, or illustrations, because it has no
 * way to know whether any of that would be true. Where a less honest
 * implementation would emit plausible-sounding filler, this one emits a prompt
 * asking the preacher to supply it.
 *
 * That constraint is the feature. A pastor can trust everything this writes,
 * because it never asserts anything about scripture it has not been handed.
 */
@Singleton
class TemplateAssistant @Inject constructor() : SermonAssistant {

    override val id: String = "template-v1"
    override val isAvailable: Boolean = true
    override val unavailableReason: String? = null

    override suspend fun outlineFromTopic(
        topic: String,
        request: OutlineRequest
    ): SermonScaffold {
        val shape = StyleShapes.forStyle(request.style)
        return SermonScaffold(
            title = topic.trim().replaceFirstChar { it.uppercase() },
            centralTheme = "[State in one sentence what you want the congregation " +
                "to believe or do about: ${topic.trim()}]",
            mainText = null,
            introduction = shape.introductionPrompt,
            points = (1..request.pointCount).map { n ->
                SermonPoint(
                    heading = "[Point $n — ${shape.pointPrompt}]",
                    supportingTexts = emptyList(),
                    developmentPrompt = shape.developmentPrompt
                )
            },
            illustrationPrompts = shape.illustrationPrompts,
            applications = shape.applicationPrompts,
            conclusion = shape.conclusionPrompt,
            callToAction = shape.callToActionPrompt,
            generatedBy = id
        )
    }

    override suspend fun outlineFromPassage(
        reference: BibleReference,
        request: OutlineRequest
    ): SermonScaffold {
        val shape = StyleShapes.forStyle(request.style)
        val book = BookCatalog.byNumber[reference.bookNumber]
        val display = reference.display()

        return SermonScaffold(
            title = "[Working title from $display]",
            centralTheme = "[One sentence: what does $display actually claim?]",
            mainText = reference,
            introduction = shape.introductionPrompt,
            points = (1..request.pointCount).map { n ->
                SermonPoint(
                    heading = "[Point $n — drawn from the text, not imposed on it]",
                    // Expository style anchors each point to a slice of the passage.
                    supportingTexts = if (request.style == SermonStyle.EXPOSITORY) {
                        listOf(reference)
                    } else {
                        emptyList()
                    },
                    developmentPrompt = shape.developmentPrompt
                )
            },
            illustrationPrompts = shape.illustrationPrompts,
            applications = buildList {
                addAll(shape.applicationPrompts)
                if (book != null) {
                    add(
                        "[Context check: who was ${book.englishName} written to, and " +
                            "what did this mean to them before it means anything to us?]"
                    )
                }
            },
            conclusion = shape.conclusionPrompt,
            callToAction = shape.callToActionPrompt,
            generatedBy = id
        )
    }

    /**
     * Returns curated cross-references only.
     *
     * The data set behind this is intentionally small and hand-checked (see
     * CrossReferenceTable). An algorithmic "related verses" feature that guesses
     * is worse than nothing in a preaching tool — a wrong Messianic link
     * repeated from a pulpit is a real cost. Empty is an acceptable answer here.
     */
    override suspend fun relatedPassages(reference: BibleReference): List<CrossReference> =
        CrossReferenceTable.lookup(reference)
}

private data class StyleShape(
    val introductionPrompt: String,
    val pointPrompt: String,
    val developmentPrompt: String,
    val illustrationPrompts: List<String>,
    val applicationPrompts: List<String>,
    val conclusionPrompt: String,
    val callToActionPrompt: String
)

private object StyleShapes {

    fun forStyle(style: SermonStyle): StyleShape = when (style) {
        SermonStyle.EXPOSITORY -> StyleShape(
            introductionPrompt = "[Open by raising the question the passage itself answers.]",
            pointPrompt = "a movement in the text, in the text's own order",
            developmentPrompt = "[Explain: what does the text say? Then: why does it say " +
                "it here? Only then: what follows for us?]",
            illustrationPrompts = listOf(
                "[One illustration per point, drawn from ordinary life your congregation shares.]"
            ),
            applicationPrompts = listOf(
                "[What should change on Monday because this passage is true?]",
                "[Who in this room is hearing this hardest, and what do they need said to them?]"
            ),
            conclusionPrompt = "[Restate the central theme. Do not introduce new material.]",
            callToActionPrompt = "[One specific, doable response. Not 'try harder'.]"
        )

        SermonStyle.TEXTUAL -> StyleShape(
            introductionPrompt = "[Read the short text. State why these few verses carry the weight.]",
            pointPrompt = "a phrase or clause of the text",
            developmentPrompt = "[Stay inside the text. Let each phrase set its own point.]",
            illustrationPrompts = listOf("[Keep illustrations short — the text is doing the work.]"),
            applicationPrompts = listOf("[Apply phrase by phrase rather than all at the end.]"),
            conclusionPrompt = "[Return to the whole text, read once more.]",
            callToActionPrompt = "[A response the text itself calls for.]"
        )

        SermonStyle.TOPICAL -> StyleShape(
            introductionPrompt = "[Name the topic and why it is urgent now.]",
            pointPrompt = "one facet of the topic, each with its own anchoring text",
            developmentPrompt = "[Guard against proof-texting: check each passage in context " +
                "before you use it. If it does not hold up, drop the point.]",
            illustrationPrompts = listOf("[Illustrations carry a topical sermon — plan them early.]"),
            applicationPrompts = listOf("[What does a faithful response to this topic look like?]"),
            conclusionPrompt = "[Tie the facets back into one claim.]",
            callToActionPrompt = "[A concrete next step on this topic.]"
        )

        SermonStyle.DEVOTIONAL -> StyleShape(
            introductionPrompt = "[Set a quiet, personal tone. Shorter than a Sunday opening.]",
            pointPrompt = "a single reflection",
            developmentPrompt = "[Aim for one idea, well turned, not three.]",
            illustrationPrompts = listOf("[One image, held throughout.]"),
            applicationPrompts = listOf("[A question to sit with, not a task list.]"),
            conclusionPrompt = "[Close with the text, unhurried.]",
            callToActionPrompt = "[An invitation to prayer or reflection.]"
        )

        SermonStyle.YOUTH -> StyleShape(
            introductionPrompt = "[Start with something true about their actual week.]",
            pointPrompt = "one idea, stated in one sentence they could repeat",
            developmentPrompt = "[Short movements. Ask more than you tell. Expect interruption.]",
            illustrationPrompts = listOf("[Concrete and current. Avoid references you have to explain.]"),
            applicationPrompts = listOf("[What does this look like at school this week?]"),
            conclusionPrompt = "[One sentence they will remember.]",
            callToActionPrompt = "[Something they can actually do before next week.]"
        )

        SermonStyle.SMALL_GROUP -> StyleShape(
            introductionPrompt = "[Opening question that surfaces experience before doctrine.]",
            pointPrompt = "a discussion movement",
            developmentPrompt = "[Write questions, not statements. Leave room for the group " +
                "to reach it before you do.]",
            illustrationPrompts = listOf("[Invite the group's own examples instead of supplying one.]"),
            applicationPrompts = listOf(
                "[Observation: what does the text say?]",
                "[Interpretation: what did it mean to them?]",
                "[Application: what does it ask of us?]"
            ),
            conclusionPrompt = "[Summarise what the group concluded, not what you planned.]",
            callToActionPrompt = "[A commitment the group can hold each other to.]"
        )
    }
}

/**
 * Hand-checked cross references.
 *
 * This table is small on purpose and should only grow by deliberate, verified
 * additions. Each entry below is a well-attested parallel. If you are not
 * certain of an entry, leave it out — see the class doc on [TemplateAssistant].
 */
private object CrossReferenceTable {

    private fun ref(book: Int, ch: Int, v: Int, endCh: Int = ch, endV: Int = v) =
        BibleReference(book, ch, v, endCh, endV)

    // Synoptic parallels for the feeding of the five thousand, which the spec
    // uses as its worked example.
    private val feedingFiveThousand = listOf(
        ref(40, 14, 13, 14, 21), // Matthew
        ref(41, 6, 30, 6, 44),   // Mark
        ref(42, 9, 10, 9, 17),   // Luke
        ref(43, 6, 1, 6, 14)     // John
    )

    private val table: Map<String, List<CrossReference>> = buildMap {
        feedingFiveThousand.forEach { source ->
            put(
                "${source.bookNumber}.${source.startChapter}",
                feedingFiveThousand
                    .filter { it.bookNumber != source.bookNumber }
                    .map { CrossReference(it, CrossReference.Relationship.PARALLEL_ACCOUNT) }
            )
        }
    }

    fun lookup(reference: BibleReference): List<CrossReference> =
        table["${reference.bookNumber}.${reference.startChapter}"].orEmpty()
}
