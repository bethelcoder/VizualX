# VizualX — Feasibility Spike & Hackathon Plan

## 1. Purpose

This document defines the experimental plan for determining what the target Android hardware can actually support.

The project should not assume that a capability works simply because Android exposes an API for it.

The principle is:

> **Test the hardware. Measure the system. Then decide the architecture.**

The project is intentionally exploratory. If a proposed capability works, we keep it. If it does not, we adapt the implementation rather than protecting the original design.

---

# 2. Primary Technical Questions

Before building the complete product, answer these questions:

1. Can the target Android device run the front and rear cameras concurrently?
2. What resolutions and frame rates are available when both cameras are active?
3. Can both streams be processed continuously?
4. Can local computer vision process the streams fast enough?
5. Can microphone capture run continuously alongside camera processing?
6. Can environmental sounds be classified locally?
7. Can the device run Gemini Nano/AICore capabilities?
8. Which local vision capabilities are practical on the device?
9. Can local AI maintain acceptable latency?
10. What happens to battery and device temperature during sustained perception?
11. Can text-to-speech operate while perception continues?
12. Can voice interaction remain responsive while camera and audio pipelines are active?
13. Can the system maintain a foreground perception service correctly?
14. What happens when the network disappears?
15. What happens when the device becomes thermally constrained?
16. How much of the system can realistically remain local?
17. Which tasks should fall back to cloud AI?
18. Can the complete perception loop run reliably enough for the campus demonstration?

---

# 3. Feasibility Spike Order

Run the experiments in this order.

```text
1. Device capability
        ↓
2. Concurrent cameras
        ↓
3. Frame processing
        ↓
4. Microphone
        ↓
5. Local perception
        ↓
6. Audio classification
        ↓
7. Local generative AI
        ↓
8. Voice interaction
        ↓
9. World state
        ↓
10. Context prioritization
        ↓
11. Full multimodal loop
        ↓
12. Campus demonstration
```

Do not build the entire product before proving the difficult hardware assumptions.

---

# 4. Experiment 1 — Device Capability

Record:

- Device model
- Android version
- Camera IDs
- Camera orientations
- Supported concurrent camera combinations
- Available resolutions
- Available frame rates
- RAM
- CPU/GPU characteristics where available
- NPU/AI accelerator availability where available
- AICore/Gemini Nano availability
- Available sensors
- Battery state

Create a machine-readable capability report if practical.

Example:

```text
DEVICE CAPABILITY REPORT

Device:
Android:
Rear camera:
Front camera:

Concurrent camera support:
YES / NO

Rear concurrent resolution:
Front concurrent resolution:

Rear FPS:
Front FPS:

AICore:
AVAILABLE / UNAVAILABLE

Gemini Nano:
AVAILABLE / UNAVAILABLE

Environmental sound ML:
AVAILABLE / UNAVAILABLE
```

---

# 5. Experiment 2 — Concurrent Cameras

Implement the smallest possible native Android test using Kotlin and CameraX.

Goal:

```text
REAR CAMERA ────────┐
                    ├──► Display / Frame Counter
FRONT CAMERA ───────┘
```

Do not add AI yet.

Measure:

- Whether both cameras open simultaneously
- Startup time
- Frame rate
- Resolution
- Stability
- Memory consumption
- CPU usage if measurable
- Device temperature
- Battery impact

Success criteria:

- Both cameras remain active for a sustained test.
- Frames are received from both cameras.
- The application does not crash.
- The device remains responsive.

If concurrent cameras are unsupported, document the limitation and continue with the best available single-camera architecture.

---

# 6. Experiment 3 — Dual-Stream Frame Processing

Once concurrent cameras work, process frames from both streams.

Example:

```text
REAR FRAME
    ↓
Frame Processor
    ↓
Observation

FRONT FRAME
    ↓
Frame Processor
    ↓
Observation
```

Do not send frames to a generative model yet.

First determine whether the phone can handle continuous local processing.

Measure:

- Frames processed per second
- Dropped frames
- Processing latency
- CPU/GPU load
- Memory
- Temperature
- Battery drain

The system does not need to process every captured frame.

A lower processing frequency may be preferable if it gives significantly better thermal and battery behavior.

---

# 7. Experiment 4 — Continuous Microphone

Create a separate microphone experiment.

Test:

- Continuous microphone capture
- Audio buffer processing
- Interaction with camera processing
- Audio latency
- CPU impact
- Battery impact
- Foreground service requirements
- TTS interaction

The microphone must not block the camera pipeline.

The camera and audio systems should remain independent producers of environmental observations.

---

# 8. Experiment 5 — Environmental Sound Understanding

Test local sound classification.

Initial target categories:

```text
Vehicle
Horn
Siren
Footsteps
Speech
Alarm
Announcement
Unknown
```

