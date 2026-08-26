package com.smartagent.app.data

enum class InputMode(val label: String) {
    AFFILIATE("Affiliate product"),
    ORGANIC("Organic idea")
}

enum class Platform(val label: String) {
    TIKTOK("TikTok"),
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
    STORY("Storytelling"),
    HONEST_REVIEW("Honest review"),
    FACELESS("Faceless video"),
    PROBLEM_SOLUTION("Problem solution")
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
    val hasScreenshot: Boolean = false
)

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
