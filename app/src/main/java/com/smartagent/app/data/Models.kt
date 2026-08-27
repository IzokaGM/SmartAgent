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

enum class MarketingAngle(val label: String, val instruction: String) {
    AUTO("Let SmartAgent choose", "Choose the strongest truthful angle for the supplied facts and audience."),
    PAIN_POINT("Pain point", "Lead with one specific audience problem, then connect the product to a practical solution."),
    BENEFIT("Main benefit", "Lead with the most relevant verified benefit or use case without exaggeration."),
    DEMONSTRATION("Show it working", "Build the copy around a clear product demonstration and observable result."),
    VALUE("Value for money", "Focus on verified value, convenience, durability, quantity, or savings. Do not invent comparisons."),
    LIFESTYLE("Lifestyle fit", "Show naturally where the product fits into the target audience's daily routine."),
    CURIOSITY("Curiosity", "Open with a credible curiosity gap and reveal the answer progressively without clickbait.")
}

enum class ThreadLength(val label: String, val replyCount: Int) {
    SHORT("Short: 1 reply", 1),
    STANDARD("Standard: 2 replies", 2),
    LONG("Long: 4 replies", 4)
}

enum class ThreadLinkPlacement(val label: String) {
    FINAL_REPLY("Final reply"),
    MAIN_POST("Main post"),
    OMIT("Do not include")
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
    val marketingAngle: MarketingAngle = MarketingAngle.AUTO,
    val audience: String = "",
    val variantCount: Int = 1,
    val threadReplyCount: Int = ThreadLength.STANDARD.replyCount,
    val threadLinkPlacement: ThreadLinkPlacement = ThreadLinkPlacement.FINAL_REPLY,
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

    ;

    fun displayLabel(platform: Platform): String = when (this) {
        TITLE -> "Title"
        HOOKS -> if (platform == Platform.WHATSAPP) "Opening options" else "Hook options"
        STORYBOARD -> when (platform) {
            Platform.FACEBOOK -> "Post structure"
            Platform.WHATSAPP -> "Status sequence"
            else -> "Storyboard"
        }
        VOICE_OVER -> when (platform) {
            Platform.FACEBOOK -> "Ready-to-publish post"
            Platform.WHATSAPP -> "Ready-to-send copy"
            else -> "Full script"
        }
        CAPTION -> when (platform) {
            Platform.FACEBOOK -> "Short caption"
            Platform.WHATSAPP -> "Short status"
            else -> "Caption"
        }
        HASHTAGS -> "Hashtags"
        PINNED_COMMENT -> if (platform == Platform.WHATSAPP) "Reply starter" else "Pinned comment"
        CHECKLIST -> "Creator checklist"
    }
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

    fun asPlainText(platform: Platform? = null): String = ContentSection.entries.joinToString("\n\n") { section ->
        val label = platform?.let(section::displayLabel) ?: section.label
        "${label.uppercase()}\n${contentFor(section)}"
    }
}

fun List<ContentPack>.asVariantText(platform: Platform? = null): String = mapIndexed { index, pack ->
    "ALTERNATIVE ${index + 1}\n\n${pack.asPlainText(platform)}"
}.joinToString("\n\n====================\n\n")

data class FlowScenePrompt(
    val sceneNumber: Int,
    val sceneTitle: String,
    val prompt: String
)

data class FlowPromptPack(
    val masterPrompt: String,
    val scenes: List<FlowScenePrompt>,
    val continuityPrompt: String,
    val negativePrompt: String,
    val usageNotes: String
) {
    fun asPlainText(): String = buildString {
        append("MASTER VISUAL PROMPT\n").append(masterPrompt.trim())
        scenes.forEach { scene ->
            append("\n\nSCENE ").append(scene.sceneNumber)
                .append(": ").append(scene.sceneTitle.trim())
                .append('\n').append(scene.prompt.trim())
        }
        append("\n\nCONTINUITY PROMPT\n").append(continuityPrompt.trim())
        append("\n\nNEGATIVE PROMPT\n").append(negativePrompt.trim())
        append("\n\nUSAGE NOTES\n").append(usageNotes.trim())
    }
}

data class ThreadsPack(
    val title: String,
    val mainPost: String,
    val replies: List<String>,
    val checklist: String
) {
    fun posts(): List<String> = listOf(mainPost) + replies

    fun replacePost(index: Int, content: String): ThreadsPack = when {
        index == 0 -> copy(mainPost = content)
        index in 1..replies.size -> copy(
            replies = replies.toMutableList().also { it[index - 1] = content }
        )
        else -> this
    }

    fun asPlainText(): String = buildString {
        append("TITLE\n").append(title.trim())
        append("\n\nPOST UTAMA\n").append(mainPost.trim())
        replies.forEachIndexed { index, reply ->
            append("\n\nREPLY ").append(index + 1).append('\n').append(reply.trim())
        }
        append("\n\nCREATOR CHECKLIST\n").append(checklist.trim())
    }
}

fun List<ThreadsPack>.asThreadsVariantText(): String = mapIndexed { index, pack ->
    "THREAD ALTERNATIVE ${index + 1}\n\n${pack.asPlainText()}"
}.joinToString("\n\n====================\n\n")

data class GenerationRecord(
    val id: Long,
    val createdAt: Long,
    val title: String,
    val platform: String,
    val result: String,
    val isFavourite: Boolean = false
)

data class SavedProduct(
    val id: Long,
    val updatedAt: Long,
    val link: String,
    val name: String,
    val price: String = "",
    val seller: String = "",
    val promotion: String = "",
    val description: String = "",
    val features: String = "",
    val additionalFacts: String = ""
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
