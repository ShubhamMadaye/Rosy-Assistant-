package com.personal.rosy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.service.voice.VoiceInteractionService
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.personal.rosy.data.AppPreferences
import com.personal.rosy.service.RosyService

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: AppPreferences
    private lateinit var switchWakeWord: SwitchCompat
    private lateinit var tvWakeWordStatus: TextView
    private lateinit var btnSetDefaultAssistant: Button
    private lateinit var btnTestAssist: Button
    private lateinit var btnBatterySettings: Button

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableWakeWordService()
        } else {
            switchWakeWord.isChecked = false
            prefs.isAlwaysOnWakeWordEnabled = false
            Toast.makeText(this, "Microphone permission required for wake word", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = AppPreferences(this)

        switchWakeWord = findViewById(R.id.switchWakeWord)
        tvWakeWordStatus = findViewById(R.id.tvWakeWordStatus)
        btnSetDefaultAssistant = findViewById(R.id.btnSetDefaultAssistant)
        btnTestAssist = findViewById(R.id.btnTestAssist)
        btnBatterySettings = findViewById(R.id.btnBatterySettings)

        // Wake word toggle (OFF by default)
        switchWakeWord.isChecked = prefs.isAlwaysOnWakeWordEnabled
        updateWakeWordStatusText(prefs.isAlwaysOnWakeWordEnabled)

        switchWakeWord.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    enableWakeWordService()
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            } else {
                disableWakeWordService()
            }
        }

        // Open System Digital Assistant settings directly
        btnSetDefaultAssistant.setOnClickListener {
            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback to manage default apps
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                } catch (_: Exception) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }

        // Test overlay button
        btnTestAssist.setOnClickListener {
            val launched = VoiceInteractionService.showSession(this, Bundle(), 0)
            if (!launched) {
                Toast.makeText(
                    this,
                    "Please set Rosy as your Default Digital Assistant app first!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // ColorOS Battery Optimizations Shortcut
        btnBatterySettings.setOnClickListener {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Battery optimization already disabled for Rosy", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enableWakeWordService() {
        prefs.isAlwaysOnWakeWordEnabled = true
        updateWakeWordStatusText(true)
        val intent = Intent(this, RosyService::class.java).apply {
            action = RosyService.ACTION_START_LISTENING
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun disableWakeWordService() {
        prefs.isAlwaysOnWakeWordEnabled = false
        updateWakeWordStatusText(false)
        val intent = Intent(this, RosyService::class.java).apply {
            action = RosyService.ACTION_STOP_LISTENING
        }
        startService(intent)
    }

    private fun updateWakeWordStatusText(enabled: Boolean) {
        if (enabled) {
            tvWakeWordStatus.text = "Always-on wake word active (Foreground Service)"
            tvWakeWordStatus.setTextColor(0xFFFF80AB.toInt())
        } else {
            tvWakeWordStatus.text = "Service inactive (zero idle battery/mic usage)"
            tvWakeWordStatus.setTextColor(0xFF00E676.toInt())
        }
    }
}
