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
        private const val MAX_HISTORY = 50
    }
}
