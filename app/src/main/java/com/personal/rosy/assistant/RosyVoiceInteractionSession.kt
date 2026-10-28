package com.personal.rosy.assistant

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.personal.rosy.R
import com.personal.rosy.service.RosyService
import com.personal.rosy.tts.TtsManager

class RosyVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    private val tag = "RosyAssistSession"
    private var ttsManager: TtsManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var tvStatus: TextView? = null
    private var ivOrb: ImageView? = null
    private var orbAnimator: ObjectAnimator? = null

    override fun onCreateContentView(): View {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.layout_assist_overlay, null)

        tvStatus = view.findViewById(R.id.tvAssistantStatus)
        ivOrb = view.findViewById(R.id.ivOrb)

        // Dismiss when tapping outside the card
        view.findViewById<View>(R.id.overlayRoot).setOnClickListener {
            dismissSession()
        }

        // Setup subtle breathing/pulse animation on the orb
        ivOrb?.let { orb ->
            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.18f, 1.0f)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.18f, 1.0f)
            orbAnimator = ObjectAnimator.ofPropertyValuesHolder(orb, scaleX, scaleY).apply {
                duration = 1200
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }
        }

        return view
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.i(tag, "Assist session shown (Power button / Assist triggered)")

        // 1. Tell background service (if wake-word was active) to pause Porcupine mic usage immediately
        val pauseIntent = Intent(context, RosyService::class.java).apply {
            action = RosyService.ACTION_PAUSE_FOR_INTERACTION
        }
        try {
            context.startService(pauseIntent)
        } catch (_: Exception) {}

        // 2. Start orb animation
        orbAnimator?.start()
        tvStatus?.text = "Yes?"

        // 3. Create TTS on-demand only when triggered
        ttsManager = TtsManager(context) {
            ttsManager?.speak("Yes?") {
                mainHandler.post {
                    // Small delay so user hears the response before overlay closes
                    mainHandler.postDelayed({
                        dismissSession()
                    }, 800)
                }
            }
        }
    }

    private fun dismissSession() {
        finish()
    }

    override fun onHide() {
        super.onHide()
        releaseResources()

        // Resume wake word engine if user has enabled it
        val resumeIntent = Intent(context, RosyService::class.java).apply {
            action = RosyService.ACTION_RESUME_AFTER_INTERACTION
        }
        try {
            context.startService(resumeIntent)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseResources()
    }

    private fun releaseResources() {
        orbAnimator?.cancel()
        ttsManager?.shutdown()
        ttsManager = null
    }
}
