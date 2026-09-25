# VizualX — Product & Technical Architecture

## 1. Product Direction

**VizualX** is an on-device, multimodal AI perception assistant for visually impaired users.

The system is designed to act as an intelligent second pair of eyes and ears. Instead of requiring the user to explicitly capture an image every time they need help, the system continuously observes its surroundings and communicates information that is important to the user.

The initial prototype environment is a **university campus**.

The campus-first approach provides a controlled environment containing:

- Buildings
- Paths
- Stairs
- Ramps
- Entrances
- Lecture halls
- Libraries
- Cafeterias
- Pedestrians
- Vehicles
- Crossings
- Signs
- Campus landmarks

The initial goal is not to build a universal navigation application. The goal is to build the underlying **perception engine** that can later support navigation and eventually wearable hardware such as smart glasses.

---

## 2. Product Principle

The system should not describe everything it sees.

Its responsibility is:

```text
Perception
    ↓
Context
    ↓
Importance
    ↓
Communication
```

For example, instead of continuously reporting:

> There is a tree. There is a bench. There is a person. There is a window.

the system should communicate something actionable:

> Someone is approaching from your left.

or:

> There are stairs directly ahead.

or:

> A vehicle is approaching from your right.

The intelligence of the product is therefore not only object recognition. It is deciding **what matters now**.

---

## 3. Continuous Multimodal Perception

The target architecture combines multiple sources of environmental information.

```text
                     WORLD
                       │
          ┌────────────┴────────────┐
          │                         │
     VISUAL INPUT              AUDIO INPUT
          │                         │
     ┌────┴────┐              Microphone
     │         │                   │
 Front Camera Rear Camera          │
     │         │                   │
     └────┬────┘                   │
          │                         │
          └────────────┬────────────┘
                       ▼
              PERCEPTION ENGINE
                       │
             ┌─────────┴─────────┐
             │                   │
         Vision Models       Audio Models
             │                   │
             └─────────┬─────────┘
                       ▼
                WORLD CONTEXT
                       │
                       ▼
              CONTEXT PRIORITIZER
                       │
                       ▼
                AI REASONING
                       │
                       ▼
                VOICE / HAPTICS
```

The system should continuously perceive the environment but should **not continuously speak**.

Silence is a valid state.

---

## 4. Front + Rear Camera Concept

Android supports concurrent use of multiple cameras on devices that advertise support for the required camera combination.

The intended prototype is:

```text
                    PHONE
                      │
           ┌──────────┴──────────┐
           │                     │
      REAR CAMERA           FRONT CAMERA
       "Ahead"                "Behind"
           │                     │
           ▼                     ▼
      Vision Pipeline       Vision Pipeline
           │                     │
           └──────────┬──────────┘
                      ▼
              Scene Understanding
                      │
                      ▼
               World Context
```

This does **not** imply perfect 360-degree vision.

Two phone cameras can provide substantial front/rear coverage, but there can still be blind spots depending on:

- Device hardware
- Camera field of view
- Camera placement
- Concurrent-camera limitations
- Lens characteristics

The prototype should therefore call this **multi-directional perception**, not guaranteed 360° perception.

The system must test the exact demo device rather than assuming concurrent cameras are supported.

---

## 5. Why Native Android / Kotlin

The project has evolved beyond a conventional mobile application.

The system may need to coordinate:

- CameraX
- Concurrent camera operation
- Camera frame processing
- Microphone capture
- Environmental sound classification
- Foreground services
- On-device AI
- ML Kit
- MediaPipe
- LiteRT
- Android text-to-speech
- Accessibility/TalkBack
- Haptics
- Sensors
- Location
- Bluetooth
- Audio focus
- Battery and thermal constraints
- Background execution rules

Native Kotlin therefore becomes the primary platform.

The rationale is not simply performance.

The product is becoming an **Android perception system**, and native Android provides direct access to the platform capabilities required to experiment with that system.

---

## 6. Android Architecture

Proposed high-level structure:

