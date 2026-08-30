---
id: 20260830-004
title: "Humor and honesty dials have no observable effect on LLM output"
type: bug
severity: medium
reported: 2026-08-30
---

### 🐛 [BUG]: Humor and honesty dials have no observable effect on LLM output
*Origin: Local Testing*

#### 🚨 Current vs. Expected Behaviour
- **Current Behaviour:** Same joke returned at both humor=10 and humor=90. Honesty dial shows no observable modulation either.
- **Expected Behaviour:** High humor = drier, more sarcastic, more frequent jokes. Low humor = literal, mission-focused, no jokes. High honesty = blunt, low honesty = more tactful/evasive.

#### 🎬 Replication Steps
1. Set humor to 90 via dial command ("set humor to 90")
2. Ask the LLM for a joke
3. Set humor to 10 via dial command ("set humor to 10")
4. Ask the LLM for a joke again
5. Compare the two replies — they are indistinguishable

#### 🎯 Failure Boundary (BDD-style)
- [ ] **Scenario: Regression Verification**
  - **Given:** `PersonalityDials` set to humor=10 and humor=90 in separate turns
  - **When:** User requests a joke at each setting
  - **Then:** The replies are indistinguishable in dryness/sarcasm
- [ ] **Scenario: Resolution Validation (Fix Verification)**
  - **Given:** Dial lines moved to after the rules block with stronger framing
  - **When:** User requests a joke at humor=10 vs humor=90
  - **Then:** The replies differ significantly (high=dry/sarcastic, low=literal/no jokes)

#### 🔍 Diagnostic & Contextual Data
- **Impacted Subsystems:** `CreaturePersona.prompt()`, `PersonalityDials`
- **Error Logs / Stack Traces:**
  ```text
  > humor=90: "Why did the programmer quit his job? Because he didn't get arrays."
  > humor=10: "Why did the programmer quit his job? Because he didn't get arrays."
  ```
- **Environmental Factors:** All LLM backends; dial values interpolated in middle of persona block