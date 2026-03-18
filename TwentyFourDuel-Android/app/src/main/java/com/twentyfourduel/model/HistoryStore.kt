package com.twentyfourduel.model

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("game_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "game_history_v1"

    fun load(): List<GameRecord> {
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<GameRecord>>() {}.type
        return try {
            gson.fromJson<List<GameRecord>>(json, type).sortedByDescending { it.date }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(record: GameRecord) {
        val records = load().toMutableList()
        records.add(0, record)
        val trimmed = records.take(50)
        prefs.edit().putString(key, gson.toJson(trimmed)).apply()
    }

    fun delete(id: String) {
        val records = load().toMutableList()
        records.removeAll { it.id == id }
        prefs.edit().putString(key, gson.toJson(records)).apply()
    }
}
