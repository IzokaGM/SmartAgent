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
    val facts: String,
    val resolvedUrl: String,
    val retrievalNote: String = ""
)

class ProductAccessBlockedException(message: String) : Exception(message)
