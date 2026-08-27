package com.smartagent.app.data

object PromptBuilder {
    fun build(request: ContentRequest): String {
        val languageInstruction = when (request.language) {
            OutputLanguage.MALAY -> "Write in natural conversational Malaysian Bahasa Melayu. Avoid Indonesian vocabulary, stiff translations, excessive slang, and forced English mixing."
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
        val platformInstruction = when (request.platform) {
            Platform.TIKTOK, Platform.SHOPEE_VIDEO, Platform.INSTAGRAM_REELS -> """
                This is a short-form video copy pack. Make every scene filmable on a phone.
                The storyboard must include timestamps, visual action, spoken line, and on-screen text.
                The full script must sound natural when spoken and must fit $voiceOverTarget.
            """.trimIndent()
            Platform.FACEBOOK -> """
                This is a Facebook copy pack, not a video script.
                Use storyboard for the post structure and persuasion flow.
                Use voice_over for the complete ready-to-publish Facebook post with short mobile-friendly paragraphs.
                Use caption for a shorter alternative caption. Do not include timestamps or camera directions.
            """.trimIndent()
            Platform.WHATSAPP -> """
                This is a WhatsApp copy pack, not a video script.
                Use storyboard for a clear three-part Status sequence.
                Use voice_over for one complete ready-to-send sales message.
                Use caption for one short WhatsApp Status version. Keep it personal and concise.
            """.trimIndent()
            Platform.THREADS -> "Threads uses its dedicated generator."
        }

        return """
            You are SmartAgent, a senior direct-response copywriter for Malaysian creators.

            Create a publish-ready content pack using the details below.

            $sourceDescription

            Platform: ${request.platform.label}
            Target duration: ${request.durationSeconds} seconds
            Content style: ${request.style.label}
            Marketing angle: ${request.marketingAngle.label}
            Angle direction: ${request.marketingAngle.instruction}
            Target audience: ${request.audience.ifBlank { "General Malaysian audience" }}
            $languageInstruction

            $brandVoiceInstruction
            $platformInstruction

            Copy quality rules:
            1. Lead with one concrete audience tension, desire, observation, or useful surprise.
            2. Prefer specific, simple sentences over generic hype or filler.
            3. Make the product connection feel earned. Do not force the product into the first sentence unless the selected style calls for it.
            4. Use one clear primary promise that is fully supported by the verified facts.
            5. Show how a feature matters in real use instead of merely listing features.
            6. Use a call to action that matches the platform and buying readiness of the audience.
            7. Avoid generic openings such as "Produk ini memang best", "Ramai tak tahu", or "Korang pernah tak" unless the supplied context makes them specific and credible.
            8. Do not use fake scarcity, fake authority, fake personal experience, or invented social proof.

            Accuracy rules:
            1. Never invent a price, discount, specification, rating, testimonial, benefit or product feature.
            2. Treat the editable verified fields above as the source of truth. Do not replace them with conflicting page text.
            3. Clearly mark any missing information that the creator should verify.
            4. Do not make unsupported medical, financial or guaranteed-result claims.
            5. Make the hook interesting without misleading viewers.
            6. Adapt the structure, call to action, pacing, and formatting specifically for ${request.platform.label}.

            Create exactly ${request.variantCount} complete content ${if (request.variantCount == 1) "pack" else "alternatives"}.
            ${if (request.marketingAngle == MarketingAngle.AUTO) "Each alternative must use a genuinely different creative angle." else "Keep the selected marketing angle, but make each alternative use a different hook and execution."}
            Keep every verified fact consistent across alternatives.

            Return a structured variants list. Every variant must contain these fields:
            - title: one short working title.
            - hooks: three clearly numbered and distinct opening hooks.
            - storyboard: follow the platform-specific instruction above.
            - voice_over: the complete primary copy described in the platform-specific instruction above.
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
            Marketing angle: ${request.marketingAngle.label}
            Angle direction: ${request.marketingAngle.instruction}
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
            ${if (request.marketingAngle == MarketingAngle.AUTO) "Each alternative must use a genuinely different angle." else "Keep the selected angle, but vary the opening and narrative execution."}
            Preserve every verified fact.

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
            Marketing angle: ${request.marketingAngle.label}. ${request.marketingAngle.instruction}
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
        Marketing angle: ${request.marketingAngle.label}. ${request.marketingAngle.instruction}
        Audience: ${request.audience.ifBlank { "General Malaysian audience" }}
        $languageInstruction
        Saved personal brand voice: ${request.brandVoice.promptText().ifBlank { "Not active" }}

        The creator already has the content pack below:

        ${currentPack.asPlainText(request.platform)}

        Regenerate only the section named "${section.displayLabel(request.platform)}". Keep it consistent with the verified product facts,
        platform, duration, audience, and the other existing sections. Never invent product facts or claims.
        Return only the replacement section content.
    """.trimIndent()
    }

    fun buildFlowPromptPack(request: ContentRequest, contentPack: ContentPack): String = """
        You are SmartAgent's production prompt writer.

        Convert the approved copy pack below into reusable visual-generation prompts for Flow or a similar external platform.
        SmartAgent does not generate video or audio. It only prepares prompts that the creator can copy elsewhere.

        Product or topic: ${request.productName}
        Verified facts:
        ${request.productFacts.ifBlank { "No additional verified facts supplied" }}
        Platform: ${request.platform.label}
        Duration: ${request.durationSeconds} seconds
        Audience: ${request.audience.ifBlank { "General Malaysian audience" }}

        APPROVED COPY PACK
        ${contentPack.asPlainText(request.platform)}

        Requirements:
        1. Write the visual prompts in precise English because they are intended for external generation tools.
        2. Preserve the exact product identity and all verified visible details. Never invent packaging, colours, claims, prices, people, locations, or results.
        3. Produce one scene prompt for every scene in the approved storyboard, in the same order and within the total duration.
        4. Each scene prompt must specify vertical 9:16 framing, subject, setting, action, composition, camera movement, lighting, mood, and approximate duration.
        5. Keep spoken copy and on-screen wording in the original content language. Do not translate it.
        6. The master prompt must define the overall visual identity and realistic phone-shot or creator style.
        7. The continuity prompt must keep the same person, clothing, product, packaging, room, lighting, and colour treatment across scenes when relevant.
        8. The negative prompt must prevent duplicated limbs or objects, malformed hands, distorted faces, changed product labels, unreadable text, watermarks, unrelated logos, and inconsistent product colours.
        9. Do not request automatic narration, music, voice cloning, or a finished video. These are production prompts only.

        Return:
        - master_prompt: one reusable visual foundation prompt.
        - scene_prompts: an ordered list containing scene_number, scene_title, and one complete standalone prompt.
        - continuity_prompt: one reusable consistency block.
        - negative_prompt: one reusable exclusion block.
        - usage_notes: brief instructions explaining the suggested order for copying these prompts into an external platform.
    """.trimIndent()
}
