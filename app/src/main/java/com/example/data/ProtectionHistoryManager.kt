package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ProtectionHistoryItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ProtectionHistoryManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("wibes_history_prefs", Context.MODE_PRIVATE)

    fun getHistory(): List<ProtectionHistoryItem> {
        val raw = prefs.getString("history_items", null) ?: return emptyList()
        val list = mutableListOf<ProtectionHistoryItem>()
        try {
            val jsonArr = JSONArray(raw)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                list.add(
                    ProtectionHistoryItem(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        originalApkName = obj.optString("originalApkName", "app.apk"),
                        protectedApkName = obj.optString("protectedApkName", "app_protected.apk"),
                        originalSize = obj.optLong("originalSize", 0L),
                        protectedSize = obj.optLong("protectedSize", 0L),
                        profile = obj.optString("profile", "Maximum Protection"),
                        appliedCount = obj.optInt("appliedCount", 0),
                        durationMs = obj.optLong("durationMs", 0L),
                        outputFilePath = obj.optString("outputFilePath", ""),
                        outputFileUriString = if (obj.has("outputFileUriString") && !obj.isNull("outputFileUriString")) obj.getString("outputFileUriString") else null,
                        certFingerprintSha256 = obj.optString("certFingerprintSha256", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.timestamp }
    }

    fun addItem(item: ProtectionHistoryItem) {
        val current = getHistory().toMutableList()
        current.add(0, item)
        val jsonArr = JSONArray()
        for (h in current.take(50)) {
            val obj = JSONObject().apply {
                put("id", h.id)
                put("timestamp", h.timestamp)
                put("originalApkName", h.originalApkName)
                put("protectedApkName", h.protectedApkName)
                put("originalSize", h.originalSize)
                put("protectedSize", h.protectedSize)
                put("profile", h.profile)
                put("appliedCount", h.appliedCount)
                put("durationMs", h.durationMs)
                put("outputFilePath", h.outputFilePath)
                put("outputFileUriString", h.outputFileUriString)
                put("certFingerprintSha256", h.certFingerprintSha256)
            }
            jsonArr.put(obj)
        }
        prefs.edit().putString("history_items", jsonArr.toString()).apply()
    }

    fun clearHistory() {
        prefs.edit().remove("history_items").apply()
    }
}
