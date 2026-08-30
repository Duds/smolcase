---
id: BUG-TEMPLATE
title: "Bug Report Template"
type: template
---

### 🐛 [BUG]: Short, Descriptive Summary of Issue
*Origin: Local Scratchpad / System Logs*

#### 🚨 Current vs. Expected Behaviour
- **Current Behaviour:** [What the system is actually doing right now]
- **Expected Behaviour:** [What the system should be doing instead]

#### 🎬 Replication Steps
*Clear, sequential actions to trigger the defect:*
1. Go to `[Insert component / route]`
2. Execute action `[Insert action]`
3. Observe error / failure state `[Insert exact error message or visual anomaly]`

#### 🎯 Failure Boundary (BDD-style)
*Defines the exact conditions where the system breaks:*
- [ ] **Scenario: Regression Verification**
  - **Given:** [The initial state or preconditions required]
  - **When:** [The replication steps are executed]
  - **Then:** [The current broken behaviour is observed]
- [ ] **Scenario: Resolution Validation (Fix Verification)**
  - **Given:** [The same initial state or preconditions]
  - **When:** [The replication steps are executed after the fix]
  - **Then:** [The expected behavior is successfully restored without side effects]

#### 🔍 Diagnostic & Contextual Data
- **Impacted Subsystems:** [e.g., API Gateway, DB Migrations, Client State]
- **Error Logs / Stack Traces:**
  ```text
  [Paste raw error output, stack traces, or failing network payloads here]
  ```
- **Environmental Factors:** [e.g., Only happens on Node 20+, Safari mobile only, Dev vs Prod]