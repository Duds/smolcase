---
id: 20260830-002
title: "LLM latency degrades from ~3.5s to 10s+ over a session"
type: bug
severity: high
reported: 2026-08-30
---

### 🐛 [BUG]: LLM latency degrades from ~3.5s to 10s+ over a session
*Origin: Local Testing / System Logs*

#### 🚨 Current vs. Expected Behaviour
- **Current Behaviour:** Latency climbs steadily across a single session: early turns ~3.2-4.1s, mid turns ~4.2-6.1s, late turns ~5-10.7s.
- **Expected Behaviour:** Stable latency at ~2-3s per turn throughout the session.

#### 🎬 Replication Steps
1. Start a conversation session with `GemmaBackend`
2. Send consecutive messages to the LLM (8+ turns)
3. Observe latency increase over time, measured via `latency_ms` in conversation logs

#### 🎯 Failure Boundary (BDD-style)
- [ ] **Scenario: Regression Verification**
  - **Given:** A GemmaBackend session with `maxNumTokens=1024`
  - **When:** 10+ turns have been exchanged
  - **Then:** Turn latency exceeds 5,000ms
- [ ] **Scenario: Resolution Validation (Fix Verification)**
  - **Given:** A GemmaBackend session with `maxNumTokens=128`
  - **When:** 10+ turns have been exchanged
  - **Then:** Turn latency stays under 3,000ms

#### 🔍 Diagnostic & Contextual Data
- **Impacted Subsystems:** `GemmaBackend.kt`, LiteRT-LM inference engine
- **Error Logs / Stack Traces:**
  ```text
  | Turn | Latency (ms) |
  |------|-------------|
  | Early | 3,200-4,100 |
  | Mid   | 4,200-6,100 |
  | Late  | 5,000-10,700 |
  ```
- **Environmental Factors:** On-device Pixel 8 (Tensor G3), GemmaBackend only