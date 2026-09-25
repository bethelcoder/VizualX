# VizualX — Master Engineering Roadmap & Hackathon Plan

> **Product Vision**: An AI-powered second pair of eyes and ears for South Africa, starting with university campus environments and designed around continuous, context-aware perception.

---

## 1. Phased Engineering Execution Plan

Following the principles established in `docs/vizualX-feasibility-and-hackathon-plan.md` and `docs/vizualX-product-architecture.md`, the implementation follows a strict **Spec-Driven Feasibility Sequence**. We test difficult hardware and ML assumptions before layering complex reasoning.

```text
┌────────────────────────────────────────────────────────┐
│ Phase 0: Architecture & Android Foundation [COMPLETED] │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│ Phase 1: Hardware & Sensor Feasibility Spikes          │
│   • Exp 001: Device capability report & camera IDs     │
│   • Exp 002: Concurrent front + rear camera stream     │
│   • Exp 003: Dual-stream frame processing latency      │
│   • Exp 004: Continuous 16kHz microphone capture       │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│ Phase 2: Local Perception & On-Device ML               │
│   • Exp 005: Environmental sound classifier (YAMNet)   │
│   • Exp 006: Local vision (obstacles, stairs, vehicles)│
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│ Phase 3: Intelligence, State & Voice Loop              │
│   • Exp 007: Local Generative AI (AICore/Nano/Cloud)   │
│   • Exp 008: Voice state machine & priority TTS        │
│   • Exp 009: Rolling World State (TTL decay memory)    │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│ Phase 4: Multimodal Synthesis & Foreground Service     │
│   • Exp 010: Context prioritizer & silence engine      │
│   • Exp 011: Multimodal fusion (Vision + Audio alert)  │
│   • Exp 012: Sustained battery & thermal profiling     │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│ Phase 5: Campus Demonstration Scenarios                │
│   • Scenario 1: Path & doorway awareness               │
│   • Scenario 2: Immediate obstacle avoidance           │
│   • Scenario 3: Descending stairs alert                │
│   • Scenario 4: Approaching vehicle audio+vision fusion│
│   • Scenario 5: Campus landmark & sign query           │
└────────────────────────────────────────────────────────┘
```

---

## 2. Detailed Phase Breakdown

### Phase 0: Android Foundation & Architecture Setup (Status: Complete)
- [x] Gradle multi-plugin build configuration (AGP 8.5.0, Kotlin 2.0.21, Jetpack Compose BOM 2024.09.00).
- [x] Modular architecture with separated packages (`camera`, `audio`, `perception`, `context`, `speech`, `ai`, `accessibility`, `services`, `ui`).
- [x] CameraX and Camera2 capability probe (`CameraCapabilityDetector`).
- [x] Continuous background audio capture engine (`ContinuousAudioCapture`).
- [x] Short-term rolling memory buffer (`WorldState`) with TTL expiration.
- [x] Context prioritization engine (`ContextEngine`) enforcing the *Silence is a Valid State* rule.
- [x] Priority-aware Text-to-Speech manager (`SpeechManager`) with urgent hazard interruption.
- [x] Tactile hazard feedback manager (`HapticFeedbackManager`).
- [x] Foreground perception service foundation (`ForegroundPerceptionService`).
- [x] High-contrast, accessible Jetpack Compose UI with live preview, diagnostics modal, VU meter, and 1-tap simulation suite.
- [x] Unit test suite (`ContextEngineTest`, `WorldStateTest`).

---

### Phase 1: Hardware & Sensor Feasibility Spikes
*Goal: Measure exact physical device capabilities and determine if dual-stream concurrent camera is viable on the test hardware.*

1. **Experiment 001 — Device Capability Audit**:
   - Run `CameraCapabilityDetector` on the physical target device.
   - Document device model, camera IDs, lens facings, and supported concurrent camera combinations in `docs/experiments/001-device-capability.md`.
2. **Experiment 002 — Concurrent Front/Rear Camera Streaming**:
   - If supported by device hardware, open front (facing behind) and rear (facing ahead) streams simultaneously.
   - If unsupported, activate single-camera fallback with quick-switch capability.
3. **Experiment 003 — Dual-Stream Frame Processing Latency**:
   - Benchmark image analysis throttling (4 FPS – 10 FPS) to balance thermal dissipation, battery draw, and detection responsiveness.