The system does not need perfect sound recognition during the first experiment.

The goal is to determine:

- What can be detected reliably?
- How quickly can it be detected?
- Can confidence scores be obtained?
- Can the classifier run continuously?
- What false positives occur?

Example observation:

```json
{
  "type": "vehicle",
  "direction": "unknown",
  "confidence": 0.87,
  "timestamp": 10.42
}
```

Direction estimation should only be claimed if the hardware and audio pipeline genuinely support it.

---

# 9. Experiment 6 — Local Vision

Test lightweight local perception before testing generative AI.

Potential detections:

- Person
- Vehicle
- Door
- Stairs
- Obstacle
- Sign
- Text
- Crosswalk

The output should be structured rather than natural language.

Example:

```json
{
  "camera": "rear",
  "observations": [
    {
      "type": "person",
      "position": "ahead",
      "confidence": 0.91
    },
    {
      "type": "stairs",
      "position": "ahead",
      "confidence": 0.84
    }
  ]
}
```

The exact model technology should be selected based on measured performance.

Candidate technologies include:

- ML Kit
- MediaPipe
- LiteRT
- Device-supported AI APIs
- Other compatible on-device models

---

# 10. Experiment 7 — Local Generative AI

Determine whether the target device can practically use on-device generative AI capabilities.

Investigate:

- AICore availability
- Gemini Nano availability
- Supported APIs
- Input types
- Latency
- Context limits
- Memory behavior
- Offline behavior
- Battery impact

Test simple tasks first:

```text
Input:
"There is a doorway and stairs ahead."

Output:
"Stairs are ahead, near a doorway."
```

Then test multimodal tasks if the device supports them.

Do not assume local generative AI should process every frame.

---

# 11. Experiment 8 — Cloud AI Fallback

Implement a minimal cloud AI path for capabilities that local processing cannot handle.

The cloud path should be invoked selectively.

Potential triggers:

```text
User asks a complex question
        ↓
Local capability insufficient
        ↓
Cloud AI request
```

or:

```text
Local perception detects:
unknown landmark

        ↓

Cloud reasoning

        ↓

"That appears to be the Wits Science Stadium."
```

The mobile application must never contain cloud provider secrets.

---

# 12. Experiment 9 — Voice Interaction

The prototype should support:

```text
User speaks
     ↓
Speech recognition
     ↓
Intent / question
     ↓
AI
     ↓
Text response
     ↓
Android TTS
```

The interaction should work while environmental perception continues.

Example:

```text
User:
"What building is that?"

        ↓

Perception state

        ↓

AI

        ↓

"That is the library."

        ↓

TTS
```

---

# 13. Experiment 10 — World State

Introduce a short-lived environmental state.

Example:

```text
WORLD STATE

FRONT:
  stairs: 6m
  person: 2m

REAR:
  person: 3m

AUDIO:
  vehicle
  footsteps

LOCATION:
  campus
```

The world state should update continuously.

It should not become a permanent database of everything the camera has seen.

Use short-lived observations with timestamps.

---

# 14. Experiment 11 — Context Prioritization

The context engine should determine what matters.

Example input:

```text
Front:
person
stairs
tree
bench

Rear:
person

Audio:
vehicle approaching
```

Potential result:

```text
HIGH PRIORITY:
Vehicle approaching.

HIGH PRIORITY:
Stairs ahead.

NORMAL:
Person approaching.

IGNORE:
Tree.
Bench.
```

Only high-value events should normally produce unsolicited speech.

---

# 15. Experiment 12 — Multimodal Reasoning

Combine camera and audio observations.

Example:

```text
VISION:
vehicle detected on right

AUDIO:
engine sound

CONTEXT:
user approaching crossing

        ↓

EVENT:

Vehicle approaching from right
```

Then:

```text
AI OUTPUT:

"Caution. A vehicle is approaching
from your right."
```

The system should avoid inventing details that are not supported by the sensors.

---

# 16. Foreground Perception Service

Continuous camera and microphone operation should be implemented with Android's supported foreground-service model.

The intended lifecycle is:

```text
User opens VizualX
        ↓
User starts assistance
        ↓
Permissions granted
        ↓
ASSISTANCE ACTIVE
        ↓
Foreground perception service
        ↓
Camera + microphone + AI
```

The system should clearly communicate that environmental perception is active.

Do not attempt to hide continuous camera or microphone usage.

---

# 17. Failure and Fallback Strategy

Every major perception capability should have a fallback.

### Both cameras unsupported

Use the rear camera as the primary perception source.

### Local AI unsupported

Use cloud AI selectively.

### Network unavailable

Continue local perception where available.

### Local model too slow

Reduce processing frequency or simplify the model.

