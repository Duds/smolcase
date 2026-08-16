---
name: tinytroupe-workshop
description: |
  Design and run multi-agent persona simulations using Microsoft's TinyTroupe for workshops,
  focus groups, evaluations, assessments, brainstorming sessions, market research, ad testing,
  product feedback, and other group activities. Use when the user wants to: simulate people
  with specific personalities for group interactions; run AI-powered focus groups or workshops;
  evaluate proposals, ads, or products with simulated personas; generate synthetic behavioral
  data from persona interactions; conduct market research or brainstorming with artificial
  agents; or create assessment panels with diverse simulated stakeholders. Also triggers on
  mentions of "TinyTroupe", "persona simulation", "simulated focus group", "AI workshop",
  "synthetic user research", or "agent-based evaluation".
---

# TinyTroupe Workshop Skill

Run multi-agent persona simulations for group activities, evaluations, and workshops using Microsoft's TinyTroupe.

## What This Skill Does

- Sets up TinyTroupe simulations with realistic personas
- Configures group activities (workshops, focus groups, assessments, brainstorming)
- Runs simulations and extracts structured results
- Generates reports from simulated interactions
- Publishes results to Kimi Dashboards (Canvases) as live Widgets
- Validates simulations against real data or LLM-as-judge criteria
- Renders publication-quality PDF briefs via Quoin

## Prerequisites

1. Python 3.10+
2. OpenAI API key or Azure OpenAI credentials
3. TinyTroupe installed: `pip install git+https://github.com/microsoft/TinyTroupe.git@main`
4. Environment variables set:
   - `OPENAI_API_KEY=your-key` (or `AZURE_OPENAI_KEY` + `AZURE_OPENAI_ENDPOINT`)

## Quick Start

```python
from tinytroupe.agent import TinyPerson
from tinytroupe.environment import TinyWorld
from tinytroupe.extraction import ResultsExtractor

# 1. Define personas
facilitator = TinyPerson("Alex")
facilitator.define("occupation", {"title": "Workshop Facilitator"})
facilitator.define("personality", {"traits": ["You are encouraging and keep discussions on track."]})

participant = TinyPerson("Jordan")
participant.define("occupation", {"title": "Product Manager"})
participant.define("personality", {"traits": ["You are skeptical of hype and ask hard questions."]})

# 2. Create environment
world = TinyWorld("Workshop Room", [facilitator, participant])
world.make_everyone_accessible()

# 3. Run simulation
facilitator.listen("Lead a 5-minute brainstorm on AI features for our app")
world.run(10)

# 4. Extract results
extractor = ResultsExtractor()
results = extractor.extract_results_from_world(world)
```

## Core Workflow

### Step 1: Choose Activity Type

Select the simulation pattern. See [references/activity-templates.md](references/activity-templates.md) for full templates.

| Activity | Best For | Agent Count |
|----------|----------|-------------|
| `workshop` | Structured problem-solving, design sprints | 3-8 |
| `focus_group` | Product feedback, ad testing, concept evaluation | 4-10 |
| `assessment_panel` | Rubric-based evaluation, peer review, judging | 3-5 |
| `brainstorm` | Idea generation, divergent thinking | 3-6 |
| `interview` | 1:1 depth interviews, customer discovery | 1-2 |
| `debate` | Stress-testing arguments, polarizing topics | 2-4 |

### Step 2: Define Personas

Create personas with depth. Use `TinyPersonFactory` for populations or define manually.

**Manual definition** (high control):
```python
person = TinyPerson("Name")
person.define("age", 35)
person.define("occupation", {"title": "Role", "description": "Detailed context"})
person.define("personality", {"traits": [...], "big_five": {...}})
person.define("preferences", {"interests": [...], "likes": [...], "dislikes": [...]})
person.define("beliefs", [...])
person.define("behaviors", {"routines": [...]})
```

**Factory-generated** (diverse populations):
```python
from tinytroupe.factory import TinyPersonFactory
factory = TinyPersonFactory.create_factory_from_demography(
    demography_description_or_file_path="./demographics.json",
    population_size=20,
    context="Market research for a fitness app"
)
people = factory.generate_people(number_of_people=20)
```