```text
VizualX
│
├── CameraX
│   ├── Front Camera
│   ├── Rear Camera
│   └── Frame Processing
│
├── Audio
│   ├── Microphone Capture
│   └── Sound Classification
│
├── On-Device AI / ML
│   ├── ML Kit
│   ├── MediaPipe
│   ├── LiteRT
│   └── Gemini Nano / AICore where supported
│
├── Perception Engine
│
├── World State
│
├── Context Engine
│
├── AI Orchestrator
│   ├── Local AI
│   └── Cloud AI
│
├── Voice
│   ├── Speech Recognition
│   └── Text-to-Speech
│
├── Accessibility
│   ├── TalkBack
│   └── Haptics
│
├── Location
│
└── Foreground Perception Service
```

UI can be implemented using **Jetpack Compose**.

---

## 7. Three-Layer Intelligence Model

The system should separate perception from higher-level reasoning.

### Layer 1 — Reflexes

Fast local detection that can operate continuously.

Potential capabilities:

- Person detection
- Vehicle detection
- Obstacle detection
- Stairs detection
- Door detection
- Crosswalk detection
- Text detection
- Sound classification

Example:

```json
{
  "vehicle": 0.91,
  "person": 0.83,
  "stairs": 0.87
}
```

The exact models are implementation decisions to be tested.

The important architectural principle is that this layer should be lightweight and fast.

---

### Layer 2 — Situational Awareness

This layer maintains a short-term representation of the user's environment.

Example:

```text
WORLD STATE

Time: 10:42:31

FRONT:
  person ~2.4m
  stairs ~6m
  doorway ~9m

REAR:
  person ~3m

AUDIO:
  vehicle approaching right
  footsteps behind

LOCATION:
  campus
  library zone

MOVEMENT:
  user walking forward
```

The system should reason about changes in this state rather than treating every camera frame as an independent conversation.

---

### Layer 3 — Generative AI

Generative AI should be used selectively for higher-level reasoning and conversational tasks.

Examples:

- "What's around me?"
- "What building is that?"
- "What does that sign say?"
- "Where am I?"
- "Explain what I'm looking at."

A large model should not necessarily be invoked continuously.

---

## 8. Local-First, Cloud-Enhanced AI

The target architecture is:

```text
                   VizaualX
                      │
          ┌───────────┴───────────┐
          │                       │
       ON DEVICE                CLOUD
          │                       │
   ┌──────┼───────┐               │
   │      │       │               │
 Camera  Audio  Sensors      Complex reasoning
   │      │       │               │
   └──────┼───────┘               │
          │                       │
       Local ML              Cloud AI
          │                       │
          └───────────┬───────────┘
                      ▼
               Context Engine
                      │
                      ▼
                  Response
                      │
                 Android TTS
```

Local processing is preferred for:

- Low latency
- Reduced network dependence
- Privacy
- Reduced data usage
- Potential offline operation
- Lower recurring API cost

Cloud processing remains available for tasks where local models are insufficient.

The project should **not** assume that every target device can run every AI capability locally. Device support, latency, battery consumption, thermals and accuracy must be tested.

---

## 9. Continuous Voice Assistant

The intended interaction should feel conversational.

Example:

**User:**

> VizualX, what's around me?

**AI:**

> You're on a pedestrian path. There's a building entrance approximately ten metres ahead.

The user continues walking.

**AI:**

> There's a person approaching from your left.

**User:**

> What building is that?

**AI:**

> That's the Wits Science Stadium.

The assistant should therefore maintain conversational context while environmental perception continues in parallel.

---

## 10. Voice State Machine

Proposed state model:

```text
IDLE
 │
 │ user activates
 ▼
LISTENING
 │
 ▼
UNDERSTANDING
 │
 ▼
RESPONDING
 │
 ▼
MONITORING
 │
 ├── important event ──► RESPONDING
 │
 └── nothing important ─► MONITORING
```

A user should also be able to enter a quiet monitoring state:

```text
User:
"Be quiet."

        ↓

QUIET MONITORING

Camera + audio continue

        ↓

Critical event

        ↓

AI speaks
```

This prevents unnecessary speech while retaining safety-relevant awareness.

---

## 11. Event Priority

The system should classify events by importance.

### Critical

- Vehicle approaching
- Immediate obstacle
- Potential collision risk
- Stairs immediately ahead

### High

- Door
- Crossing
- Unexpected obstacle
- Important campus landmark

### Normal

- Sign
- Building
- Person
- General environment

### Low

- Trees
- Benches
- Decorative objects
- Background activity

The priority system determines whether an event should interrupt current speech or remain silent.

Example:

