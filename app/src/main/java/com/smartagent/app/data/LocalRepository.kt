package com.smartagent.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LocalRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadModel(): String {
        val saved = preferences.getString(KEY_MODEL, DEFAULT_MODEL)
            ?.trim()
            .orEmpty()
        val normalised = saved.removePrefix("models/")
        return if (normalised == LEGACY_DEFAULT_MODEL || normalised.isBlank()) {
            preferences.edit().putString(KEY_MODEL, DEFAULT_MODEL).apply()
            DEFAULT_MODEL
        } else {
            normalised
        }
    }

    fun saveModel(model: String) {
        preferences.edit().putString(KEY_MODEL, model.trim().ifBlank { DEFAULT_MODEL }).apply()
    }

    fun loadBrandVoice(): BrandVoiceProfile = BrandVoiceProfile(
        name = preferences.getString(KEY_BRAND_NAME, "").orEmpty(),
        tone = preferences.getString(KEY_BRAND_TONE, "").orEmpty(),
        preferredCallToAction = preferences.getString(KEY_BRAND_CTA, "").orEmpty(),
        phrasesToUse = preferences.getString(KEY_BRAND_USE, "").orEmpty(),
        phrasesToAvoid = preferences.getString(KEY_BRAND_AVOID, "").orEmpty()
    )

    fun saveBrandVoice(profile: BrandVoiceProfile) {
        preferences.edit()
            .putString(KEY_BRAND_NAME, profile.name.trim())
            .putString(KEY_BRAND_TONE, profile.tone.trim())
            .putString(KEY_BRAND_CTA, profile.preferredCallToAction.trim())
            .putString(KEY_BRAND_USE, profile.phrasesToUse.trim())
            .putString(KEY_BRAND_AVOID, profile.phrasesToAvoid.trim())
            .apply()
    }

    fun clearBrandVoice() {
        preferences.edit()
            .remove(KEY_BRAND_NAME)
            .remove(KEY_BRAND_TONE)
            .remove(KEY_BRAND_CTA)
            .remove(KEY_BRAND_USE)
            .remove(KEY_BRAND_AVOID)
            .apply()
    }

    fun loadHistory(): List<GenerationRecord> {
        val raw = preferences.getString(KEY_HISTORY, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        GenerationRecord(
                            id = item.getLong("id"),
                            createdAt = item.getLong("createdAt"),
                            title = item.getString("title"),
                            platform = item.getString("platform"),
                            result = item.getString("result")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addHistory(record: GenerationRecord): List<GenerationRecord> {
        val updated = listOf(record) + loadHistory().filterNot { it.id == record.id }
        saveHistory(updated.take(MAX_HISTORY))
        return updated.take(MAX_HISTORY)
    }

    fun clearHistory() {
        preferences.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveHistory(records: List<GenerationRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("id", record.id)
                    .put("createdAt", record.createdAt)
                    .put("title", record.title)
                    .put("platform", record.platform)
                    .put("result", record.result)
            )
        }
        preferences.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    companion object {
        const val DEFAULT_MODEL = "gemini-3.5-flash-lite"
        private const val LEGACY_DEFAULT_MODEL = "gemini-2.5-flash-lite"
        private const val PREFS_NAME = "smartagent_local"
        private const val KEY_MODEL = "gemini_model"
        private const val KEY_HISTORY = "generation_history"
        private const val KEY_BRAND_NAME = "brand_name"
        private const val KEY_BRAND_TONE = "brand_tone"
        private const val KEY_BRAND_CTA = "brand_cta"
        private const val KEY_BRAND_USE = "brand_phrases_use"
        private const val KEY_BRAND_AVOID = "brand_phrases_avoid"
        private const val MAX_HISTORY = 50
    }
}
