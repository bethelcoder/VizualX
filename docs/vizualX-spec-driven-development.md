# VizualX — Spec-Driven Development Guide

## 1. Purpose

VizualX is an experimental accessibility system with hardware, AI, real-time processing and safety implications.

Because of that complexity, development should not be driven by:

> "Let's just code the feature and see what happens."

Every meaningful capability should move through:

```text
SPEC
  ↓
IMPLEMENT
  ↓
VERIFY
  ↓
DOCUMENT
  ↓
DECIDE
```

This process is intentionally lightweight.

The goal is not bureaucracy.

The goal is to prevent the team from building large amounts of code around untested assumptions.

---

# 2. Development Philosophy

Use these principles:

1. Test difficult assumptions early.
2. Build the smallest useful implementation.
3. Prefer measurable experiments over speculation.
4. Keep perception separate from reasoning.
5. Keep platform code separate from product intelligence.
6. Do not over-engineer unproven capabilities.
7. Document important technical decisions.
8. Never hide uncertainty in AI output.
9. Treat accessibility as a core requirement.
10. Treat safety-related perception conservatively.

---

# 3. The Development Loop

Every meaningful task follows:

```text
┌──────────────┐
│ 1. SPEC      │
└──────┬───────┘
       ↓
┌──────────────┐
│ 2. IMPLEMENT │
└──────┬───────┘
       ↓
┌──────────────┐
│ 3. VERIFY    │
└──────┬───────┘
       ↓
┌──────────────┐
│ 4. DOCUMENT  │
└──────┬───────┘
       ↓
┌──────────────┐
│ 5. DECIDE    │
└──────────────┘
```

---

# 4. SPEC

Before implementation, define:

### Problem

What are we trying to solve?

### Goal

What should exist after implementation?

### Constraints

What must remain true?

### Acceptance Criteria

How will we know it works?

### Risks

What assumptions could make the implementation fail?

### Measurements

What should be measured?

For hardware/AI tasks, measurements may include:

- Latency
- FPS
- Accuracy
- Confidence
- Battery consumption
- Temperature
- Memory
- CPU/GPU usage
- Network usage
- Crash rate

---

# 5. Specification Template

Use this template for feature specifications.

```markdown
# Feature: <Name>

## Problem

Describe the problem.

## Goal

Describe the desired outcome.

## Scope

### In Scope

- ...

### Out of Scope

- ...

## Technical Approach

Describe the intended implementation.

## Dependencies

- ...

## Risks

- ...

## Acceptance Criteria

### AC-01

Given ...
When ...
Then ...

### AC-02

Given ...
When ...
Then ...

## Measurements

- ...

## Verification

- Unit test
- Instrumented test
- Physical-device test
- Manual UAT

## Definition of Done

- [ ] Implementation complete
- [ ] Tests complete
- [ ] Device verification complete
- [ ] Documentation updated
```

---

# 6. Given / When / Then

Acceptance criteria should preferably use:

```text
Given
When
Then
```

Example:

```markdown
### AC-01 — Concurrent Cameras

Given a device that supports concurrent front and rear cameras

When the user starts assistance

Then the rear and front camera streams become active
and frames are received from both streams.
```

Another:

```markdown
### AC-02 — Unsupported Device

Given a device that does not support the required camera combination

When the user starts assistance

Then the application does not crash
and informs the system that multi-camera perception is unavailable.
```

---

# 7. Experiments Are Also Specifications

A feasibility spike is not an informal test.

It should have a specification.

Example:

```markdown
# Experiment: Concurrent Cameras

## Question

Can the target device operate front and rear cameras concurrently?

## Hypothesis

CameraX concurrent camera support may allow both streams
to operate simultaneously on the target device.

## Method

1. Query available concurrent camera combinations.
2. Initialize the supported combination.
3. Capture frames from both streams.
4. Run for 10 minutes.
5. Record FPS, crashes, latency, temperature and battery.

## Success Criteria

Both streams remain operational for the duration
without application failure.

## Decision

KEEP / MODIFY / REPLACE / DROP
```

---

# 8. IMPLEMENT

