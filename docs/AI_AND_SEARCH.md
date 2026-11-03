# On-device AI and semantic search

## What ships today

`TemplateAssistant` — deterministic, offline, no model. It produces
**homiletical structure** (the shape a sermon of a given style takes) and
leaves every claim about the text to the preacher.

Where a less careful implementation would generate plausible-sounding
background or illustrations, this one emits a prompt asking the preacher to
supply them. That constraint is the point: a pastor can trust everything it
writes, because it never asserts anything about scripture it was not handed.

Six styles: Expository, Textual, Topical, Devotional, Youth, Small Group.

Cross-references come from a small hand-checked table. It contains the synoptic
parallels and nothing speculative. **Grow it only by verified addition** — a
wrong Messianic link repeated from a pulpit is a real cost, and an empty result
is an acceptable answer here.

## Adding the on-device LLM

`SermonAssistant` is the seam. A model-backed implementation binds in
`AssistantModule` behind a capability check, with the template path kept as the
fallback for low-RAM devices. Nothing above the interface changes.

What it actually costs, so you can plan rather than discover:

**Model distribution.** Quantized small models (Gemma 3n E2B, Phi-3 mini 4-bit,
Llama 3.2 1B/3B) run roughly 1–3 GB. They are **not redistributable in your
repo** — Gemma requires licence acceptance on Kaggle/Hugging Face, and the
others have their own terms. So: a runtime download, which needs a progress UI,
resumability, storage checks, and a user who consents to a multi-gigabyte
download. On Philippine mobile data that is a significant ask; make it
Wi-Fi-default.

**Device reality.** The spec's `<6GB RAM` fallback threshold is the right
instinct. A 2–3 GB model on a 4 GB phone will be killed by the OS under memory
pressure, often mid-generation. Check `ActivityManager.isLowRamDevice` and
total RAM, and fail over to templates rather than letting it OOM.

**Latency.** Expect tens of seconds for a full outline on mid-range hardware,
not the sub-second feel of the rest of the app. That needs its own UX:
streaming output, a visible cancel, and a warm-up state.

**Runtime choice.** MediaPipe LLM Inference is the lower-friction path on
Android; LiteRT gives more control. Both were moving quickly as of early 2026 —
**check current API surfaces before writing against either**, as my information
may be out of date.

**Honesty in the UI.** Whatever generates a scaffold, label it. `SermonScaffold`
carries `generatedBy` for exactly this. A preacher should always know whether a
line came from a machine.

## Semantic search

Not implemented. The search screen says so on screen rather than returning poor
keyword matches for a natural-language query and letting the user assume it
understood them.

To build it:

1. **Embeddings.** Embed all ~31,000 verses with a small sentence encoder.
   Precompute at build time in `scripts/`, not on device — on-device embedding
   of a whole Bible takes far too long for first-run.
2. **Storage.** 31,000 × 384 dimensions × 4 bytes ≈ 48 MB as float32. Quantize
   to int8 and it is ~12 MB, which is shippable. Store as a flat binary asset,
   not in SQLite rows.
3. **Search.** At 31,000 vectors a brute-force cosine scan is genuinely fast
   enough (single-digit milliseconds with int8 SIMD). **Do not reach for an ANN
   index at this scale** — it adds a dependency and failure mode for no
   measurable gain.
4. **Ranking.** Blend FTS and vector scores rather than choosing one. Keyword
   wins for "faith move mountains"; vectors win for "what was Jesus' first
   miracle".
5. **Question answering.** The spec's examples ("What was the first miracle of
   Jesus?" → John 2:1-11) are not reliably solved by embeddings alone. A small
   curated question→reference table will outperform a model for the common
   cases and will never be confidently wrong. Consider doing that first.
