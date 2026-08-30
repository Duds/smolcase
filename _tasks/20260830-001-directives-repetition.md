---
id: 20260830-001
title: "LLM repeatedly parrots 'directives' from system prompt"
type: bug
severity: high
reported: 2026-08-30
---

### 🐛 [BUG]: LLM repeatedly parrots "directives" from system prompt
*Origin: System Prompt Leakage*

#### 🚨 Current vs. Expected Behaviour
- **Current Behaviour:** In nearly every LLM reply, the model uses the word "directives" — a word from the system prompt's `CreaturePersona.prompt()`.
- **Expected Behaviour:** The creature should never repeat or acknowledge the word "directives" or any other system prompt terminology. Replies should be natural in-character TARS.

#### 🎬 Replication Steps
1. Start a conversation with any LLM backend (GemmaBackend, GeminiNanoBackend)
2. Ask any question to the creature
3. Observe the reply contains the word "directives"

#### 🎯 Failure Boundary (BDD-style)
- [ ] **Scenario: Regression Verification**
  - **Given:** A conversation is active with the current `CreaturePersona.prompt()`
  - **When:** Any user message is processed by the LLM backend
  - **Then:** The reply contains the word "directives" (or "execute directives")
- [ ] **Scenario: Resolution Validation (Fix Verification)**
  - **Given:** A conversation is active with the updated prompt (no "directives")
  - **When:** Any user message is processed by the LLM backend
  - **Then:** The reply is natural in-character TARS without repeating system prompt terminology

#### 🔍 Diagnostic & Contextual Data
- **Impacted Subsystems:** `CreaturePersona.prompt()`, `GemmaBackend`, `GeminiNanoBackend`
- **Error Logs / Stack Traces:**
  ```text
  > "My directives are to execute assigned tasks efficiently."
  > "My directives are to process input and execute directives efficiently."
  > "I will execute the directives."
  ```
- **Environmental Factors:** All LLM backends, all conversations