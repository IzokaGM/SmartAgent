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
            Verification status:
            ${request.verificationSummary.ifBlank { "No extraction verification recorded" }}
            ${if (request.hasScreenshot) "A product screenshot is attached. Extract only clearly visible facts from it." else "No screenshot is attached."}
            """.trimIndent()
        } else {
            """
            Content topic: ${request.productName.ifBlank { "Not supplied" }}
            User notes:
            ${request.productFacts.ifBlank { "No additional notes supplied" }}
            """.trimIndent()
        }

        val voiceOverTarget = when (request.durationSeconds) {
            10 -> "20 to 28 words"
            15 -> "30 to 40 words"
            30 -> "65 to 80 words"
            60 -> "130 to 155 words"
            120 -> "260 to 300 words"
            else -> "a natural speaking length for ${request.durationSeconds} seconds"
        }
        val brandVoiceInstruction = if (request.brandVoice.isConfigured()) {
            """
            Saved personal brand voice:
            ${request.brandVoice.promptText()}
            Follow this voice consistently unless it conflicts with accuracy or safety rules.
            """.trimIndent()
        } else {
            "No saved personal brand voice is active."
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

            $brandVoiceInstruction

            Accuracy rules:
            1. Never invent a price, discount, specification, rating, testimonial, benefit or product feature.
            2. Treat the editable verified fields above as the source of truth. Do not replace them with conflicting page text.
            3. Clearly mark any missing information that the creator should verify.
            4. Do not make unsupported medical, financial or guaranteed-result claims.
            5. Keep the spoken script realistic for the selected duration.
            6. Make the hook interesting without misleading viewers.
            7. Target $voiceOverTarget for the full voice-over and do not exceed the selected duration.
            8. Adapt the caption, call to action, pacing, and formatting specifically for ${request.platform.label}.

            Create exactly ${request.variantCount} complete content ${if (request.variantCount == 1) "pack" else "alternatives"}.
            Each alternative must use a genuinely different creative angle while keeping every verified fact consistent.

            Return a structured variants list. Every variant must contain these fields:
            - title: one short working title.
            - hooks: three clearly numbered and distinct opening hooks.
            - storyboard: a scene-by-scene plan with timestamps, visual or action, voice-over, and on-screen text. The final timestamp must fit the selected duration.
            - voice_over: one clean voice-over script that can be recorded directly.
            - caption: a platform-appropriate caption with a clear call to action.
            - hashtags: five to eight relevant hashtags. Never promise that they will make the post viral.
            - pinned_comment: one short comment designed to invite a genuine response.
            - checklist: facts or claims to check before publishing. If none are needed, write "No additional checks identified".
        """.trimIndent()
    }

    fun buildSection(
        request: ContentRequest,
        section: ContentSection,
        currentPack: ContentPack
    ): String {
        val languageInstruction = when (request.language) {
            OutputLanguage.MALAY -> "Write in natural Malaysian Bahasa Melayu. Avoid stiff translated language."
            OutputLanguage.ENGLISH -> "Write in clear British English."
        }
        return """
        You are SmartAgent, a careful content assistant for Malaysian creators.

        Product or topic: ${request.productName}
        Verified facts: ${request.productFacts}
        Verification status: ${request.verificationSummary}
        Platform: ${request.platform.label}
        Duration: ${request.durationSeconds} seconds
        Style: ${request.style.label}
        Audience: ${request.audience.ifBlank { "General Malaysian audience" }}
        $languageInstruction
        Saved personal brand voice: ${request.brandVoice.promptText().ifBlank { "Not active" }}

        The creator already has the content pack below:

        ${currentPack.asPlainText()}

        Regenerate only the section named "${section.label}". Keep it consistent with the verified product facts,
        platform, duration, audience, and the other existing sections. Never invent product facts or claims.
        Return only the replacement section content.
    """.trimIndent()
    }
}
