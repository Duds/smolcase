# Empirical Validation Workflows

Validate TinyTroupe simulation results against real-world data to build confidence in simulation-based insights.

---

## Table of Contents

1. [Why Validate](#why-validate)
2. [Validation Pipeline](#validation-pipeline)
3. [Statistical Comparison](#statistical-comparison)
4. [LLM-as-Judge Validation](#llm-as-judge-validation)
5. [Calibration Workflow](#calibration-workflow)
6. [Validation Report Template](#validation-report-template)

---

## Why Validate

Simulated personas are not real people. Validation answers:
- **Do simulated preferences match real preferences?** (e.g., product rankings)
- **Do simulated sentiment distributions match real surveys?** (e.g., NPS, Likert scales)
- **Do simulated debates explore the same argument space as real ones?**

TinyTroupe provides `SimulationExperimentEmpiricalValidator` for quantitative comparison. Use it when you have (or can collect) real control data.

---

## Validation Pipeline

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Run Simulation │────→│ Collect Real    │────→│  Compare with   │
│  (TinyTroupe)   │     │  Data (Survey)  │     │  Statistical    │
│                 │     │                 │     │  Tests          │
└─────────────────┘     └─────────────────┘     └─────────────────┘
         │                                               │
         ↓                                               ↓
┌─────────────────┐                           ┌─────────────────┐
│ Extract results │                           │ Generate        │
│ as structured   │                           │ Validation      │
│ data (CSV/JSON) │                           │ Report          │
└─────────────────┘                           └─────────────────┘
```

---

## Statistical Comparison

### Single-Value Per Agent

Best for: Likert scales, ratings, binary choices, NPS.

```python
from tinytroupe.validation import (
    SimulationExperimentEmpiricalValidator,
    validate_simulation_experiment_empirically
)
import pandas as pd

# 1. Load real survey data
control_data = SimulationExperimentEmpiricalValidator.read_empirical_data_from_csv(
    file_path="real_survey.csv",
    experimental_data_type="single_value_per_agent",
    agent_id_column="Responder #",
    value_column="Satisfaction_Score",  # 1-5 Likert
    agent_comments_column="Comments",
    dataset_name="Real Survey (n=50)"
)

# 2. Create treatment data from simulation
# After running simulation, extract agent responses
sim_df = pd.DataFrame([
    {"name": agent.name, "Satisfaction_Score": extracted_score}
    for agent, extracted_score in zip(agents, scores)
])

treatment_data = SimulationExperimentEmpiricalValidator.read_empirical_data_from_dataframe(
    df=sim_df,
    experimental_data_type="single_value_per_agent",
    agent_id_column="name",
    value_column="Satisfaction_Score",
    dataset_name="Simulation (n=50)"
)

# 3. Run statistical validation
result = validate_simulation_experiment_empirically(
    control_data=control_data,
    treatment_data=treatment_data,
    validation_types=["statistical"],
    statistical_test_type="t_test",  # or "ks_test"
    output_format="values"
)

print(f"Overall validation score: {result.overall_score}")
print(f"Statistical results: {result.statistical_results}")
```

### Ordinal Ranking Per Agent

Best for: preference rankings, A/B/C option selection, Borda counts.

```python
# Real data: each respondent ranked options 1st, 2nd, 3rd
control_data = SimulationExperimentEmpiricalValidator.read_empirical_data_from_csv(
    file_path="ranking_survey.csv",
    experimental_data_type="ordinal_ranking_per_agent",
    agent_id_column="Responder #",
    value_column="Ranking",  # e.g., "A>B>C"
    dataset_name="Real Ranking Survey"
)

# Simulation: extract rankings from agents
treatment_data = SimulationExperimentEmpiricalValidator.read_empirical_data_from_dataframe(
    df=sim_rankings_df,
    experimental_data_type="ordinal_ranking_per_agent",
    agent_id_column="name",
    value_column="Ranking",
    dataset_name="Simulation Rankings"
)

result = validate_simulation_experiment_empirically(
    control_data=control_data,
    treatment_data=treatment_data,
    validation_types=["statistical"],
    statistical_test_type="ks_test"
)
```

### Interpreting Results

| Metric | What It Means | Action If Poor |
|--------|---------------|----------------|
| **t-test p-value** > 0.05 | Distributions are similar | Good — sim matches reality |
| **t-test p-value** < 0.05 | Distributions differ significantly | Revise personas or prompt |
| **KS-test** | Tests shape similarity (not just means) | Prefer when distributions are non-normal |
| **Overall score** | Composite confidence metric | < 0.6 means redesign needed |

---

## LLM-as-Judge Validation

When you lack real data, use an LLM to evaluate simulation quality against defined criteria.

```python
from tinytroupe.validation import TinyPersonValidator
from tinytroupe.proposition import Proposition

# 1. Persona adherence
adherence_prop = Proposition(
    claim="The agent's responses are consistent with their stated persona specification",
    target=person,
    prefix_length=10,
    suffix_length=0
)
adherence_score = adherence_prop.evaluate()  # 0-9

# 2. Self-consistency
consistency_prop = Proposition(
    claim="The agent's current position is consistent with their earlier stated positions",
    target=person,
    prefix_length=20,
    suffix_length=0
)
consistency_score = consistency_prop.evaluate()

# 3. Fluency
fluency_prop = Proposition(
    claim="The agent's responses are fluent, coherent, and not repetitive",
    target=person,
    prefix_length=5,
    suffix_length=0
)
fluency_score = fluency_prop.evaluate()

# 4. Custom: argument diversity
diversity_prop = Proposition(
    claim="The agent explores diverse arguments rather than repeating the same point",
    target=person,
    prefix_length=15,
    suffix_length=0
)
diversity_score = diversity_prop.evaluate()
```

### Quality Thresholds

| Score | Quality |
|-------|---------|
| 7–9 | Excellent — no action needed |
| 5–6 | Acceptable — monitor |
| 3–4 | Poor — intervene or revise |
| 0–2 | Unusable — restart simulation |

Enable automatic action correction:
```python
from tinytroupe import config_manager
config_manager.update("action_generator_enable_quality_checks", True)
config_manager.update("action_generator_quality_threshold", 6)
config_manager.update("action_generator_max_attempts", 5)
```

---

## Calibration Workflow

Iteratively improve simulation accuracy:

```
Iteration 1: Run simulation with initial personas
     ↓
Compare to real data (or judge quality)
     ↓
Identify gaps: e.g., simulators are too positive
     ↓
Revise personas: add skepticism, reduce optimism bias
     ↓
Iteration 2: Re-run with revised personas
     ↓
Compare again
     ↓
Converged? → Lock personas for future runs
```

### Calibration Checklist

- [ ] Run simulation with diverse persona set (not all optimistic)
- [ ] Extract quantitative outcomes (ratings, rankings, choices)
- [ ] Compare to real data using t-test or KS-test
- [ ] If no real data, run LLM-as-judge on adherence, consistency, fluency
- [ ] Revise personas that score below threshold
- [ ] Re-run and compare again
- [ ] Document final persona specs as validated baseline

---

## Validation Report Template

Generate a structured validation report after each simulation:

```markdown
# Simulation Validation Report

## Simulation Metadata
- Activity type: [workshop / focus_group / assessment / ...]
- Date: YYYY-MM-DD
- Agents: N
- Steps: M
- Cost: $X.XX

## Validation Method
- [ ] Statistical comparison to real data
- [ ] LLM-as-judge evaluation
- [ ] Both

## Statistical Results
(if applicable)
- Test type: t-test / KS-test
- p-value: X.XXX
- Overall score: X.XX
- Interpretation: [distributions match / distributions differ]

## LLM-as-Judge Results
(if applicable)
- Persona adherence: X/9
- Self-consistency: X/9
- Fluency: X/9
- [Custom criterion]: X/9

## Gaps Identified
1. [e.g., Simulated agents were too agreeable]
2. [e.g., Missing demographic: no junior perspectives]

## Recommended Revisions
1. [e.g., Add conflict-oriented persona]
2. [e.g., Reduce action correction strictness]

## Confidence Level
- [ ] High — results can inform decisions
- [ ] Medium — useful for directional insight
- [ ] Low — for exploration only
```

Store reports alongside simulation configs for auditability.