### Device overheats

Reduce perception frequency or disable non-critical workloads.

### Battery becomes low

Prioritize critical perception and reduce expensive processing.

### Vision confidence is low

Communicate uncertainty instead of inventing an answer.

### Audio confidence is low

Do not announce an uncertain environmental sound as fact.

---

# 18. Performance Principles

Continuous perception creates a resource-management problem.

The system should optimize:

```text
Accuracy
Latency
Battery
Thermals
Privacy
Network usage
```

There is no single perfect setting.

For example:

```text
30 FPS camera
+
continuous AI inference
```

may be unnecessary.

A better architecture may be:

```text
30 FPS capture
       ↓
10 FPS perception
       ↓
Event-based reasoning
       ↓
Speech only when needed
```

The actual numbers must be determined experimentally.

---

# 19. Battery and Thermal Testing

Run sustained tests.

At minimum measure:

- Starting battery percentage
- Duration
- Ending battery percentage
- Device temperature if available
- Camera processing rate
- AI processing rate
- Network usage
- Number of spoken events
- Crashes or service restarts

Test:

```text
10 minutes
30 minutes
60 minutes
```

if time permits.

Continuous perception is only useful if the phone remains usable.

---

# 20. Privacy Testing

Determine exactly which information leaves the device.

Document:

```text
LOCAL:
Camera frame processing
Object detection
Audio classification
World state
Critical event detection

CLOUD:
Only selected tasks requiring cloud reasoning
```

This is a target architecture, not an assumption.

Every cloud request should be traceable.

---

# 21. Campus Demonstration

The demo should take place in a campus environment.

Suggested sequence:

### Scenario 1 — Environment

User activates assistance.

System:

> "You're on a pedestrian path. There's a building entrance ahead."

### Scenario 2 — Obstacle

User approaches an obstacle.

System:

> "There's an obstacle directly ahead."

### Scenario 3 — Stairs

System detects stairs.

> "Stairs going down ahead."

### Scenario 4 — Vehicle

Vision and audio detect a vehicle.

> "Caution. A vehicle is approaching from your right."

### Scenario 5 — Landmark

User:

> "What building is that?"

System:

> "That's the Wits Science Stadium."

### Scenario 6 — Sign

User asks:

> "What does that sign say?"

System reads and speaks the text.

---

# 22. Demonstration Philosophy

The demo should not attempt to prove that the system can safely navigate every environment.

It should demonstrate:

```text
Continuous perception
        +
Multimodal understanding
        +
Context prioritization
        +
Natural voice interaction
        +
Local-first processing
```

The strongest demonstration is a small number of reliable interactions rather than a large collection of unstable features.

---

# 23. Smart-Glasses Compatibility

The architecture should keep the following components independent:

```text
Camera Input
Audio Input
Perception
World State
Context Engine
AI Orchestrator
Voice Output
```

The UI should not own the intelligence.

Future hardware can therefore replace:

```text
Phone cameras
```

with:

```text
Glasses cameras
```

and:

```text
Phone speakers
```

with:

```text
Open-ear audio
```

without rewriting the entire perception engine.

---

# 24. Experimental Decision Rules

After each experiment, record:

```text
QUESTION
What were we testing?

METHOD
How was it tested?

RESULT
What happened?

MEASUREMENTS
Latency:
FPS:
Battery:
Temperature:
Accuracy:
Crashes:

DECISION
KEEP
MODIFY
REPLACE
DROP

REASON
Why?
```

Do not make architectural decisions based purely on assumptions.

---

# 25. Definition of Technical Success

The prototype is technically successful if the team can demonstrate a stable loop similar to:

```text
CAMERA + MICROPHONE
        ↓
LOCAL PERCEPTION
        ↓
WORLD STATE
        ↓
CONTEXT PRIORITIZATION
        ↓
IMPORTANT EVENT
        ↓
AI REASONING
        ↓
VOICE RESPONSE
```

while the system continues monitoring the environment.

The exact percentage of processing that is local versus cloud is not fixed beforehand.

The measured device capabilities determine the final split.

---

# 26. Definition of Product Success for the Hackathon

The prototype should make the following idea believable:

> **A phone can act as an intelligent second pair of eyes and ears for a visually impaired person, continuously perceiving a campus environment and communicating only information that matters.**

The prototype does not need to solve every accessibility problem.

It needs to convincingly demonstrate the foundation of a future assistive perception platform.

---

# 27. Final Direction

The project should now be treated as:

**VizualX**

> **An AI-powered second pair of eyes and ears for South Africa, beginning with campus environments and designed around continuous, context-aware perception.**

The phone is the current prototype hardware.

The perception engine is the long-term product foundation.

The architecture should remain experimental enough to discover what current Android hardware can actually achieve.
