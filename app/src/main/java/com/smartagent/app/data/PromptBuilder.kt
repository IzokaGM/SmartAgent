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

    fun buildThreads(request: ContentRequest): String {
        val languageInstruction = when (request.language) {
            OutputLanguage.MALAY -> "Write in natural conversational Malaysian Bahasa Melayu. Avoid Indonesian wording and stiff translations."
            OutputLanguage.ENGLISH -> "Write in natural conversational British English."
        }
        val brandVoiceInstruction = if (request.brandVoice.isConfigured()) {
            "Saved personal brand voice:\n${request.brandVoice.promptText()}"
        } else {
            "No saved personal brand voice is active."
        }
        val linkInstruction = when (request.threadLinkPlacement) {
            ThreadLinkPlacement.FINAL_REPLY -> if (request.productLink.isNotBlank()) {
                "Place this exact affiliate link once, only in the final reply: ${request.productLink.trim()}"
            } else {
                "No affiliate link was supplied. End with a natural call to action without inventing a link."
            }
            ThreadLinkPlacement.MAIN_POST -> if (request.productLink.isNotBlank()) {
                "Place this exact affiliate link once, only in the main post: ${request.productLink.trim()}"
            } else {
                "No affiliate link was supplied. Do not invent one."
            }
            ThreadLinkPlacement.OMIT -> "Do not include any link in the thread."
        }

        return """
            You are SmartAgent, a careful Threads copywriter for Malaysian creators.

            Create a genuine multi-post Threads chain, not a video script and not one long caption.

            Product or topic: ${request.productName.ifBlank { "Not supplied" }}
            Product link: ${request.productLink.ifBlank { "Not supplied" }}
            Verified facts:
            ${request.productFacts.ifBlank { "No verified facts supplied" }}
            Verification status: ${request.verificationSummary.ifBlank { "No verification recorded" }}
            Style: ${request.style.label}
            Audience: ${request.audience.ifBlank { "General Malaysian audience" }}
            $languageInstruction
            $brandVoiceInstruction

            Required thread structure:
            1. Main post: lead with a relatable situation, problem, opinion, or curiosity gap. Do not sound like an advertisement immediately.
            2. Early replies: continue the story naturally, then introduce the product or main idea and its verified features.
            3. Final reply: give a clear but natural call to action. $linkInstruction
            4. Produce exactly ${request.threadReplyCount} replies after the main post.
            5. Each part must make sense in sequence and be concise enough for comfortable mobile reading. Aim for no more than 450 characters per part.
            6. Do not repeat the same opening or product description in multiple replies.
            7. Do not put labels such as "Post utama" or "Reply 1" inside the generated text.
            8. Do not use fake personal experience, fake testimonials, invented urgency, or unsupported claims.
            9. Never invent a price, discount, specification, rating, benefit, or product feature.
            10. Use the exact supplied affiliate link without shortening or changing it, and only where instructed.

            Create exactly ${request.variantCount} complete thread ${if (request.variantCount == 1) "pack" else "alternatives"}.
            Each alternative must use a genuinely different angle while preserving every verified fact.

            Return a structured variants list. Every variant must contain:
            - title: a short internal working title.
            - main_post: the opening Threads post.
            - replies: exactly ${request.threadReplyCount} reply strings in posting order.
            - checklist: facts, prices, promotions, links, or claims the creator should check before publishing. If none are needed, write "No additional checks identified".
        """.trimIndent()
    }

    fun buildThreadPost(
        request: ContentRequest,
        postIndex: Int,
        currentPack: ThreadsPack
    ): String {
        val partLabel = if (postIndex == 0) "main post" else "reply $postIndex"
        val linkInstruction = when (request.threadLinkPlacement) {
            ThreadLinkPlacement.FINAL_REPLY -> "The exact link may appear only in reply ${request.threadReplyCount}: ${request.productLink}"
            ThreadLinkPlacement.MAIN_POST -> "The exact link may appear only in the main post: ${request.productLink}"
            ThreadLinkPlacement.OMIT -> "Do not include any link."
        }
        return """
            You are SmartAgent, a careful Threads copywriter for Malaysian creators.

            Regenerate only the $partLabel in the existing thread below.
            Keep the replacement consistent with the surrounding posts, verified facts, audience, style, language, and saved brand voice.
            Preserve the thread's narrative flow. Aim for no more than 450 characters. Return only the replacement text.

            Product or topic: ${request.productName}
            Verified facts: ${request.productFacts}
            Verification status: ${request.verificationSummary}
            Language: ${request.language.label}
            Style: ${request.style.label}
            Audience: ${request.audience.ifBlank { "General Malaysian audience" }}
            Saved personal brand voice: ${request.brandVoice.promptText().ifBlank { "Not active" }}
            $linkInstruction

            Existing thread:
            ${currentPack.asPlainText()}

            Accuracy rules: never invent product facts, prices, promotions, personal experience, testimonials, urgency, or benefits.
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
