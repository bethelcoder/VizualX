# VizualX

> **An AI-powered second pair of eyes and ears for South Africa, beginning with campus environments and designed around continuous, context-aware perception.**

VizualX is an on-device, multimodal assistive perception system for visually impaired users. Instead of requiring users to snap photos repeatedly, VizualX continuously observes surroundings through camera and microphone streams, maintaining a rolling short-term **World State**, filtering noise, and speaking **only when something important happens**.

---

## System Architecture

```text
                     WORLD
                       │
          ┌────────────┴────────────┐
          │                         │
     VISUAL INPUT              AUDIO INPUT
          │                         │
     ┌────┴────┐               Microphone
     │         │                    │
 Rear Camera Front Camera           │
  (Ahead)     (Behind)              │
     │         │                    │
     └────┬────┘                    │
          │                         │
          └────────────┬────────────┘
                       ▼
               PERCEPTION ENGINE
                       │
              ┌────────┴────────┐
              │                 │
          Vision ML          Audio ML
              │                 │
              └────────┬────────┘
                       ▼
                  WORLD STATE (TTL Memory)
                       │
                       ▼
               CONTEXT PRIORITIZER
          (Critical / High / Normal / Low)
                       │
                       ▼
                  AI REASONING
           (Local Reflexes + Cloud AI)
                       │
                       ▼
                 VOICE / HAPTICS
```

---

## Core Principles

1. **Importance over exhaustive description**: The system avoids cognitive overload. Silence is a valid state; decorative objects remain quiet while safety hazards are announced immediately.
2. **Zero-touch, hands-free startup**: App automatically greets the user based on time-of-day (*"Good morning / afternoon / evening"*), counts down 3-2-1, and starts perception immediately.
3. **Local-first execution**: Critical reflexes, hazard detection, and audio monitoring operate locally on the device with zero network latency.
4. **Safety conservatism**: The system communicates observations and uncertainty, never presenting itself as infallible.

---

## Repository Structure

```text
VizualX/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/vizualx/app/
│       │   │   ├── camera/          # CameraX & Ahead stream management
│       │   │   ├── audio/           # Continuous 16kHz audio capture & classifier
│       │   │   ├── perception/      # Real-time ML Kit Object & Hazard Detection
│       │   │   ├── context/         # Rolling WorldState & Context prioritization
│       │   │   ├── ai/              # 3-Layer intelligence orchestrator
│       │   │   ├── speech/          # TextToSpeech (priority interruption) & SpeechRecognizer
│       │   │   ├── accessibility/   # Tactile haptic vibration engine
│       │   │   ├── services/        # Foreground Perception Service
│       │   │   └── ui/              # Jetpack Compose dark-mode UI & Diagnostics
│       │   └── AndroidManifest.xml
│       └── test/                    # Unit tests for ContextEngine, WorldState, etc.
│
├── docs/                            # Specifications, architectural plans & experiment spikes
├── models/                          # On-device ML models (LiteRT, MediaPipe, YAMNet)
├── ROADMAP.md                       # Phased technical roadmap & Android Studio guide
└── AGENTS.md                        # Spec-driven development agent guidelines
```

---

## Setup & Cloning Guide for Collaborators

### 1. Prerequisites (What to Install)
- **Android Studio**: Android Studio (Koala, Ladybug, or Hedgehog).
- **JDK**: JDK 17 or JDK 21 (bundled automatically inside Android Studio as `jbr`).
- **Android SDK Components** (installed via Android Studio *SDK Manager*):
  - Android SDK Platform 34 or 35
  - Android SDK Build-Tools (34.0.0 or 35.0.0)
  - Android SDK Platform-Tools (ADB)
- **Test Device / Emulator**:
  - **Physical Device (Recommended)**: Android phone running Android 10+ (API 29+), with **Developer Options** and **USB Debugging** enabled.
  - **Emulator**: Android Virtual Device (AVD) running **Android 14 (API 34) or API 33/35 standard image**.

### 2. Opening in Android Studio
1. Clone the repository to a local directory (e.g., `C:\Projects\VizualX` or `~/Projects/VizualX`).
   > **Note**: Avoid placing the project inside cloud-sync folders (like OneDrive, Google Drive, or Dropbox) as cloud file locks can interrupt Gradle APK packaging.
2. Open Android Studio -> **File -> Open** -> select the `VizualX` folder.
3. Allow Android Studio to complete Gradle Sync (Gradle 8.7 & AGP 8.5.0).
4. Select your device / emulator and click **Run 'app'** (`Shift + F10`).

---

## Common Setup Issues & Fixes

### Error: `Can't find service: package`
- **Cause**: The emulator or physical device's Package Manager service was not fully booted or ADB was hung when attempting installation.
- **Fix for Emulator**: Open **Device Manager** in Android Studio -> click the 3 dots on your virtual device -> select **Cold Boot Now** (or **Wipe Data**), wait until the Android home screen appears, then click Run.
- **Fix for Physical Device**: Unplug and reconnect USB cable, or run `adb kill-server && adb start-server`. On Xiaomi/Redmi/MIUI/Oppo devices, ensure **"Install via USB"** is turned ON in Developer Options.

### Warning: `Android 16 KB Alignment`
- **Cause**: Deploying to a 16 KB page-size test emulator image on Android 15.
- **Fix**: Use a standard **API 34 (Android 14)** or **API 35 standard (4 KB)** system image in Android Studio Device Manager.

---

## Build and Run via Terminal

```bash
# Set JAVA_HOME (if running outside Android Studio)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"

# Run unit tests
.\gradlew.bat testDebugUnitTest

# Assemble debug APK
.\gradlew.bat assembleDebug

# Install on connected device
.\gradlew.bat installDebug
```
