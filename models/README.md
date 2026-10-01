# VizualX — On-Device ML Models

This directory holds local machine learning models (TFLite, MediaPipe tasks, LiteRT models) discovered and evaluated during feasibility spikes.

## On-Device Vision & LLM Model Assets

### 1. Object Detection (MediaPipe Tasks Vision)
- **Model**: `efficientdet_lite0.tflite`
- **Location**: `app/src/main/assets/efficientdet_lite0.tflite`
- **Purpose**: Low-latency 2D spatial bounding box detection for collision avoidance.

### 2. Auditory Brain LLM (MediaPipe LLM Inference API)
- **Supported Models**: Gemma 2B Instruction-Tuned (`gemma-2b-it-gpu.bin`) or Phi-3-Mini (`phi-3-mini-4k-instruct-gpu.bin`).
- **Deployment Location Options**:
  1. **Option A (Recommended for fast deployment via ADB)**:
     ```bash
     adb push gemma-2b-it-gpu.bin /data/local/tmp/gemma-2b-it-gpu.bin
     ```
  2. **Option B (Bundled in Android Assets)**:
     Drop the `.bin` file in:
     `app/src/main/assets/gemma-2b-it-gpu.bin`
     *Note: The app will automatically extract the binary from assets to internal storage on first boot.*

### 3. Audio Classification & Scene Understanding
- **Sound Classification**: YAMNet / Environmental audio classifier for sirens, horns, alarms.
- **Scene / OCR**: ML Kit Text Recognition for campus signs and building plaques.
