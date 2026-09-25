# VizualX — Agent Guidelines & Working Rules

Welcome, Agent. You are contributing to **VizualX**, an on-device, continuous multimodal AI perception assistant for visually impaired users.

## 1. Core Engineering Principles

1. **Spec-Driven Development First**:
   Every non-trivial capability or hardware experiment must follow:
   ```text
   SPEC → IMPLEMENT → VERIFY → DOCUMENT → DECIDE
   ```
2. **Silence is a Valid State**:
   The system must not continuously describe every object (e.g. background trees, benches). Only actionable, safety-critical or user-queried context should produce spoken output.
3. **Never Invent Certainty**:
   Never convert sensor ambiguity into false confidence. If an observation is unclear, communicate uncertainty rather than making safety-critical decisions.
4. **Local-First, Cloud-Enhanced**:
   Keep fast reflexes and perception on-device. Invoke cloud generative reasoning only when local capabilities are insufficient.
5. **Keep Intelligence Decoupled from UI**:
   The UI is merely a monitor and debug surface for development. The perception engine and world state must remain standalone so they can eventually run on smart glasses.

## 2. Working Checklist for Every Task

- [ ] Check `ROADMAP.md` and the relevant experiment in `docs/experiments/`.
- [ ] Implement the smallest useful change that answers the technical question.
- [ ] Run unit tests (`./gradlew testDebugUnitTest`) to ensure no regression.
- [ ] Verify physical device behavior when touching camera, audio, or sensors.
- [ ] Document results and mark decision (`KEEP`, `MODIFY`, `REPLACE`, `DROP`).

## 3. Commit Discipline

Use conventional commits:
```text
feat(camera): add dual-stream preview support
feat(audio): add continuous 16kHz microphone capture
feat(context): implement priority-driven hazard interruption
test(context): verify vehicle alert ranking
docs(experiment): record device capability results
```
