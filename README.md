# 🌸 Rosy Assistant

A lightweight Android voice assistant that integrates directly with the **Android System Assistant** framework (`VoiceInteractionService`). Rosy replaces your device's default assistant (like Google Assistant) and can be triggered via the **long-press Home button**, **power button gesture**, or an optional **always-on "Hey Rosy" wake word** powered by Picovoice Porcupine.

---

## ✨ Features

- **System-level assistant integration** — registered as a proper Android `VoiceInteractionService`, not a workaround
- **Custom overlay UI** — animated orb with a breathing pulse animation appears on trigger
- **Text-to-Speech response** — speaks "Yes?" using Android's built-in TTS engine (no internet required)
- **Optional always-on wake word** — "Hey Rosy" via Picovoice Porcupine (OFF by default, zero battery/mic idle usage when off)
- **Mic handoff** — Porcupine pauses before the session opens and resumes cleanly after
- **Battery optimization helper** — one-tap shortcut to disable battery restrictions for the foreground service
- **No cloud dependency** — works fully on-device

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      MainActivity                        │
│  • Set Rosy as default assistant (opens system settings) │
│  • Toggle always-on wake word ON/OFF                     │
│  • Battery optimization shortcut                         │
│  • Test-trigger the assist session                       │
└────────────────────┬────────────────────────────────────┘
                     │ starts / stops
                     ▼
┌─────────────────────────────────────────────────────────┐
│              RosyService  (Foreground Service)           │
│  • Runs ONLY when wake word toggle is ON                 │
│  • Hosts Picovoice Porcupine for "Hey Rosy" detection    │
│  • Pauses mic on ACTION_PAUSE_FOR_INTERACTION            │
│  • Resumes mic on ACTION_RESUME_AFTER_INTERACTION        │
│  • Calls VoiceInteractionService.showSession() on detect │
└─────────────────────────────────────────────────────────┘

System triggers (long-press Home / power button / showSession())
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│         RosyVoiceInteractionService                      │
│  • Registered as Android system assistant                │
│  • Entry point for all system-level assist triggers      │
└────────────────────┬────────────────────────────────────┘
                     │ creates session via
                     ▼
┌─────────────────────────────────────────────────────────┐
│       RosyVoiceInteractionSessionService                 │
│  • Instantiates RosyVoiceInteractionSession per trigger  │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│         RosyVoiceInteractionSession                      │
│  • Inflates the overlay UI (orb + status text)           │
│  • Starts orb pulse animation                            │
│  • Initializes TtsManager on-demand                      │
│  • Speaks "Yes?" then auto-dismisses after 800 ms        │
│  • On hide → tells RosyService to resume Porcupine       │
└─────────────────────────────────────────────────────────┘
                     │ uses
                     ▼
┌─────────────────────────────────────────────────────────┐
│                    TtsManager                            │
│  • Wraps Android TextToSpeech                            │
│  • Lazy-init — created only when session is triggered    │
│  • Callback-based: onInitialized → speak → onDone        │
│  • Shuts down fully on session dismiss (no idle cost)    │
└─────────────────────────────────────────────────────────┘
```

### Key Design Decisions

| Decision | Reason |
|---|---|
| `VoiceInteractionService` instead of Accessibility Service | Proper Android API, no accessibility permission abuse |
| TTS initialized on-demand per session | Zero idle memory/battery; engine is shut down after every response |
| Porcupine paused before `showSession()` | Mic can only be held by one consumer; avoids audio conflicts |
| Wake word OFF by default | Respect user battery & privacy — opt-in only |
| `BuildConfig` for API key | Keeps secrets out of source control |

---

## 📁 Project Structure

```
rosyAssistant/
├── app/
│   └── src/main/
│       ├── java/com/personal/rosy/
│       │   ├── MainActivity.kt                          # Settings & toggle UI
│       │   ├── assistant/
│       │   │   ├── RosyVoiceInteractionService.kt       # System assistant entry point
│       │   │   ├── RosyVoiceInteractionSessionService.kt# Session factory
│       │   │   └── RosyVoiceInteractionSession.kt       # Overlay UI + TTS logic
│       │   ├── data/
│       │   │   └── AppPreferences.kt                    # SharedPreferences wrapper
│       │   ├── service/
│       │   │   └── RosyService.kt                       # Porcupine wake word foreground service
│       │   └── tts/
│       │       └── TtsManager.kt                        # Android TTS wrapper
│       ├── res/
│       │   ├── layout/
│       │   │   ├── activity_main.xml                    # Main settings screen
│       │   │   └── layout_assist_overlay.xml            # Orb overlay shown on trigger
│       │   ├── drawable/
│       │   │   ├── ic_rosy_orb.xml                      # Orb vector graphic
│       │   │   └── bg_assist_overlay.xml                # Overlay background
│       │   └── xml/
│       │       └── voice_interaction_service.xml        # Links service → session service
│       └── AndroidManifest.xml
├── local.properties          # 🔒 Your secrets (git-ignored)
├── .gitignore
├── build.gradle.kts          # Root build config
└── app/build.gradle.kts      # App-level build + BuildConfig injection
```

---

## 🚀 Setup

### Prerequisites

- Android Studio **Hedgehog** (2023.1.1) or newer
- Android SDK **API 26+** (Android 8.0 Oreo)
- A **Picovoice account** (free) if you want the always-on wake word feature
- A physical Android device (recommended) — emulators have limited mic support

---

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/rosyAssistant.git
cd rosyAssistant
```

---

### 2. Configure the Picovoice Access Key

> **Skip this step if you don't need the always-on wake word.** The app works fully without it — Porcupine is only used when the toggle is switched ON.

1. Sign up for a free account at [console.picovoice.ai](https://console.picovoice.ai/)
2. Copy your **Access Key** from the dashboard
3. Open `local.properties` in the project root and paste your key:

```properties
PICOVOICE_ACCESS_KEY=your_actual_key_here
```

> `local.properties` is git-ignored — your key will never be committed.

---

### 3. Add the Wake Word Model File

Place your custom `hey_rosy_android.ppn` Porcupine model file in:

```
app/src/main/assets/hey_rosy_android.ppn
```

You can train a custom wake word (free) at [console.picovoice.ai](https://console.picovoice.ai/) → **Wake Word** → create `hey rosy` → download the `.ppn` for Android.

---

### 4. Build & Install

Open the project in Android Studio and click **Run**, or via command line:

```bash
./gradlew installDebug
```

---

### 5. Set Rosy as Your Default Assistant

This is **required** for Rosy to respond to the Home button long-press.

1. Open the Rosy app
2. Tap **"Set as Default Assistant"**
3. In system settings, select **Rosy** under *Digital Assistant App*

Or navigate manually:
> **Settings → Apps → Default Apps → Digital Assistant App → Rosy**

---

### 6. (Optional) Enable Always-On Wake Word

1. Open the Rosy app
2. Toggle **"Always-on wake word"** ON
3. Grant the **Microphone** permission when prompted
4. Tap **"Disable Battery Optimization"** to prevent Android from killing the foreground service

---

## 🔐 Permissions

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Required for Porcupine wake word detection |
| `FOREGROUND_SERVICE` | Keep wake word service alive in background |
| `FOREGROUND_SERVICE_MICROPHONE` | Android 12+ requirement for mic in foreground service |
| `POST_NOTIFICATIONS` | Show the persistent "Listening..." notification |
| `WAKE_LOCK` | Prevent CPU from sleeping during wake word detection |

---

## 🛠️ Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| Min SDK | API 26 (Android 8.0) |
| Target SDK | API 35 (Android 15) |
| Assistant Framework | Android `VoiceInteractionService` |
| Wake Word | [Picovoice Porcupine](https://picovoice.ai/products/porcupine/) v3.0.2 |
| Text-to-Speech | Android built-in `TextToSpeech` |
| Build System | Gradle (Kotlin DSL) |
| UI | XML layouts + `ObjectAnimator` for orb pulse |

---

## 🔧 How the Wake Word Flow Works

```
Device idle
    │
    ▼
[RosyService running as Foreground Service]
    │  Porcupine listening on mic
    │
    ▼  "Hey Rosy" detected
[PorcupineManagerCallback fires]
    │
    ▼
[stopPorcupine()]  ← releases mic immediately
    │
    ▼
[VoiceInteractionService.showSession()]
    │
    ▼
[RosyVoiceInteractionSession.onShow()]
    │  Sends ACTION_PAUSE_FOR_INTERACTION to RosyService (safety)
    │  Starts orb animation
    │  Initializes TtsManager
    │
    ▼
TTS speaks "Yes?" → onDone fires → 800ms delay → dismissSession()
    │
    ▼
[onHide()] → sends ACTION_RESUME_AFTER_INTERACTION
    │
    ▼
[RosyService restarts Porcupine]  ← back to listening
```

---

## 🐛 Troubleshooting

**Rosy doesn't respond to Home long-press**
> Make sure Rosy is set as the **Default Digital Assistant** in system settings. See Step 5.

**"Porcupine init failed"** in logs
> Check that your `PICOVOICE_ACCESS_KEY` in `local.properties` is valid and that `hey_rosy_android.ppn` exists in `app/src/main/assets/`.

**Wake word service keeps getting killed**
> Disable battery optimization for Rosy — tap the **"Disable Battery Optimization"** button in the app. On ColorOS / MIUI, also whitelist the app in the system battery manager.

**TTS not speaking**
> Ensure your device has a TTS engine installed (**Settings → Accessibility → Text-to-Speech**). Google TTS is recommended.

**Mic conflict / Porcupine crashes when opening session**
> This is handled automatically via `ACTION_PAUSE_FOR_INTERACTION`. If it persists, check logcat for `RosyService` and `RosyAssistSession` tags.

---

## 📄 License

This project is for personal use. See [LICENSE](LICENSE) for details.

