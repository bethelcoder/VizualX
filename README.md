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
2. **Multi-directional perception**: Front and rear cameras combine (where hardware allows) to watch ahead and behind simultaneously.
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
│       │   │   ├── camera/          # CameraX & Concurrent Camera detection
│       │   │   ├── audio/           # Continuous 16kHz audio capture & classifier
│       │   │   ├── perception/      # Structured observation pipeline
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

## Build and Run

### Prerequisites
- JDK 17 or JDK 21
- Android SDK (API 29+)
- Physical Android device (recommended) or Android Emulator (API 30+)

### Building via Terminal
```bash
# Set JAVA_HOME (if not already set)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"

# Run unit tests
.\gradlew.bat testDebugUnitTest

# Assemble debug APK
.\gradlew.bat assembleDebug

# Install on connected device
.\gradlew.bat installDebug
```
