# VizualX — Project Setup Guide

## 1. Purpose

This document defines the initial development environment and repository setup for the native Android implementation of VizualX.

The project is intentionally designed around experimentation. The first setup must make it easy to test:

- Concurrent front and rear cameras
- Continuous microphone capture
- On-device perception
- Environmental sound classification
- Local AI
- Cloud AI fallback
- Continuous voice interaction
- Foreground perception services
- Battery and thermal behavior

Do not build the complete product during initialization.

The objective is to establish a clean, testable Android foundation and immediately make the difficult hardware capabilities easy to experiment with.

---

# 2. Technology Direction

## Platform

- Android
- Kotlin
- Jetpack Compose

## Camera

- CameraX
- Concurrent Camera where supported
- Camera2 only where lower-level access becomes necessary

## On-device AI / ML

Potential technologies:

- ML Kit
- MediaPipe
- LiteRT
- Gemini Nano / AICore where supported
- Other compatible local models discovered during feasibility testing

No model technology is considered mandatory until tested on the target device.

## Voice

- Android Speech Recognition where appropriate
- Android Text-to-Speech
- Audio APIs appropriate for continuous environmental capture

## System capabilities

- Foreground Services
- Haptics
- Accessibility/TalkBack
- Location
- Sensors
- Audio focus

## Cloud

Cloud AI may be used selectively when local capabilities are insufficient.

API secrets must never be embedded in the Android application.

---

# 3. Development Environment

Recommended baseline:

- Android Studio
- Android SDK
- Kotlin
- Gradle
- Git
- Physical Android test device

A physical device is strongly preferred.

The most important capabilities of this project cannot be validated reliably through an emulator alone.

---

# 4. Repository Structure

Recommended initial structure:

```text
second-sight/
│
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/.../
│       │   │   ├── camera/
│       │   │   ├── audio/
│       │   │   ├── perception/
│       │   │   ├── context/
│       │   │   ├── ai/
│       │   │   ├── speech/
│       │   │   ├── accessibility/
│       │   │   ├── location/
│       │   │   ├── services/
│       │   │   └── ui/
│       │   │
│       │   ├── res/
│       │   └── AndroidManifest.xml
│       │
│       └── test/
│
├── docs/
│   ├── architecture/
│   ├── decisions/
│   ├── experiments/
│   └── accessibility/
│
├── models/
│   └── README.md
│
├── roadmap.md
├── AGENTS.md
├── README.md
├── .gitignore
└── settings.gradle.kts
```

The exact Kotlin package name should be selected during project initialization.

---

# 5. Core Modules

The initial application should conceptually separate the following concerns.

```text
Camera
  ↓
Perception
  ↓
World State
  ↓
Context Engine
  ↓
AI Orchestrator
  ↓
Speech
```

Audio should enter the same perception layer:

```text
Audio
  ↓
Audio Perception
  ↓
World State
```

The UI should not directly own this pipeline.

---

# 6. Camera Layer

Responsibilities:

- Request camera permissions
- Initialize CameraX
- Detect available camera combinations
- Attempt concurrent front/rear operation
- Produce camera frames
- Manage lifecycle
- Expose camera observations to the perception layer

The camera module should not decide what an observation means to the user.

For example:

```text
Camera:
"frame received"

Perception:
"vehicle detected"

Context:
"vehicle approaching"

Speech:
"Caution. A vehicle is approaching."
```

---

# 7. Audio Layer

Responsibilities:

- Request microphone permission
- Capture environmental audio
- Produce audio buffers
- Run or invoke sound classification
- Expose structured audio observations

The audio layer should not generate final user-facing responses.

Example:

```json
{
  "type": "vehicle",
  "confidence": 0.87,
  "timestamp": 1727260000
}
```

---

# 8. Perception Layer

The perception layer converts raw sensor data into structured observations.

Example:

```json
{
  "source": "rear_camera",
  "type": "person",
  "position": "ahead",
  "confidence": 0.91,
  "timestamp": 1727260000
}
```

Potential observations:

- Person
- Vehicle
- Door
- Stairs
- Obstacle
- Sign
- Text
- Crosswalk
- Horn
- Siren
- Footsteps
- Speech
- Alarm

The supported observation types should grow through experiments.

---

# 9. World State

World State maintains a short-lived representation of current surroundings.

It should contain:

- Recent observations
- Source
- Timestamp
- Approximate position/direction where supported
- Confidence
- Relevant contextual metadata

Example:

```text
WORLD STATE

FRONT
  stairs: 6m
  person: 2m

REAR
  person: 3m

AUDIO
  vehicle

LOCATION
  campus
```

World State should not become a permanent recording of everything the user encounters.

---

# 10. Context Engine

The Context Engine converts observations into meaningful events.

Example:

```text
Observation:
vehicle

+

Observation:
road

+

Observation:
user approaching crossing

        ↓

Context Event:
vehicle approaching crossing
```

It should also determine priority.

```text
CRITICAL
HIGH
NORMAL
LOW
IGNORE
```

---

# 11. AI Orchestrator

The AI Orchestrator decides whether an event should be handled by:

- Local deterministic logic
- Local ML
- Local generative AI
- Cloud AI
- No AI at all

Example:

```text
Simple obstacle
      ↓
Local perception + rules

Complex question
      ↓
Local AI if available
      ↓
Cloud fallback if necessary
```

This prevents every event from being sent to a large generative model.

---

# 12. Speech Layer

Responsibilities:

- Speech recognition
- Voice interaction
- Text-to-speech
- Audio focus
- Speech interruption
- Speech priority

The speech layer should support interruption for critical events.

Example:

```text
User is asking a question

        ↓

Vehicle detected

        ↓

Critical event

        ↓

Interrupt current speech

        ↓

"Caution. Vehicle approaching
from your right."
```

---

# 13. Foreground Service

Continuous perception requires an Android-supported foreground service architecture.

Expected lifecycle:

```text
User starts assistance
        ↓
Permissions
        ↓
Foreground service
        ↓
Camera + microphone
        ↓
Perception loop
```

The service should expose clear application state:

```text
ASSISTANCE INACTIVE

ASSISTANCE STARTING

ASSISTANCE ACTIVE

ASSISTANCE PAUSED

ASSISTANCE ERROR
```

---

# 14. Permissions

The initial application should request only permissions required for the current feature set.

Likely permissions include:

- Camera
- Microphone
- Location where required
- Notifications if required by the Android foreground-service implementation

Permission handling must explain why access is required.

Do not request every permission during first launch.

---

# 15. Local and Cloud Configuration

Use environment/build configuration for cloud endpoints.

Never hardcode:

- API keys
- Provider secrets
- Private credentials
- Service account keys

The Android application should communicate with a controlled backend when cloud functionality requires secrets.

---

# 16. Testing Structure

Minimum test categories:

```text
app/src/test/
├── perception/
├── context/
├── ai/
├── speech/
└── worldstate/
```

Instrumented tests can cover Android-specific behavior.

Manual device tests are mandatory for:

- Camera concurrency
- Microphone
- Foreground service
- TTS
- Accessibility
- Battery
- Thermal behavior
- Real-world perception

---

# 17. Experiment Logging

Create:

```text
docs/experiments/
```

Each experiment should have a Markdown document.

Recommended naming:

```text
001-device-capability.md
002-concurrent-camera.md
003-dual-stream-processing.md
004-continuous-audio.md
005-local-vision.md
006-local-ai.md
007-voice-loop.md
008-world-state.md
009-multimodal-reasoning.md
```

Each experiment should follow the Spec-Driven Development process defined in `second-sight-spec-driven-development.md`.

---

# 18. Git Setup

Initialize Git immediately.

Recommended branches:

```text
main
dev
feature/*
```

Keep commits focused.

Examples:

```text
feat(android): initialize native Kotlin application
feat(camera): add camera capability detection
feat(camera): prototype concurrent front and rear cameras
feat(audio): add continuous microphone capture
feat(perception): add structured observation model
feat(context): add event prioritization
feat(speech): add priority-aware text to speech
test(camera): validate concurrent camera lifecycle
docs(experiment): record concurrent camera results
```

Avoid commits such as:

```text
update
stuff
changes
final
hackathon
```

---

# 19. Initialization Definition of Done

Project initialization is complete when:

- Android project builds successfully.
- Kotlin/Gradle configuration is stable.
- Jetpack Compose is running.
- A physical Android device can install the app.
- Camera permission flow exists.
- Microphone permission flow exists.
- CameraX is initialized.
- Device camera capability detection can run.
- Concurrent-camera support can be queried.
- Basic microphone capture can be tested.
- Foreground-service foundation is understood or prototyped.
- Test structure exists.
- Documentation structure exists.
- Git repository is initialized.
- `AGENTS.md` exists.
- Spec-driven development guide exists.
- Experiment logging structure exists.

Do not implement the complete AI system as part of initialization.

---

# 20. First Technical Milestone

After initialization, the first serious milestone is:

> **Prove whether the chosen Android device can simultaneously operate the front and rear cameras while maintaining a continuous microphone pipeline.**

Only after this experiment should significant effort be spent on the full perception architecture.

---

# 21. Engineering Rule

Do not protect assumptions.

If the experiment proves that a different architecture is better, update the architecture.

The goal is not to make the initial document correct.

The goal is to discover what actually works.
