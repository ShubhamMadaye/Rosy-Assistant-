package com.personal.rosy.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TtsManager(
    context: Context,
    private val onInitialized: () -> Unit = {}
) {
    private val tag = "TtsManager"
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val result = engine.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(tag, "Locale.US not supported, falling back to default locale")
                        engine.language = Locale.getDefault()
                    }
                    isReady = true
                    Log.d(tag, "TTS ready")
                    onInitialized()
                }
            } else {
                Log.e(tag, "TTS init error: $status")
            }
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isReady) {
            Log.w(tag, "speak() called before TTS was ready")
            onDone?.invoke()
            return
        }

        val utteranceId = "rosy_${System.currentTimeMillis()}"
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    onDone?.invoke()
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId) {
                    onDone?.invoke()
                }
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down TTS: ${e.message}")
        } finally {
            tts = null
            isReady = false
        }
    }
}
