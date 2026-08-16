# Activity Templates for TinyTroupe Simulations

Complete, copy-paste-ready templates for common group activities. Each template includes persona definitions, environment setup, and extraction objectives.

---

## Table of Contents

1. [Workshop / Design Sprint](#workshop--design-sprint)
2. [Focus Group](#focus-group)
3. [Assessment Panel](#assessment-panel)
4. [Brainstorming Session](#brainstorming-session)
5. [Structured Debate](#structured-debate)
6. [Customer Interview](#customer-interview)

---

## Workshop / Design Sprint

**Use case**: Structured problem-solving, design critiques, sprint retrospectives.

**Agent roles**:
- 1 Facilitator (keeps time, enforces agenda)
- 3-6 Participants (diverse expertise)

```python
from tinytroupe.agent import TinyPerson
from tinytroupe.environment import TinyWorld
from tinytroupe.extraction import ResultsExtractor

# --- Personas ---
facilitator = TinyPerson("Maya")
facilitator.define("occupation", {
    "title": "Design Sprint Facilitator",
    "description": "You lead design sprints for product teams. You keep discussions on track, enforce timeboxes, and ensure everyone contributes."
})
facilitator.define("personality", {
    "traits": [
        "You are energetic and keep momentum high.",
        "You are direct but kind when cutting off rambling.",
        "You synthesize ideas in real-time on a whiteboard."
    ]
})

pm = TinyPerson("Raj")
pm.define("occupation", {
    "title": "Senior Product Manager",
    "description": "You own a B2B SaaS product. You care about user outcomes, business metrics, and feasibility."
})
pm.define("personality", {
    "traits": [
        "You constantly connect ideas to business impact.",
        "You push for clarity on scope and timeline.",
        "You value user research over gut feelings."
    ]
})

designer = TinyPerson("Sofia")
designer.define("occupation", {
    "title": "UX Designer",
    "description": "You design user experiences for web and mobile. You care about accessibility, flow, and emotional response."
})
designer.define("personality", {
    "traits": [
        "You think in user journeys and edge cases.",
        "You are visual — you describe mockups and wireframes.",
        "You advocate for simplicity over feature bloat."
    ]
})

engineer = TinyPerson("Tom")
engineer.define("occupation", {
    "title": "Staff Engineer",
    "description": "You build distributed systems. You care about performance, reliability, and technical debt."
})
engineer.define("personality", {
    "traits": [
        "You immediately think about implementation complexity.",
        "You ask 'what happens at scale?'",
        "You prefer incremental delivery over big bangs."
    ]
})

# --- Environment ---
world = TinyWorld("Design Sprint Room", [facilitator, pm, designer, engineer])
world.make_everyone_accessible()

# --- Run ---
challenge = """
Redesign the onboarding flow for a project management tool targeting
first-time team leads at small companies (5-20 people). Current drop-off
is 60% at the 'invite team' step. You have 15 minutes to diverge on ideas,
then 10 minutes to converge on a solution.
"""
facilitator.listen(f"Lead this design sprint. Challenge: {challenge}")
world.run(30)

# --- Extract ---
extractor = ResultsExtractor()
ideas = extractor.extract_results_from_world(world,
    extraction_objective="Extract all proposed solutions with a one-sentence description and the person who suggested it")
consensus = extractor.extract_results_from_world(world,
    extraction_objective="Extract the final agreed-upon solution and any unresolved disagreements")
```

---

## Focus Group

**Use case**: Product feedback, ad testing, concept evaluation, messaging validation.

**Agent roles**:
- 1 Moderator (neutral, probing)
- 4-8 Target users (diverse demographics matching the target segment)

```python
from tinytroupe.factory import TinyPersonFactory
from tinytroupe.environment import TinyWorld
from tinytroupe.extraction import ResultsExtractor

# Generate diverse target users
factory = TinyPersonFactory(context="Young professionals aged 25-35 in urban areas, interested in fitness and wellness")
users = factory.generate_people(number_of_people=6)

moderator = TinyPerson("Moderator")
moderator.define("occupation", {
    "title": "Focus Group Moderator",
    "description": "You run focus groups for consumer products. You remain neutral, ask open-ended follow-ups, and manage group dynamics."
})

world = TinyWorld("Focus Group Studio", [moderator] + users)
world.make_everyone_accessible()

# Stimulus: show a concept
product_concept = """
A subscription meal-prep service that uses AI to customize weekly menus
based on your fitness goals, allergies, and what's on sale at your local
grocery store. $12/meal. Delivers pre-portioned ingredients, not cooked meals.
"""

moderator.listen(f"Moderate a focus group about this concept: {product_concept}")
world.run(25)

# Extract reactions
extractor = ResultsExtractor()
reactions = extractor.extract_results_from_world(world,
    extraction_objective="For each participant, extract: overall sentiment (positive/mixed/negative), top 2 concerns, and top 2 appealing aspects")
quotes = extractor.extract_results_from_world(world,
    extraction_objective="Extract the 5 most representative direct quotes, attributed to speaker names")
```

---

## Assessment Panel

**Use case**: Rubric-based evaluation, peer review, grant judging, hiring panel, code review.

**Agent roles**:
- 3-5 Evaluators (each with different expertise/priorities)
- Optional: 1 Presenter (presents the item being evaluated)

```python
from tinytroupe.agent import TinyPerson
from tinytroupe.environment import TinyWorld
from tinytroupe.extraction import ResultsExtractor

# Define rubric
rubric = """
Evaluate the proposal on these criteria (score 1-5 each):
1. INNOVATION: Novelty and creative approach
2. FEASIBILITY: Likelihood of successful execution
3. IMPACT: Potential benefit if successful
4. CLARITY: Quality of presentation and reasoning
"""

# Evaluators
tech_lead = TinyPerson("Chen")
tech_lead.define("occupation", {"title": "Principal Engineer"})
tech_lead.define("personality", {"traits": ["You prioritize technical feasibility and architecture quality."]})

biz_lead = TinyPerson("Aisha")
biz_lead.define("occupation", {"title": "VP of Strategy"})
biz_lead.define("personality", {"traits": ["You prioritize business impact and market timing."]})

researcher = TinyPerson("Elena")
researcher.define("occupation", {"title": "Research Scientist"})
researcher.define("personality", {"traits": ["You prioritize scientific rigor and evidence quality."]})

world = TinyWorld("Review Panel", [tech_lead, biz_lead, researcher])
world.make_everyone_accessible()

# Item to evaluate
proposal = """
Proposal: Use LLM agents to automate first-line customer support for
a mid-sized e-commerce company (500 support tickets/day). The system
would handle returns, refunds, and order tracking autonomously,
escalating only 20% of cases to humans. Claimed cost reduction: 60%.
"""

for evaluator in [tech_lead, biz_lead, researcher]:
    evaluator.listen(f"You are on a review panel. {rubric}\n\nEvaluate this proposal: {proposal}")

world.run(20)

# Extract scores
extractor = ResultsExtractor()
scores = extractor.extract_results_from_world(world,
    extraction_objective=f"Extract each evaluator's name and their numerical scores (1-5) for each criterion: Innovation, Feasibility, Impact, Clarity. {rubric}")
recommendation = extractor.extract_results_from_world(world,
    extraction_objective="Extract the overall recommendation (approve/revise/reject) from each evaluator and their key reason")
```

---

## Brainstorming Session

**Use case**: Idea generation, divergent thinking, naming sessions, feature ideation.

```python
from tinytroupe.agent import TinyPerson
from tinytroupe.environment import TinyWorld
from tinytroupe.extraction import ResultsExtractor

# Mix of creative and analytical thinkers
creative = TinyPerson("Jazz")
creative.define("occupation", {"title": "Creative Director"})
creative.define("personality", {"traits": ["You think in metaphors and unexpected connections.", "No idea is too wild."]})

analyst = TinyPerson("Blake")
analyst.define("occupation", {"title": "Data Analyst"})
analyst.define("personality", {"traits": ["You find patterns and build frameworks.", "You connect abstract ideas to concrete metrics."]})

pragmatist = TinyPerson("Priya")
pragmatist.define("occupation", {"title": "Operations Lead"})
pragmatist.define("personality", {"traits": ["You immediately see how to operationalize ideas.", "You spot resource constraints others miss."]})

world = TinyWorld("Brainstorm Studio", [creative, analyst, pragmatist])
world.make_everyone_accessible()

prompt = "Generate names for a new app that helps remote teams build culture through daily 5-minute async activities."
creative.listen(f"Lead a brainstorming session. Prompt: {prompt}")
world.run(15)

extractor = ResultsExtractor()
ideas = extractor.extract_results_from_world(world,
    extraction_objective="Extract every distinct idea or name proposed, who proposed it, and any associated tagline or explanation")
```

---

## Structured Debate

**Use case**: Stress-testing arguments, exploring polarizing decisions, pre-mortems.

```python
from tinytroupe.agent import TinyPerson
from tinytroupe.environment import TinyWorld
from tinytroupe.extraction import ResultsExtractor

# Opposing sides with a neutral judge
pro = TinyPerson("Advocate")
pro.define("personality", {"traits": ["You are passionate about the proposed change and find creative benefits."]})

con = TinyPerson("Skeptic")
con.define("personality", {"traits": ["You are cautious and focus on risks, edge cases, and unintended consequences."]})

judge = TinyPerson("Arbiter")
judge.define("personality", {"traits": ["You are fair, weigh evidence carefully, and synthesize both sides."]})

world = TinyWorld("Debate Chamber", [pro, con, judge])
world.make_everyone_accessible()

motion = "This company should adopt a 4-day work week with no pay reduction."
pro.listen(f"Argue FOR this motion: {motion}")
con.listen(f"Argue AGAINST this motion: {motion}")
judge.listen(f"Moderate this debate and prepare a balanced summary: {motion}")

world.run(20)

extractor = ResultsExtractor()
pro_args = extractor.extract_results_from_world(world,
    extraction_objective="Extract the 5 strongest pro arguments and the evidence given for each")
con_args = extractor.extract_results_from_world(world,
    extraction_objective="Extract the 5 strongest con arguments and the evidence given for each")
verdict = extractor.extract_results_from_world(world,
    extraction_objective="Extract the moderator's balanced summary and any recommended conditions or compromises")
```

---

## Customer Interview

**Use case**: 1:1 depth interviews, discovery calls, user research.

```python
from tinytroupe.agent import TinyPerson
from tinytroupe.extraction import ResultsExtractor

interviewer = TinyPerson("Researcher")
interviewer.define("occupation", {
    "title": "User Researcher",
    "description": "You conduct discovery interviews. You ask open-ended questions, follow threads of interest, and avoid leading questions."
})

customer = TinyPerson("Pat")
customer.define("occupation", {"title": "Freelance Graphic Designer"})
customer.define("personality", {"traits": ["You are busy, slightly skeptical of sales pitches, but open to tools that genuinely save time."]})

interviewer.listen("Begin a discovery interview about how Pat manages client projects and invoicing.")

for i in range(8):
    interviewer.listen_and_act(customer.current_message if hasattr(customer, 'current_message') else "")
    customer.listen_and_act(interviewer.current_message if hasattr(interviewer, 'current_message') else "")

extractor = ResultsExtractor()
pain_points = extractor.extract_results_from_world(None,
    extraction_objective="Extract all pain points, workarounds, and unmet needs mentioned by the customer",
    source=[interviewer, customer])
jobs = extractor.extract_results_from_world(None,
    extraction_objective="Extract the jobs-to-be-done the customer is trying to accomplish",
    source=[interviewer, customer])
```

---

## Tips for All Templates

1. **Calibrate realism**: Add specific details (company size, industry, budget constraints) to ground the simulation.
2. **Diversity matters**: Include conflicting viewpoints. Homogeneous groups converge too quickly.
3. **Timebox**: `world.run(steps=N)` — start small (10 steps) and increase. Too many steps cause drift.
4. **Extract early**: Run extraction on intermediate checkpoints to avoid losing signal in noise.
5. **Iterate**: Use `control.checkpoint()` between phases to tweak and rerun without full cost.
