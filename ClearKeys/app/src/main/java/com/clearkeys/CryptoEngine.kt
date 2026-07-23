package com.clearkeys

import java.security.SecureRandom

class CryptoEngine {

    private val random = SecureRandom()
    private val encodeMap = mutableMapOf<String, String>()
    private val decodeMap = mutableMapOf<String, String>()

    fun loadDictionary(dictionary: Map<String, String>) {
        encodeMap.clear()
        decodeMap.clear()
        encodeMap.putAll(dictionary)
        dictionary.forEach { (key, value) -> decodeMap[value] = key }
    }

    fun getDictionary(): Map<String, String> = encodeMap.toMap()

    fun encodeWord(word: String): String {
        val lower = word.lowercase()
        encodeMap[lower]?.let { return preserveCase(it, word) }
        if (lower.length < 3) return word
        val replacement = generateInnocentWord(lower.length)
        encodeMap[lower] = replacement
        decodeMap[replacement] = lower
        return preserveCase(replacement, word)
    }

    fun decodeWord(word: String): String {
        val lower = word.lowercase()
        decodeMap[lower]?.let { return preserveCase(it, word) }
        return word
    }

    fun encodeText(text: String): String {
        val words = text.split(Regex("(?<=\\s)|(?=\\s)"))
        val result = StringBuilder()
        for (word in words) {
            if (word.isBlank()) { result.append(word); continue }
            val cleanWord = word.trimEnd(',', '.', '!', '?', ':', ';', ')', ']')
            val punctuation = word.substring(cleanWord.length)
            result.append(if (cleanWord.all { it.isLetter() }) encodeWord(cleanWord) else cleanWord)
            result.append(punctuation)
        }
        return result.toString()
    }

    fun decodeText(text: String): String {
        val words = text.split(Regex("(?<=\\s)|(?=\\s)"))
        val result = StringBuilder()
        for (word in words) {
            if (word.isBlank()) { result.append(word); continue }
            val cleanWord = word.trimEnd(',', '.', '!', '?', ':', ';', ')', ']')
            val punctuation = word.substring(cleanWord.length)
            result.append(if (cleanWord.all { it.isLetter() }) decodeWord(cleanWord) else cleanWord)
            result.append(punctuation)
        }
        return result.toString()
    }

    private fun generateInnocentWord(length: Int): String {
        val innocent = listOf(
            "cat", "dog", "bird", "tree", "sky", "water", "book", "table",
            "chair", "lamp", "door", "window", "garden", "flower", "mountain",
            "river", "ocean", "cloud", "sun", "moon", "star", "wind", "rain",
            "snow", "bread", "milk", "coffee", "tea", "sugar", "salt",
            "apple", "orange", "banana", "grape", "lemon", "cherry",
            "house", "room", "kitchen", "bathroom", "bedroom", "garage",
            "car", "bus", "train", "bicycle", "road", "street", "park",
            "school", "office", "shop", "market", "library", "museum",
            "music", "movie", "photo", "painting", "sculpture", "dance",
            "happy", "calm", "kind", "warm", "soft", "bright", "gentle",
            "morning", "evening", "night", "day", "week", "month", "year",
            "friend", "family", "neighbor", "colleague", "teacher", "student",
            "pizza", "pasta", "salad", "soup", "cake", "cookie", "candy",
            "summer", "winter", "spring", "autumn", "holiday", "weekend",
            "phone", "computer", "screen", "keyboard", "mouse", "printer",
            "shirt", "shoes", "jacket", "hat", "gloves", "scarf", "socks"
        )
        val suitable = innocent.filter { it.length in (length - 2)..(length + 2) }
        return if (suitable.isNotEmpty()) suitable[random.nextInt(suitable.size)]
        else (1..length).map { ('a'..'z').random() }.joinToString("")
    }

    private fun preserveCase(modified: String, original: String): String {
        if (original.all { it.isUpperCase() }) return modified.uppercase()
        if (original.first().isUpperCase()) return modified.replaceFirstChar { it.uppercase() }
        return modified
    }
}
