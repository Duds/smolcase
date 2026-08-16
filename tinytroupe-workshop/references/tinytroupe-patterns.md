# TinyTroupe API Patterns

Reference for common TinyTroupe patterns, utilities, and advanced techniques.

---

## Table of Contents

1. [Persona Definition Patterns](#persona-definition-patterns)
2. [Population Generation](#population-generation)
3. [Environment Control](#environment-control)
4. [Interventions & Steering](#interventions--steering)
5. [Validation Techniques](#validation-techniques)
6. [Result Extraction Strategies](#result-extraction-strategies)
7. [Caching & Cost Management](#caching--cost-management)
8. [Configuration Management](#configuration-management)

---

## Persona Definition Patterns

### Complete Manual Definition

```python
from tinytroupe.agent import TinyPerson

person = TinyPerson("Name")
person.define("age", 34)
person.define("gender", "Non-binary")
person.define("nationality", "German")
person.define("residence", "Berlin")
person.define("education", "TU Berlin, MSc Computer Science")
person.define("occupation", {
    "title": "Senior DevOps Engineer",
    "organization": "SaaS startup",
    "description": "You manage cloud infrastructure. You care about uptime, cost optimization, and developer experience."
})
person.define("long_term_goals", [
    "Build infrastructure that scales effortlessly",
    "Mentor junior engineers"
])
person.define("style", "Direct, technical, occasionally sarcastic")
person.define("personality", {
    "traits": [
        "You are impatient with vague requirements.",
        "You automate everything you touch.",
        "You value observability and metrics."
    ],
    "big_five": {
        "openness": "Medium. Practical creativity.",
        "conscientiousness": "High. Extremely organized.",
        "extraversion": "Low. Prefers async communication.",
        "agreeableness": "Medium. Helpful but blunt.",
        "neuroticism": "Low. Calm under incidents."
    }
})
person.define("preferences", {
    "interests": ["Kubernetes", "Observability", "Rust", "Home automation"],
    "likes": ["Clean dashboards", "Infrastructure as Code", "On-call rotations that work"],
    "dislikes": ["Manual processes", "Vendor lock-in", "Alert fatigue"]
})
person.define("beliefs", [
    "You get the infrastructure you deserve.",
    "Complexity is the enemy of reliability.",
    "Every manual step is a bug waiting to happen."
])
person.define("behaviors", {
    "routines": [
        "Morning: check dashboards, review overnight alerts",
        "Weekly: capacity planning and cost review"
    ],
    "habits": [
        "Writes runbooks for every alert",
        "Documents decisions in Architecture Decision Records"
    ]
})
```

### JSON Specification File

Save personas as `.agent.json` files for reuse:

```json
{
    "type": "TinyPerson",
    "persona": {
        "name": "Dr. Sarah Chen",
        "age": 42,
        "occupation": {
            "title": "Clinical Psychologist",
            "description": "You specialize in anxiety disorders. You are careful, evidence-based, and prioritize patient safety."
        },
        "personality": {
            "traits": [
                "You speak carefully and avoid overstating confidence.",
                "You ask clarifying questions before giving advice.",
                "You are deeply skeptical of wellness trends lacking evidence."
            ]
        },
        "beliefs": [
            "Mental health treatment must be personalized.",
            "The placebo effect is real but not a treatment strategy."
        ]
    }
}
```

Load it:
```python
person = TinyPerson.load_specification("./personas/dr_chen.agent.json")
```

### Fragments for Reuse

Create shared fragments:

```json
{
    "type": "Fragment",
    "persona": {
        "preferences": {
            "interests": ["Climate tech", "Regenerative agriculture"],
            "likes": ["Data-driven policy", "Systems thinking"]
        },
        "beliefs": [
            "Technology alone won't solve the climate crisis.",
            "Local solutions often outperform global mandates."
        ]
    }
}
```

Apply to multiple agents:
```python
for person in population:
    person.import_fragment("./fragments/climate_conscious.fragment.json")
```

---

## Population Generation

### Factory with Context

```python
from tinytroupe.factory import TinyPersonFactory

factory = TinyPersonFactory(context="A fintech startup in Lagos, Nigeria")
person = factory.generate_person(
    "Create a young professional who is skeptical of traditional banks but cautious about new apps"
)
```

### Factory from Demographics

```python
factory = TinyPersonFactory.create_factory_from_demography(
    demography_description_or_file_path="./populations/us_midwest.json",
    population_size=50,
    context="Market research for a home insurance app"
)
people = factory.generate_people(number_of_people=50, parallelize=True)

# Inspect diversity
print(factory.sampling_dimensions)
print(factory.sampling_plan)
print(factory.generated_minibios)
```

### Profiling Generated Populations

```python
from tinytroupe.profiling import Profiler

profiler = Profiler()
profiler.profile(people)  # Shows demographic distributions
```

---

## Environment Control

### Basic World

```python
from tinytroupe.environment import TinyWorld

world = TinyWorld("Meeting Room", agents)
world.make_everyone_accessible()
world.run(20)
```

### Broadcasting Messages

```python
world.broadcast("The deadline has moved up by 2 weeks.")
```

### Custom Environment Subclass

```python
from tinytroupe.environment import TinyWorld

class WorkshopRoom(TinyWorld):
    def __init__(self, name, agents, agenda):
        super().__init__(name, agents)
        self.agenda = agenda
        self.current_agenda_item = 0
    
    def advance_agenda(self):
        if self.current_agenda_item < len(self.agenda):
            self.broadcast(f"AGENDA ITEM {self.current_agenda_item + 1}: {self.agenda[self.current_agenda_item]}")
            self.current_agenda_item += 1

agenda = ["Problem framing (5 min)", "Divergent ideation (10 min)", "Convergence (10 min)"]
world = WorkshopRoom("Sprint Room", agents, agenda)
```

---

## Interventions & Steering

### Time-Based Interventions

```python
from tinytroupe.intervention import Intervention

# Force a topic shift at step 10
topic_shift = Intervention(
    condition=lambda env: env.current_step == 10,
    action=lambda env: env.broadcast("Time's up for ideation. Now rank your top 3 ideas.")
)
world.add_intervention(topic_shift)
```

### Stalled Conversation Detection

```python
import time

last_activity_time = time.time()

def check_stalled(env):
    global last_activity_time
    # Custom logic to detect repetition or silence
    return (time.time() - last_activity_time) > 30

nudge = Intervention(
    condition=check_stalled,
    action=lambda env: env.broadcast("Let's hear from someone who hasn't spoken yet.")
)
```

### TinyStory for Narrative Structure

```python
from tinytroupe.story import TinyStory

story = TinyStory(world)
story.add_event(5, "A new constraint is revealed: budget is cut by 50%")
story.add_event(15, "The CEO enters and asks for a 30-second pitch")
story.run()
```

---

## Validation Techniques

### Persona Adherence Check

```python
from tinytroupe.validation import TinyPersonValidator

validator = TinyPersonValidator()
score = validator.validate(person, expectation="Should be skeptical of vendor solutions")
print(f"Adherence score: {score}")  # 0-9 scale
```

### Propositions for Claims

```python
from tinytroupe.proposition import Proposition

# Check if agent maintains technical depth
prop = Proposition(
    claim="The agent's responses demonstrate deep infrastructure knowledge",
    target=person,
    prefix_length=10,  # Use last 10 events for context
    suffix_length=0
)
result = prop.evaluate()  # Returns score 0-9 or boolean
```

### Self-Consistency Check

```python
from tinytroupe.validation import self_consistency_proposition

prop = self_consistency_proposition(person)
score = prop.evaluate()
```

---

## Result Extraction Strategies

### World-Level Extraction

```python
from tinytroupe.extraction import ResultsExtractor

extractor = ResultsExtractor()

# Extract specific data points
decisions = extractor.extract_results_from_world(world,
    extraction_objective="Extract all decisions made, who made them, and the reasoning")

# Extract sentiment
sentiment = extractor.extract_results_from_world(world,
    extraction_objective="Classify overall group sentiment as positive/mixed/negative with evidence")
```

### Agent-Level Extraction

```python
# Extract a single agent's position
position = extractor.extract_results_from_agent(person,
    extraction_objective="Extract this person's final recommendation and confidence level")
```

### Reducers for Structured Data

```python
from tinytroupe.extraction import ResultsReducer

reducer = ResultsReducer()

# Transform conversation to synthetic dataset
dataset = reducer.reduce_results(world,
    reduction_objective="Convert each turn into a JSON object with {speaker, role, message, sentiment}")
```

### Artifact Export

```python
from tinytroupe.artifact import ArtifactExporter

exporter = ArtifactExporter()
exporter.export(world, format="markdown", filename="workshop_output.md")
```

---

## Caching & Cost Management

### Simulation State Caching

```python
from tinytroupe import control

control.begin("simulation.cache.json")
world.run(10)
control.checkpoint()  # Save state

# Modify something, then resume from checkpoint
world.run(10)
control.end()
```

### API Call Caching

In `config.ini`:
```ini
[Caching]
CACHE_API_CALLS=True
```

Or programmatically:
```python
from tinytroupe import config_manager
config_manager.update("cache_api_calls", True)
```

### Cost Tracking

```python
from tinytroupe.clients import client

# After simulation
client().pretty_print_cost_stats()           # Total API cost
world.pretty_print_cost_stats()               # This simulation's cost
TinyPerson.pretty_print_global_cost_stats()   # All agents total
```

Set budget limits:
```python
config_manager.update("max_cost_per_simulation", 5.00)  # $5 USD
```

---

## Configuration Management

### Programmatic Overrides

```python
from tinytroupe import config_manager

# Model selection
config_manager.update("model", "gpt-5-mini")
config_manager.update("embedding_model", "text-embedding-3-small")

# Quality control
config_manager.update("action_generator_enable_quality_checks", True)
config_manager.update("action_generator_quality_threshold", 6)
config_manager.update("action_generator_max_attempts", 5)

# Parallel execution
config_manager.update("parallelize", True)

# Logging
config_manager.update("log_level", "INFO")
```

### Custom Config File

Place `config.ini` in your working directory:

```ini
[OpenAI]
API_TYPE=openai
MODEL=gpt-5-mini
EMBEDDING_MODEL=text-embedding-3-small
TEMPERATURE=0.7
MAX_TOKENS=2000

[Simulation]
PARALLELIZE=True
MAX_STEPS=100

[Caching]
CACHE_API_CALLS=True

[ActionGenerator]
ENABLE_QUALITY_CHECKS=True
QUALITY_THRESHOLD=6
MAX_ATTEMPTS=5
```

### Switching to Azure OpenAI

```ini
[OpenAI]
API_TYPE=azure
AZURE_OPENAI_KEY=${AZURE_OPENAI_KEY}
AZURE_OPENAI_ENDPOINT=${AZURE_OPENAI_ENDPOINT}
MODEL=gpt-4o-mini
API_VERSION=2024-02-01
```

---

## Common Pitfalls

| Pitfall | Fix |
|---------|-----|
| Agents converge too quickly | Add diverse/conflicting personas; use divergence interventions |
| Agents drift off-topic | Use structured agendas; add topic-check interventions |
| Responses feel too "helpful" | Define personality traits that emphasize human flaws |
| Extraction misses details | Use specific extraction objectives; run multiple extractors |
| Costs spiral | Enable caching; start with fewer steps; use cheaper models for drafts |
| Validation scores are low | Personas may be too complex; simplify or enable action correction |