Load from JSON spec:
```python
person = TinyPerson.load_specification("./personas/analyst.agent.json")
```

**Use pre-built domain personas** from this skill's `assets/personas/`:
```python
marcus = TinyPerson.load_specification("./assets/personas/marcus-firmware-engineer.agent.json")
jordan = TinyPerson.load_specification("./assets/personas/jordan-staff-engineer.agent.json")
elena = TinyPerson.load_specification("./assets/personas/elena-service-designer.agent.json")
thomas = TinyPerson.load_specification("./assets/personas/thomas-strategy-consultant.agent.json")
avery = TinyPerson.load_specification("./assets/personas/avery-dx-engineer.agent.json")
lars = TinyPerson.load_specification("./assets/personas/lars-industrial-designer.agent.json")
ingrid = TinyPerson.load_specification("./assets/personas/ingrid-tech-writer.agent.json")
robert = TinyPerson.load_specification("./assets/personas/robert-enterprise-architect.agent.json")
ana = TinyPerson.load_specification("./assets/personas/ana-design-engineer.agent.json")
```

Apply shared fragments:("./assets/personas/avery-dx-engineer.agent.json")
lars = TinyPerson.load_specification("./assets/personas/lars-industrial-designer.agent.json")
```

Apply shared fragments:
```python
person.import_fragment("./assets/fragments/open-source-culture.fragment.json")
person.import_fragment("./assets/fragments/strategy-thinking.fragment.json")
person.import_fragment("./assets/fragments/hardware-mindset.fragment.json")
```

### Step 3: Configure Environment

```python
world = TinyWorld("Workshop Room", agents)
world.make_everyone_accessible()
```

For structured activities, seed the first message:
```python
facilitator.listen("You are leading a design sprint. Introduce the challenge: redesign the onboarding flow.")
```

### Step 4: Run Simulation

```python
world.run(steps=20)  # Number of interaction steps
```

Enable caching for iterative development:
```python
from tinytroupe import control
control.begin("workshop.cache.json")
world.run(20)
control.checkpoint()
control.end()
```

### Step 5: Extract & Report

```python
from tinytroupe.extraction import ResultsExtractor, ResultsReducer

# Extract structured data
extractor = ResultsExtractor()
results = extractor.extract_results_from_world(world, 
    extraction_objective="Extract all ideas generated and their assigned priority")

# Reduce to summary
reducer = ResultsReducer()
summary = reducer.reduce_results(world, 
    reduction_objective="Summarize consensus and top 3 ideas")
