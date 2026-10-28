package com.personal.rosy.service

import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.service.voice.VoiceInteractionService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.personal.rosy.BuildConfig
import com.personal.rosy.data.AppPreferences

class RosyService : Service() {

    private val tag = "RosyService"
    private val channelId = "rosy_wake_word_channel"
    private val notificationId = 1002

    // Picovoice configuration — key is injected via BuildConfig (see local.properties)
    private val picovoiceAccessKey = BuildConfig.PICOVOICE_ACCESS_KEY
    private val modelAssetPath = "hey_rosy_android.ppn"

    private var porcupineManager: PorcupineManager? = null
    private var isPorcupineActive = false

    companion object {
        const val ACTION_START_LISTENING = "com.personal.rosy.action.START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.personal.rosy.action.STOP_LISTENING"
        const val ACTION_PAUSE_FOR_INTERACTION = "com.personal.rosy.action.PAUSE_FOR_INTERACTION"
        const val ACTION_RESUME_AFTER_INTERACTION = "com.personal.rosy.action.RESUME_AFTER_INTERACTION"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(tag, "RosyService onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_LISTENING
        val prefs = AppPreferences(this)

        when (action) {
            ACTION_PAUSE_FOR_INTERACTION -> {
                Log.d(tag, "Pausing Porcupine so interaction can use mic/audio")
                stopPorcupine()
            }
            ACTION_RESUME_AFTER_INTERACTION -> {
                if (prefs.isAlwaysOnWakeWordEnabled) {
                    Log.d(tag, "Resuming Porcupine after interaction finished")
                    startPorcupine()
                }
            }
            ACTION_STOP_LISTENING -> {
                stopPorcupine()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START_LISTENING -> {
                if (!prefs.isAlwaysOnWakeWordEnabled) {
                    Log.i(tag, "Wake word is toggled OFF in preferences. Stopping service.")
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForeground(notificationId, buildNotification("Listening for \"Hey Rosy\"..."))
                startPorcupine()
            }
        }

        return START_STICKY
    }

    private fun startPorcupine() {
        if (isPorcupineActive) return

        try {
            val callback = PorcupineManagerCallback { keywordIndex ->
                if (keywordIndex == 0) {
                    Log.i(tag, "Wake word 'Hey Rosy' triggered!")
                    handleWakeWordTriggered()
                }
            }

            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(picovoiceAccessKey)
                .setKeywordPath(modelAssetPath)
                .setSensitivity(0.65f)
                .build(applicationContext, callback)

            porcupineManager?.start()
            isPorcupineActive = true
            Log.i(tag, "Porcupine started successfully")
        } catch (e: Exception) {
            Log.e(tag, "Failed to start Porcupine: ${e.message}", e)
            isPorcupineActive = false
        }
    }

    private fun stopPorcupine() {
        if (!isPorcupineActive) return
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
            porcupineManager = null
            isPorcupineActive = false
            Log.d(tag, "Porcupine stopped and microphone released")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping Porcupine: ${e.message}")
        }
    }

    private fun handleWakeWordTriggered() {
        // Crucial: Release mic BEFORE triggering assist session
        stopPorcupine()

        // Launch the system assist session programmatically
        val args = Bundle().apply {
            putBoolean("from_wake_word", true)
        }
        val launched = VoiceInteractionService.showSession(this, args, 0)
        Log.i(tag, "showSession from wake word launched: $launched")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Rosy Wake Word Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs only when 'Always-on wake word' is enabled"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Rosy Assistant")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPorcupine()
        Log.i(tag, "RosyService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
