package com.smartagent.app.data

object PromptBuilder {
    fun build(request: ContentRequest): String {
        val languageInstruction = when (request.language) {
            OutputLanguage.MALAY -> "Write in natural Malaysian Bahasa Melayu. Avoid stiff translated language."
            OutputLanguage.ENGLISH -> "Write in clear British English."
        }

        val sourceDescription = if (request.mode == InputMode.AFFILIATE) {
            """
            Product name: ${request.productName.ifBlank { "Not supplied" }}
            Product link: ${request.productLink.ifBlank { "Not supplied" }}
            Verified product facts supplied by the user:
            ${request.productFacts.ifBlank { "No verified facts supplied" }}
            ${if (request.hasScreenshot) "A product screenshot is attached. Extract only clearly visible facts from it." else "No screenshot is attached."}
            """.trimIndent()
        } else {
            """
            Content topic: ${request.productName.ifBlank { "Not supplied" }}
            User notes:
            ${request.productFacts.ifBlank { "No additional notes supplied" }}
            """.trimIndent()
        }

        return """
            You are SmartAgent, a careful content assistant for Malaysian creators.

            Create a publish-ready content pack using the details below.

            $sourceDescription

            Platform: ${request.platform.label}
            Target duration: ${request.durationSeconds} seconds
            Content style: ${request.style.label}
            Target audience: ${request.audience.ifBlank { "General Malaysian audience" }}
            $languageInstruction

            Accuracy rules:
            1. Never invent a price, discount, specification, rating, testimonial, benefit or product feature.
            2. Clearly mark any missing information that the creator should verify.
            3. Do not make unsupported medical, financial or guaranteed-result claims.
            4. Keep the spoken script realistic for the selected duration.
            5. Make the hook interesting without misleading viewers.

            Return exactly these sections:

            TITLE
            One short working title.

            HOOK OPTIONS
            Three distinct opening hooks.

            STORYBOARD
            A scene-by-scene plan with timestamp, visual/action, voice-over and on-screen text. Ensure the final timestamp fits the selected duration.

            FULL VOICE-OVER
            One clean voice-over script that can be recorded directly.

            CAPTION
            A platform-appropriate caption with a clear call to action.

            HASHTAGS
            Five to eight relevant hashtags. Do not promise that they will make the post viral.

            PINNED COMMENT
            One short comment designed to invite a genuine response.

            CREATOR CHECKLIST
            List facts or claims that should be checked before publishing. If none are needed, say "No additional checks identified".
        """.trimIndent()
    }
}
