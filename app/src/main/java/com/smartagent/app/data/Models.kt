package com.smartagent.app.data

enum class InputMode(val label: String) {
    AFFILIATE("Affiliate product"),
    ORGANIC("Organic idea")
}

enum class Platform(val label: String) {
    TIKTOK("TikTok"),
    SHOPEE_VIDEO("Shopee Video"),
    INSTAGRAM_REELS("Instagram Reels"),
    FACEBOOK("Facebook"),
    THREADS("Threads"),
    WHATSAPP("WhatsApp")
}

enum class OutputLanguage(val label: String) {
    MALAY("Bahasa Melayu"),
    ENGLISH("English")
}

enum class ContentStyle(val label: String) {
    UGC("UGC natural"),
    SOFT_SELL("Soft sell"),
    HARD_SELL("Hard sell"),
    STORY("Storytelling"),
    HONEST_REVIEW("Honest review"),
    FACELESS("Faceless video"),
    PROBLEM_SOLUTION("Problem solution"),
    EDUCATIONAL("Educational"),
    POV("POV"),
    COMPARISON("Comparison"),
    MYTH_BUSTER("Myth buster"),
    DEMO("Product demo"),
    TESTIMONIAL("Testimonial style")
}

data class ContentRequest(
    val mode: InputMode = InputMode.AFFILIATE,
    val productLink: String = "",
    val productName: String = "",
    val productFacts: String = "",
    val verificationSummary: String = "",
    val platform: Platform = Platform.TIKTOK,
    val durationSeconds: Int = 30,
    val language: OutputLanguage = OutputLanguage.MALAY,
    val style: ContentStyle = ContentStyle.UGC,
    val audience: String = "",
    val variantCount: Int = 1,
    val brandVoice: BrandVoiceProfile = BrandVoiceProfile(),
    val hasScreenshot: Boolean = false
)

data class BrandVoiceProfile(
    val name: String = "",
    val tone: String = "",
    val preferredCallToAction: String = "",
    val phrasesToUse: String = "",
    val phrasesToAvoid: String = ""
) {
    fun isConfigured(): Boolean = listOf(
        name,
        tone,
        preferredCallToAction,
        phrasesToUse,
        phrasesToAvoid
    ).any { it.isNotBlank() }

    fun promptText(): String = listOf(
        "Profile name" to name,
        "Tone and personality" to tone,
        "Preferred call to action" to preferredCallToAction,
        "Phrases to use" to phrasesToUse,
        "Phrases to avoid" to phrasesToAvoid
    ).filter { (_, value) -> value.isNotBlank() }
        .joinToString("\n") { (label, value) -> "$label: ${value.trim()}" }
}

fun recommendedVoiceOverWords(durationSeconds: Int): IntRange = when (durationSeconds) {
    10 -> 20..28
    15 -> 30..40
    30 -> 65..80
    60 -> 130..155
    120 -> 260..300
    else -> {
        val centre = (durationSeconds * 2.3).toInt().coerceAtLeast(1)
        (centre * 85 / 100)..(centre * 115 / 100)
    }
}

enum class ContentSection(val label: String) {
    TITLE("Title"),
    HOOKS("Hook options"),
    STORYBOARD("Storyboard"),
    VOICE_OVER("Full voice-over"),
    CAPTION("Caption"),
    HASHTAGS("Hashtags"),
    PINNED_COMMENT("Pinned comment"),
    CHECKLIST("Creator checklist")
}

data class ContentPack(
    val title: String,
    val hooks: String,
    val storyboard: String,
    val voiceOver: String,
    val caption: String,
    val hashtags: String,
    val pinnedComment: String,
    val checklist: String
) {
    fun contentFor(section: ContentSection): String = when (section) {
        ContentSection.TITLE -> title
        ContentSection.HOOKS -> hooks
        ContentSection.STORYBOARD -> storyboard
        ContentSection.VOICE_OVER -> voiceOver
        ContentSection.CAPTION -> caption
        ContentSection.HASHTAGS -> hashtags
        ContentSection.PINNED_COMMENT -> pinnedComment
        ContentSection.CHECKLIST -> checklist
    }

    fun replace(section: ContentSection, content: String): ContentPack = when (section) {
        ContentSection.TITLE -> copy(title = content)
        ContentSection.HOOKS -> copy(hooks = content)
        ContentSection.STORYBOARD -> copy(storyboard = content)
        ContentSection.VOICE_OVER -> copy(voiceOver = content)
        ContentSection.CAPTION -> copy(caption = content)
        ContentSection.HASHTAGS -> copy(hashtags = content)
        ContentSection.PINNED_COMMENT -> copy(pinnedComment = content)
        ContentSection.CHECKLIST -> copy(checklist = content)
    }

    fun asPlainText(): String = ContentSection.entries.joinToString("\n\n") { section ->
        "${section.label.uppercase()}\n${contentFor(section)}"
    }
}

fun List<ContentPack>.asVariantText(): String = mapIndexed { index, pack ->
    "ALTERNATIVE ${index + 1}\n\n${pack.asPlainText()}"
}.joinToString("\n\n====================\n\n")

data class GenerationRecord(
    val id: Long,
    val createdAt: Long,
    val title: String,
    val platform: String,
    val result: String
)

data class ProductDetails(
    val name: String,
    val price: String = "",
    val description: String = "",
    val features: List<String> = emptyList(),
    val seller: String = "",
    val promotion: String = "",
    val imageUrl: String = "",
    val resolvedUrl: String,
    val sourceLabel: String,
    val confidence: ExtractionConfidence,
    val warning: String = "",
    val requiresBrowserReview: Boolean = false
) {
    fun verifiedFactsText(): String = buildString {
        appendField("Price", price)
        appendField("Description", description)
        if (features.isNotEmpty()) {
            append("Features:\n")
            features.filter { it.isNotBlank() }.forEach { feature ->
                append("- ").append(feature.trim()).append('\n')
            }
        }
        appendField("Seller", seller)
        appendField("Promotion", promotion)
    }.trim()

    private fun StringBuilder.appendField(label: String, value: String) {
        val clean = value.trim()
        if (clean.isNotBlank()) append(label).append(": ").append(clean).append('\n')
    }
}

enum class ExtractionConfidence(val label: String) {
    HIGH("High confidence"),
    MEDIUM("Check details"),
    LOW("Needs review")
}

class ProductAccessBlockedException(message: String) : Exception(message)
