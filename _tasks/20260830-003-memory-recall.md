---
id: 20260830-003
title: "Memory facts not recalled by LLM across turns"
type: bug
severity: medium
reported: 2026-08-30
---

### 🐛 [BUG]: Memory facts not recalled by LLM across turns
*Origin: Local Testing*

#### 🚨 Current vs. Expected Behaviour
- **Current Behaviour:** User says "my favourite colour is blue". When asked "what is my favourite colour" two turns later, the LLM replies "I do not have access to your personal preferences."
- **Expected Behaviour:** The LLM correctly recalls facts stored in `MemoryStore` when asked about them within the same session.

#### 🎬 Replication Steps
1. Tell the creature a fact (e.g., "my favourite colour is blue")
2. Send 1-2 unrelated messages
3. Ask about the previously shared fact
4. Observe the LLM fails to recall it

#### 🎯 Failure Boundary (BDD-style)
- [ ] **Scenario: Regression Verification**
  - **Given:** `MemoryStore` has stored user facts and `soulSummary()` returns them
  - **When:** User asks about a stored fact 2+ turns after sharing it
  - **Then:** The LLM replies it does not know or have access to that information
- [ ] **Scenario: Resolution Validation (Fix Verification)**
  - **Given:** `MemoryStore` has stored user facts with restructured prompt
  - **When:** User asks about a stored fact 2+ turns after sharing it
  - **Then:** The LLM correctly recalls and responds with the stored fact

#### 🔍 Diagnostic & Contextual Data
- **Impacted Subsystems:** `MemoryStore`, `CreaturePersona.prompt()`, `GemmaBackend`, `GeminiNanoBackend`
- **Error Logs / Stack Traces:**
  ```text
  User: "my favourite colour is blue"
  User: (2+ turns later) "what is my favourite colour"
  LLM: "I do not have access to your personal preferences."
  ```
- **Environmental Factors:** All LLM backends, same-session context only