Only implement what the specification requires.

Do not add unrelated:

- UI systems
- Authentication
- Databases
- Analytics
- Abstractions
- Third-party libraries
- Refactors

unless the current requirement actually needs them.

---

# 9. Smallest Useful Implementation

For a new capability:

```text
Capability
   ↓
Minimal implementation
   ↓
Real device
   ↓
Measure
   ↓
Expand
```

Do not immediately build a complete abstraction framework.

For example, for concurrent cameras:

### First

```text
Open camera A
Open camera B
Display frames
```

### Then

```text
Process frames
```

### Then

```text
Generate observations
```

### Then

```text
Feed World State
```

### Then

```text
Contextualize
```

### Then

```text
Speak
```

---

# 10. VERIFY

Verification should happen at multiple levels.

## Level 1 — Compilation

Does the project build?

## Level 2 — Unit Tests

Does isolated logic work?

## Level 3 — Integration Tests

Do components work together?

## Level 4 — Physical Device

Does the Android hardware actually support the behavior?

## Level 5 — Real Environment

Does the system behave correctly on campus?

For VizualX, Level 4 and Level 5 are essential.

---

# 11. Verification Matrix

| Capability | Unit | Integration | Physical Device | Real Environment |
|---|---:|---:|---:|---:|
| Camera capability detection | Yes | Yes | Yes | Optional |
| Concurrent cameras | Limited | Yes | Required | Required |
| Microphone capture | Limited | Yes | Required | Required |
| Sound classification | Yes | Yes | Required | Required |
| Object detection | Yes | Yes | Required | Required |
| World State | Yes | Yes | Recommended | Required |
| Context prioritization | Yes | Yes | Recommended | Required |
| TTS | Limited | Yes | Required | Required |
| Voice interaction | Limited | Yes | Required | Required |
| Foreground service | Limited | Yes | Required | Required |
| Battery behavior | No | No | Required | Required |
| Thermal behavior | No | No | Required | Required |

---

# 12. DOCUMENT

Every significant implementation or experiment should leave behind enough information for another team member to understand:

- What was built
- Why it was built
- How it works
- How it was tested
- What happened
- What remains uncertain

Documentation should be updated immediately after verification.

Do not postpone all documentation until the end of the hackathon.

---

# 13. DECIDE

After verification, make an explicit decision.

Use:

```text
KEEP
```

The approach works and should remain.

```text
MODIFY
```

The concept works but needs changes.

```text
REPLACE
```

The goal is valid but the implementation should change.

```text
DROP
```

The capability is not currently practical or useful enough.

Example:

```markdown
## Experiment Decision

Result: MODIFY

Reason:

Concurrent cameras work, but sustained dual-stream processing
causes unacceptable thermal load.

Decision:

Keep concurrent cameras but reduce local processing frequency.
```

---

# 14. AI-Specific Development Rules

AI features must not be treated as magical black boxes.

Every AI feature should define:

- Input
- Expected output
- Confidence/uncertainty
- Failure behavior
- Latency expectations
- Privacy implications
- Whether local or cloud
- Fallback behavior

Example:

```text
Input:
Camera observation + audio observation

Output:
Potential vehicle approaching

Confidence:
0.87

Failure:
Do not announce as certain if confidence is low.

Fallback:
Continue monitoring.
```

---

# 15. Never Invent Observations

The system must not turn uncertainty into certainty.

Bad:

```text
AI:
"It is safe to cross."
```

when the system has incomplete perception.

Better:

```text
"I detect a vehicle approaching from your right."
```

or:

```text
"I cannot confidently determine whether the road is clear."
```

This rule applies throughout the perception and language pipeline.

---

# 16. Continuous Systems Require State

Do not model continuous perception as independent requests.

Avoid:

```text
Frame 1 → AI
Frame 2 → AI
Frame 3 → AI
Frame 4 → AI
```

Prefer:

```text
Frames
  ↓
Observations
  ↓
World State
  ↓
Context Events
  ↓
AI reasoning when required
```

The World State is the bridge between low-level perception and higher-level reasoning.

---

# 17. Event-Based Processing

