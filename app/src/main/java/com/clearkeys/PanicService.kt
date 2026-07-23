package com.clearkeys

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

class PanicService : AccessibilityService() {

    companion object {
        const val TAG = "FCCPanic"
        @Volatile var isPanicActive = false
    }

    private val cryptoEngine = CryptoEngine()
    private val dictionaryManager by lazy { DictionaryManager(this) }
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val processedNodes = mutableSetOf<String>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        accessibilityServiceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        val dict = dictionaryManager.loadDictionary()
        cryptoEngine.loadDictionary(if (dict.isEmpty()) dictionaryManager.buildDefaultDictionary() else dict)
        Log.i(TAG, "Panic service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isPanicActive || event == null) return
        try {
            if (event.eventType in listOf(
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            )) {
                rootInActiveWindow?.let { processNode(it); it.recycle() }
            }
        } catch (e: Exception) { Log.e(TAG, "Error: ${e.message}") }
    }

    private fun processNode(node: AccessibilityNodeInfo) {
        try {
            if (node.text != null && node.text.isNotEmpty()) {
                val original = node.text.toString()
                val nodeId = "${node.viewIdResourceName}_${node.hashCode()}"
                if (!processedNodes.contains(nodeId) && needsCleaning(original)) {
                    val cleaned = cryptoEngine.encodeText(original)
                    if (cleaned != original) {
                        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,
                            Bundle().apply { putCharSequence("ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE", cleaned) })
                        processedNodes.add(nodeId)
                        Log.d(TAG, "Panic cleaned: ${original.take(30)}...")
                    }
                }
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let { processNode(it); it.recycle() }
        } catch (e: Exception) { Log.e(TAG, "Process error: ${e.message}") }
    }

    private fun needsCleaning(text: String): Boolean {
        if (text.length < 3) return false
        val words = text.split(Regex("\\s+"))
        val dict = cryptoEngine.getDictionary()
        return words.any { word ->
            val clean = word.trimEnd(',', '.', '!', '?', ':', ';', ')', ']').lowercase()
            dict.containsKey(clean)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "com.clearkeys.PANIC") activatePanic()
        if (intent?.action == "com.clearkeys.STOP_PANIC") deactivatePanic()
        return START_STICKY
    }

    private fun activatePanic() {
        isPanicActive = true
        processedNodes.clear()
        Log.w(TAG, "🚨 PANIC MODE ACTIVATED")
        serviceScope.launch {
            delay(100)
            rootInActiveWindow?.let { processNode(it); it.recycle() }
        }
        val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        vibrator.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun deactivatePanic() {
        isPanicActive = false
        processedNodes.clear()
        Log.w(TAG, "🚨 PANIC MODE DEACTIVATED")
    }

    override fun onInterrupt() { isPanicActive = false }
    override fun onDestroy() { serviceScope.cancel(); super.onDestroy() }
}