```

## Key Patterns

### Persona Diversity with Fragments

Reuse persona elements across agents:
```python
person.import_fragment("./fragments/tech_skeptic.fragment.json")
```

### Validation

Check persona adherence:
```python
from tinytroupe.validation import TinyPersonValidator
validator = TinyPersonValidator()
score = validator.validate(person, expectation="Should be skeptical of AI hype")
```

### Interventions

Steer simulation mid-run:
```python
from tinytroupe.intervention import Intervention
intervention = Intervention(
    condition=lambda env: env.current_step == 5,
    action=lambda env: env.broadcast("Time to switch to convergent thinking. Pick the top ideas.")
)
world.add_intervention(intervention)
```

### Cost Tracking

Monitor API spend:
```python
from tinytroupe.clients import client
client().pretty_print_cost_stats()
world.pretty_print_cost_stats()
```

## Configuration

Override settings programmatically:
```python
from tinytroupe import config_manager
config_manager.update("model", "gpt-5-mini")
config_manager.update("action_generator_enable_quality_checks", True)
config_manager.update("action_generator_quality_threshold", 6)
```

Or use `config.ini` in your working directory. See TinyTroupe's example config for all options.

## Advanced Features

- **Vision support**: Agents can analyze images (v0.7.0+)
- **Empirical validation**: Compare simulation results to real survey data with `SimulationExperimentEmpiricalValidator`
- **Interactive widgets**: Use `AgentChatJupyterWidget` for real-time agent exploration
- **Parallel execution**: Enable `parallelize=True` for faster population generation
- **Widget dashboards**: Connect simulations to Kimi Blueprint Widgets for visual results
- **PDF briefs**: Generate publication-quality reports via Quoin

## Validation Workflow

Before trusting simulation results, validate them:

1. **Statistical validation** (if real data exists):
   ```python
   from tinytroupe.validation import validate_simulation_experiment_empirically
   result = validate_simulation_experiment_empirically(control_data, treatment_data)
   ```

2. **LLM-as-judge** (always recommended):
   ```python
   from tinytroupe.proposition import Proposition
   prop = Proposition(claim="Agent responses adhere to persona", target=person)
   score = prop.evaluate()  # 0-9; aim for 6+
   ```

See [references/validation-workflows.md](references/validation-workflows.md) for the full pipeline.

## Widget & Canvas Integration

To display results on a Kimi Dashboard:

1. Create a Blueprint Automation that runs the simulation and outputs structured JSON
2. Create a Widget with `slots.main` matching the output schema
3. Bind the Automation to the Widget
4. Place the Widget on a Canvas

See [references/widget-integration.md](references/widget-integration.md) for HTML templates, schemas, and Canvas layouts.

## PDF Report Generation

Generate Quoin PDF briefs from simulation results:

1. Extract results as structured data
2. Generate a `.qn.md` source file (see [references/quoin-reports.md](references/quoin-reports.md))
3. Build: `npx quoin build brief.qn.md -o out/`

Report types available: focus group brief, assessment brief, workshop brief, brainstorm catalog, debate summary, validation report.

## References

- **Activity Templates**: [references/activity-templates.md](references/activity-templates.md) — Complete templates for workshops, focus groups, assessments, brainstorming, and debates.
- **TinyTroupe Patterns**: [references/tinytroupe-patterns.md](references/tinytroupe-patterns.md) — API patterns, factory usage, validation, interventions, and extraction strategies.
- **Persona Library**: [references/persona-library.md](references/persona-library.md) — Pre-built personas for hardware, software, product design, UX/service design, business strategy, systems architecture, and DX.
- **Widget Integration**: [references/widget-integration.md](references/widget-integration.md) — Blueprint Widget/Canvas setup, HTML templates, and live dashboard patterns.
- **Validation Workflows**: [references/validation-workflows.md](references/validation-workflows.md) — Empirical validation, LLM-as-judge, calibration, and report templates.
- **Quoin PDF Reports**: [references/quoin-reports.md](references/quoin-reports.md) — Quoin source authoring, report templates, and automation integration for PDF briefs.
- **Demo Script**: [scripts/demo_workshop.py](scripts/demo_workshop.py) — End-to-end demo: load personas, run simulation, validate, extract, generate Quoin brief, output widget data.
- **Run Script**: [scripts/run_simulation.py](scripts/run_simulation.py) — Executable script that reads a JSON config and runs a simulation.
- **Extract Script**: [scripts/extract_results.py](scripts/extract_results.py) — Executable script to extract and summarize simulation outputs.
- **Pre-built Personas**: `assets/personas/` — 9 domain-specific `.agent.json` files ready to load.
- **Persona Fragments**: `assets/fragments/` — 3 reusable fragments (open-source culture, strategy thinking, hardware mindset).
- **Quoin Templates**: `assets/templates/` — Starter `.qn.md` files for focus group, assessment, and workshop briefs.
- **Run Script**: [scripts/run_simulation.py](scripts/run_simulation.py) — Executable script that reads a JSON config and runs a simulation.
- **Extract Script**: [scripts/extract_results.py](scripts/extract_results.py) — Executable script to extract and summarize simulation outputs.
- **Pre-built Personas**: `assets/personas/` — 6 domain-specific `.agent.json` files ready to load.
- **Persona Fragments**: `assets/fragments/` — 3 reusable fragments (open-source culture, strategy thinking, hardware mindset).

## Important Notes

- TinyTroupe is research/experimental. API may change.
- Always validate simulation results against real data when possible.
- Use content filters (especially with Azure OpenAI) for safety.
- Simulations supplement, not replace, human insight.