The system should remain silent when nothing important has changed.

Example:

```text
Frame
 ↓
Person detected

No meaningful change
 ↓
No speech
```

Then:

```text
Person moves into user's path
 ↓
Context change
 ↓
High-priority event
 ↓
Speech
```

This principle reduces:

- Speech overload
- AI requests
- Battery consumption
- Network usage
- User distraction

---

# 18. Priority-Driven Communication

Use:

```text
CRITICAL
HIGH
NORMAL
LOW
IGNORE
```

Critical events may interrupt speech.

Normal events should generally wait until the user asks or there is an appropriate communication opportunity.

Low-value observations should usually remain silent.

---

# 19. Local-First Development

When implementing a capability, ask:

```text
Can this be performed locally?
```

If yes, test local execution first.

Benefits:

- Lower latency
- Better privacy
- Offline capability
- Lower network use
- Potentially lower recurring cost

Then ask:

```text
Is local execution good enough?
```

If not:

```text
Can cloud AI improve it?
```

The final architecture can therefore be:

```text
LOCAL
  ↓
Good enough?
 ├── YES → use local
 └── NO
       ↓
     CLOUD
```

---

# 20. No Premature Abstractions

The project should have clear boundaries, but abstraction should follow evidence.

For example, do not build five AI-provider interfaces before testing one provider.

Instead:

```text
Prototype provider
       ↓
Understand requirements
       ↓
Identify stable interface
       ↓
Extract abstraction
```

This is especially important during a 24–48 hour hackathon.

---

# 21. Git Discipline

A commit should represent a coherent change.

Good:

```text
feat(camera): detect concurrent camera support
feat(camera): add dual-stream preview
feat(audio): add microphone capture
feat(perception): add vehicle observation model
feat(context): prioritize immediate hazards
feat(speech): interrupt speech for critical events
test(context): cover event priority ordering
docs(experiment): record camera performance results
```

Avoid:

```text
fix
changes
final
final2
working
hackathon done
```

---

# 22. Agent Instructions

Any coding agent working on VizualX should follow these rules.

Before coding:

1. Read `README.md`.
2. Read `AGENTS.md`.
3. Read the relevant architecture document.
4. Read the relevant experiment/specification.
5. Inspect the current implementation.
6. Identify existing patterns before introducing new ones.

Then:

```text
SPEC
 ↓
IMPLEMENT
 ↓
TEST
 ↓
VERIFY
 ↓
DOCUMENT
```

The agent must not silently change architecture.

If implementation reveals that the specification is wrong, stop and report the discrepancy before expanding scope.

---

# 23. Agent Completion Report

Every substantial task should end with:

```markdown
## Implementation Summary

### Implemented

- ...

### Files Changed

- ...

### Tests

- ...

### Device Verification

- ...

### Measurements

- ...

### Known Limitations

- ...

### Decision

KEEP / MODIFY / REPLACE / DROP

### Next Step

- ...
```

This keeps the team aware of what has actually been proven.

---

# 24. Definition of Done

A feature is not complete merely because the code exists.

A feature is complete when:

- [ ] Specification exists
- [ ] Implementation exists
- [ ] Relevant tests exist
- [ ] Physical-device behavior is verified where required
- [ ] Acceptance criteria pass
- [ ] Failure behavior exists
- [ ] Accessibility has been considered
- [ ] Privacy implications have been considered
- [ ] Documentation is updated
- [ ] Known limitations are recorded
- [ ] Architecture changes are documented
- [ ] Git commit is meaningful

---

# 25. Hackathon Rule

When time is limited:

```text
Reliable small system
        >
Large unreliable system
```

Do not sacrifice the core perception loop to add cosmetic features.

The core loop is:

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

If this works convincingly, the project has demonstrated its fundamental idea.

---

# 26. Final Engineering Principle

The project is not trying to prove that the original architecture was correct.

It is trying to discover:

> **What can a modern Android phone actually do when we treat it as a continuous multimodal perception device?**

Every important assumption should therefore be testable.

Every important test should produce evidence.

Every major architectural decision should follow that evidence.