```text
User:
"Tell me about this building..."

        ↓

Vehicle approaching from right

        ↓

Immediate interruption

        ↓

"Caution. Vehicle approaching from your right."
```

---

## 12. Environmental Audio

The microphone is treated as a continuous perception source.

Potential signals include:

- Vehicle engines
- Horns
- Sirens
- Bicycles
- Approaching people
- Alarms
- Announcements
- Speech
- General environmental changes

Audio should be combined with vision where possible.

Example:

```text
CAMERA:

Vehicle detected on right
Road detected ahead

+

AUDIO:

Engine sound
Increasing volume
Right-side dominance

        ↓

MULTIMODAL REASONING

        ↓

"Caution. A vehicle is approaching
from your right."
```

The system should distinguish between:

- Raw audio capture
- Audio classification
- Speech recognition
- Environmental sound understanding

These are separate concerns.

---

## 13. Campus-First Product Scope

The first practical environment is a university campus.

Potential campus-specific understanding includes:

- Building entrances
- Building identities
- Lecture halls
- Libraries
- Cafeterias
- Paths
- Stairs
- Ramps
- Crossings
- Campus landmarks
- Signs
- Pedestrian activity
- Vehicle activity

The initial objective is not full autonomous navigation.

Instead, the system establishes a perception layer on which navigation can later be built.

---

## 14. Phone as a Prototype for Future Smart Glasses

The phone should be treated as a development platform for the future wearable architecture.

Future concept:

```text
              SMART GLASSES

       ┌──────────┬──────────┐
       │ Camera L │ Camera R │
       └────┬─────┴────┬─────┘
            │          │
            ▼          ▼
         Vision      Vision

             Microphones
                  │
                  ▼
             Audio Engine
                  │
                  ▼
          PERCEPTION ENGINE
                  │
             ┌────┴────┐
             │         │
        World Model  User Context
             │         │
             └────┬────┘
                  ▼
            AI Assistant
                  │
                  ▼
             Open-ear audio
```

The phone prototype should therefore keep perception, world-state reasoning and AI orchestration independent from the phone UI wherever practical.

---

## 15. Safety Principle

The system is assistive.

It must never present itself as infallible.

For safety-critical situations, the assistant should communicate observations and uncertainty rather than make authoritative safety decisions.

Avoid:

> "It is safe to cross."

Prefer:

> "I detect a vehicle approaching from your right."

or:

> "I cannot confidently determine whether the road is clear."

The user remains in control.

The system should not claim to replace:

- A white cane
- A guide dog
- Human assistance
- Established mobility techniques
- Professional accessibility support

---

## 16. Privacy Principle

The system may observe sensitive environments containing:

- Faces
- Students
- Lecturers
- Personal conversations
- Documents
- Notices
- Private information

Local processing should therefore be preferred wherever practical.

The architecture should avoid sending continuous raw camera and microphone streams to cloud services by default.

Cloud processing should be deliberate and capability-driven.

---

## 17. Continuous Processing Principle

The system should not send every frame to a generative AI model.

Instead:

```text
Camera
  ↓
Local perception
  ↓
Structured observations
  ↓
World State
  ↓
Is something important?
  ├── No → Continue monitoring
  └── Yes
        ↓
   Contextualize
        ↓
   AI reasoning
        ↓
      Speak
```

This reduces:

- Latency
- API cost
- Network dependency
- Battery consumption
- Privacy exposure

It also gives the product a more predictable architecture.

---

## 18. Target Prototype

The first prototype should aim to demonstrate:

1. Continuous camera perception.
2. Front/rear concurrent camera operation where the device supports it.
3. Continuous environmental audio capture.
4. Basic environmental sound understanding.
5. Local object/environment perception.
6. A short-term world state.
7. Context prioritization.
8. Important-event detection.
9. Voice interaction.
10. Spoken environmental feedback.
11. Campus-specific understanding.
12. Local-first AI with cloud fallback where required.

The prototype should be judged by whether the complete perception loop works reliably, not by the number of features shown in the UI.

---

## 19. Guiding Product Statement

> **VizualX is an AI-powered second pair of eyes and ears for South Africa, beginning with campus environments and designed around continuous, context-aware perception.**

The long-term direction is a wearable assistive system.

The phone is the prototype platform.

The perception engine is the product foundation.