4. **Experiment 004 — Continuous Audio Pipeline**:
   - Validate continuous 16kHz PCM audio record loop alongside CameraX without audio glitching or frame dropping.

---

### Phase 2: Local Perception & On-Device ML
*Goal: Convert raw sensor data into structured `Observation` objects locally on device.*

1. **Experiment 005 — Environmental Sound Classification**:
   - Integrate lightweight sound classification model (YAMNet / LiteRT) to detect sirens, vehicle engines, horns, footsteps, and alarms.
2. **Experiment 006 — Local Vision Hazard Detection**:
   - Integrate ML Kit / LiteRT object detector for vehicles, pedestrians, stairways, doors, and crosswalks.
   - Output structured bounding boxes and relative positions (`AHEAD`, `LEFT`, `RIGHT`, `BEHIND`).

---

### Phase 3: Intelligence, State & Voice Interaction
*Goal: Maintain environmental context and conversational assistant capabilities.*

1. **Experiment 007 — Local Generative AI & Cloud Triage**:
   - Triage queries: Local reflexes handle hazards -> Local AI/WorldState handles surroundings brief -> Cloud AI handles complex campus landmark recognition.
2. **Experiment 008 — Voice State Machine & Priority TTS**:
   - Test user voice queries (*"VizualX, what's around me?"*) while perception continues in the background.
   - Validate that a `CRITICAL` hazard (e.g. approaching vehicle) immediately halts ongoing TTS speech and announces the emergency alert.
3. **Experiment 009 — World State Memory Pruning**:
   - Ensure observations decay naturally so the user is not warned about an obstacle they already passed.

---

### Phase 4: Multimodal Synthesis & Service Robustness
*Goal: Fuse vision and audio signals and ensure the app survives sustained background operation.*

1. **Experiment 010 — Context Prioritization Rule Engine**:
   - Verify that background objects (trees, benches) remain silent.
2. **Experiment 011 — Multimodal Fusion**:
   - Vision detection of vehicle + Audio engine sound from right -> High-confidence unified alert: *"Caution. Vehicle approaching from your right."*
3. **Experiment 012 — 30-Minute Thermal & Battery Soak Test**:
   - Measure battery consumption and temperature over 30 minutes of continuous perception.

---

### Phase 5: Campus Demonstration
*Goal: Demonstrate the live system across the 5 core campus scenarios.*
- **Scenario 1**: Path navigation & entrance door detection.
- **Scenario 2**: Unexpected path obstacle detection.
- **Scenario 3**: Descending stairs hazard announcement.
- **Scenario 4**: Vehicle approaching at a crossing (visual + audio alert).
- **Scenario 5**: Landmark query (*"What building is that?"* -> *"Wits Science Stadium"*).

---

## 3. When & How to Transition to Android Studio

### Why and When to Use Android Studio:
While Antigravity IDE is ideal for architecture planning, writing Kotlin code, designing Compose components, and running automated Gradle unit tests, **Android Studio** is strongly advantageous for:
1. **Physical Device Deployment & Debugging**: One-click install and live step-by-step debugging on connected USB/Wi-Fi Android devices.
2. **Logcat & Real-time Sensor Tracing**: Filtering camera, audio, and ML logs in real time.
3. **Android Studio Profiler**: Inspecting real-time CPU, GPU, Memory, Energy, and Thermal usage during continuous perception.
4. **Layout Inspector & Compose Preview**: Live visual inspection of UI rendering on physical screen sizes.

### Recommended Transition Milestone:
**Transition to Android Studio NOW (at the end of Phase 0)** to install the baseline build onto your physical phone or emulator and inspect the Hardware Diagnostics Dialog.

### How to Open the Project in Android Studio:
1. Open **Android Studio**.
2. Select **File -> Open...** (or click **Open** from the welcome screen).
3. Navigate to `c:\Users\nduvh\Hackathons\VizualX` and click **OK**.
4. Allow Android Studio to complete the initial Gradle Sync.
5. Connect your Android phone via USB (with Developer Options & USB Debugging enabled) or select the Android Virtual Device.
6. Click the green **Run 'app'** button (or press `Shift + F10`) to deploy VizualX!
