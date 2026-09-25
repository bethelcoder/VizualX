# Experiment 001: Device Capability & Concurrent Camera Detection

## Question
What hardware capabilities, camera IDs, orientations, stream resolutions, and concurrent multi-camera combinations does the physical test device genuinely support?

## Hypothesis
Modern Android devices running Android 11+ (API 30+) expose `CameraManager.getConcurrentCameraIds()`. If supported, front and rear camera streams can be opened concurrently for dual-stream multi-directional perception.

## Method
1. Launch `VizualX` on the physical test device.
2. Inspect the Hardware Diagnostics Dialog (powered by `CameraCapabilityDetector`).
3. Record device model, Android OS version, API level, camera sensor IDs, and concurrent camera combinations.
4. Record whether front and rear cameras can be opened simultaneously or sequentially.

## Measurements
- Device Model:
- Android OS / API Level:
- Total Cameras:
- Rear Camera ID:
- Front Camera ID:
- Concurrent Camera Support: [ YES / NO ]
- Supported Combinations:
- Available YUV/Stream Resolutions:

## Decision
- Status: [ PENDING TEST EXECUTION ]
- Action: [ KEEP / MODIFY / REPLACE / DROP ]
- Notes:
