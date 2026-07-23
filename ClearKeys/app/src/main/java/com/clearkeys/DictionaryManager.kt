package com.clearkeys

import android.content.Context
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject

class DictionaryManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "clearkeys_dict"
        private const val KEY_DICTIONARY = "dictionary_json"
    }

    fun saveDictionary(dictionary: Map<String, String>) {
        val json = JSONObject(dictionary).toString()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_DICTIONARY, json).apply()
    }

    fun loadDictionary(): Map<String, String> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DICTIONARY, null) ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        JSONObject(json).keys().forEach { key -> result[key] = json.getString(key) }
        return result
    }

    fun generateQRCode(dictionary: Map<String, String>, size: Int = 600): BitMatrix {
        val json = JSONObject(dictionary).toString()
        val compressed = Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)
        return QRCodeWriter().encode(compressed, BarcodeFormat.QR_CODE, size, size)
    }

    fun decodeQRCode(qrContent: String): Map<String, String>? {
        return try {
            val json = String(Base64.decode(qrContent, Base64.NO_WRAP))
            val result = mutableMapOf<String, String>()
            JSONObject(json).keys().forEach { key -> result[key] = json.getString(key) }
            result
        } catch (e: Exception) { null }
    }

    fun buildDefaultDictionary(): Map<String, String> = mapOf(
        "meet" to "coffee", "meeting" to "lunch", "secret" to "private",
        "password" to "recipe", "hack" to "fix", "attack" to "visit",
        "weapon" to "umbrella", "kill" to "hug", "bomb" to "cake",
        "explosive" to "birthday", "drug" to "vitamin", "cocaine" to "sugar",
        "heroin" to "candy", "malware" to "homework", "virus" to "cold",
        "trojan" to "gift", "backdoor" to "shortcut", "exploit" to "feature",
        "ransomware" to "invoice", "phishing" to "fishing", "spy" to "friend",
        "surveillance" to "babysitting", "censorship" to "spellcheck",
        "propaganda" to "advertisement", "revolution" to "renovation",
        "protest" to "picnic", "riot" to "festival", "underground" to "basement",
        "illegal" to "unusual", "torture" to "massage", "kidnap" to "surprise",
        "assassinate" to "retire", "smuggle" to "deliver", "launder" to "wash",
        "fraud" to "discount", "corruption" to "networking", "bribe" to "tip",
        "mafia" to "association", "cartel" to "cooperative",
        "terrorist" to "tourist", "militant" to "athlete",
    )
}
