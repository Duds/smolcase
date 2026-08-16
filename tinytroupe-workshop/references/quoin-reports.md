# Quoin PDF Report Generation

Generate publication-quality PDF briefs from TinyTroupe simulation results using the Quoin print publishing engine.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Report Types](#report-types)
3. [Source Authoring](#source-authoring)
4. [Automation Integration](#automation-integration)
5. [Example: Focus Group Brief](#example-focus-group-brief)
6. [Example: Assessment Panel Brief](#example-assessment-panel-brief)

---

## Prerequisites

1. Quoin installed: `npm install -g @quoin/cli`
2. Simulation results extracted as structured JSON
3. `quoin.config.json` in your project root

Typical `quoin.config.json` for briefs:
```json
{
  "theme": "report",
  "pageSize": "A4",
  "margins": { "top": "20mm", "bottom": "20mm", "left": "25mm", "right": "25mm" }
}
```

---

## Report Types

| Simulation Type | Quoin Source | Output Style |
|-----------------|--------------|--------------|
| **Focus Group** | `focus-group-brief.qn.md` | Structured findings + quotes + sentiment |
| **Assessment Panel** | `assessment-brief.qn.md` | Score tables + verdicts + rationale |
| **Workshop** | `workshop-brief.qn.md` | Agenda + ideas + decisions + next steps |
| **Brainstorm** | `brainstorm-brief.qn.md` | Idea catalog + themes + prioritization |
| **Debate** | `debate-brief.qn.md` | Motion + pro/con arguments + verdict |
| **Validation Report** | `validation-report.qn.md` | Stats + scores + confidence assessment |

---

## Source Authoring

Quoin uses semantic source files. Never write CSS or layout code. Author content in `.qn.md` with component directives.

### Template Structure

Every brief follows this structure:
```markdown
---
title: "[Simulation Name] — [Activity Type] Brief"
date: "YYYY-MM-DD"
---

::cover-page{title="..." client="Internal" date="YYYY-MM-DD" version="1.0"}

::table-of-contents

# Executive Summary

:::lead-paragraph
One-paragraph summary of what was simulated and the key insight.
:::

# Simulation Design

## Agents
| Name | Role | Key Trait |
|------|------|-----------|
| ...  | ...  | ...       |

## Stimulus
What was presented to the agents (product concept, proposal, motion, etc.)

# Key Findings

## Finding 1: [Title]
Detail and evidence.

::pull-quote{author="Agent Name"}
Direct quote supporting this finding.
::

# [Activity-Specific Section]

# Validation

# Appendix: Raw Transcript Extracts
```

---

## Automation Integration

The simulation automation can generate the `.qn.md` source dynamically:

```python
def generate_quoin_source(results: dict, activity_type: str) -> str:
    """Generate a .qn.md source file from simulation results."""
    
    lines = [
        "---",
        f'title: "{results["simulation_name"]} — {activity_type.title()} Brief"',
        f'date: "{datetime.now().strftime("%Y-%m-%d")}"',
        "---",
        "",
        f'::cover-page{{title="{results["simulation_name"]}" client="Simulation" date="{datetime.now().strftime("%Y-%m-%d")}" version="1.0"}}',
        "",
        "::table-of-contents",
        "",
        "# Executive Summary",
        "",
        ":::lead-paragraph",
        results["executive_summary"],
        ":::",
        "",
        "# Simulation Design",
        "",
        "## Agents",
        "",
        "| Name | Role |",
        "|------|------|",
    ]
    
    for agent in results["agents"]:
        lines.append(f"| {agent['name']} | {agent['role']} |")
    
    lines.extend([
        "",
        "## Stimulus",
        "",
        results.get("stimulus", "N/A"),
        "",
        "# Key Findings",
        "",
    ])
    
    for i, finding in enumerate(results["key_findings"], 1):
        lines.extend([
            f"## Finding {i}: {finding['title']}",
            "",
            finding["detail"],
            "",
        ])
        if "quote" in finding:
            lines.extend([
                f'::pull-quote{{author="{finding["quote_author"]}"}}',
                finding["quote"],
                "::",
                "",
            ])
    
    lines.extend([
        "# Appendix: Methodology",
        "",
        f"- Simulation engine: TinyTroupe",
        f"- Agents: {len(results['agents'])}",
        f"- Steps: {results.get('steps_run', 'N/A')}",
        f"- Cost: ${results.get('cost_usd', 0):.2f}",
    ])
    
    return "\n".join(lines)
```

Then build:
```bash
npx quoin build report.qn.md -o out/
```

---

## Example: Focus Group Brief

```markdown
---
title: "AI Meal Prep App — Focus Group Brief"
date: "2026-08-07"
---

::cover-page{title="AI Meal Prep App" client="Product Team" date="2026-08-07" version="1.0"}

::table-of-contents

# Executive Summary

:::lead-paragraph
Six simulated young professionals evaluated an AI-powered meal-prep concept.
Overall sentiment was mixed-positive (4/6 positive), with strong appeal around
customization but significant concerns about price and trust.
:::

# Simulation Design

## Agents
Six agents generated via TinyPersonFactory with demographic targeting:
urban professionals, 25–35, fitness-conscious.

| Name | Profile |
|------|---------|
| Alex | Gym-goer, meal-prep skeptic |
| Jordan | Busy consultant, values convenience |
| Casey | Home cook, price-sensitive |
| Riley | Vegan, sustainability-focused |
| Morgan | Tech early adopter |
| Taylor | Parent, time-constrained |

## Stimulus
"A subscription meal-prep service that uses AI to customize weekly menus based
on fitness goals, allergies, and local grocery sales. $12/meal. Delivers
pre-portioned ingredients."

# Key Findings

## Finding 1: Customization Is the Primary Hook

All six agents responded positively to the AI customization angle. The ability
to align meals with fitness goals was seen as genuinely differentiated.

::pull-quote{author="Riley"}
"If it actually respects my allergies *and* my macros without me thinking about
it, that's worth paying attention to."
::

## Finding 2: Price Is a Barrier at $12/Meal

Four of six agents flagged $12/meal as expensive compared to grocery + prep or
competitor services. The price-to-value ratio was the most common objection.

::pull-quote{author="Casey"}
"I can meal-prep for under $5 a serving. $12 is restaurant territory."
::

## Finding 3: Trust in AI Food Recommendations Is Low

Three agents expressed skepticism that AI could reliably handle allergies,
preferences, and local inventory simultaneously without errors.

# Sentiment Summary

| Agent | Sentiment | Top Concern | Top Appeal |
|-------|-----------|-------------|------------|
| Alex | Positive | Price | Fitness alignment |
| Jordan | Mixed | Commitment | Time saved |
| Casey | Negative | Price | None strong |
| Riley | Positive | AI trust | Sustainability |
| Morgan | Positive | None | Innovation |
| Taylor | Mixed | Complexity | Family-friendly |

# Recommendations

1. **Test pricing elasticity** — simulate at $8, $10, $12 price points
2. **Add trust signals** — show how AI validates allergy inputs
3. **Segment messaging** — lead with fitness for gym-goers, time for busy professionals

# Appendix

- Engine: TinyTroupe v0.7.0
- Model: gpt-5-mini
- Cost: $2.34
- Validation: LLM-as-judge adherence scores 7.2/9 avg
```

Build:
```bash
npx quoin build focus-group-brief.qn.md -o out/
# Output: out/focus-group-brief.pdf
```

---

## Example: Assessment Panel Brief

```markdown
---
title: "LLM Support Bot — Assessment Panel Brief"
date: "2026-08-07"
---

::cover-page{title="LLM Support Bot Proposal" client="Architecture Review" date="2026-08-07" version="1.0"}

::table-of-contents

# Executive Summary

:::lead-paragraph
Three expert evaluators assessed a proposal to deploy LLM agents for first-line
customer support. Two evaluators recommended conditional approval; one recommended
revision. Key risks: hallucination, data privacy, and escalation design.
:::

# Evaluator Scores

| Evaluator | Innovation | Feasibility | Impact | Clarity | Verdict |
|-----------|-----------|-------------|--------|---------|---------|
| Chen (Engineering) | 4 | 3 | 5 | 4 | Revise |
| Aisha (Strategy) | 5 | 2 | 5 | 3 | Approve* |
| Elena (Research) | 3 | 4 | 4 | 5 | Approve* |

*Conditional on addressing key risks

# Key Concerns

## Chen: Technical Feasibility

::pull-quote{author="Chen"}
"Hallucination rates in open-domain QA are still 5–10%. In a support context,
that's thousands of wrong answers daily. Need guardrails not mentioned in the
proposal."
::

## Aisha: Business Impact

::pull-quote{author="Aisha"}
"The 60% cost reduction claim assumes 80% resolution rate. Industry benchmark
for LLM-only support is closer to 50%. The business case needs stress-testing."
::

## Elena: Research Rigor

::pull-quote{author="Elena"}
"No mention of A/B testing plan, success metrics, or human-in-the-loop
validation. This is a deployment proposal, not an experiment."
::

# Recommended Conditions for Approval

1. Define hallucination guardrails and fallback protocols
2. Revise business case with conservative resolution-rate assumptions
3. Include phased rollout with A/B test and human review loop
4. Conduct privacy impact assessment before data access

# Appendix

- Panel composition: Engineering, Strategy, Research
- Scoring: 1–5 per criterion
- Engine: TinyTroupe v0.7.0
```

---

## Tips for Quoin + TinyTroupe

1. **Extract first, then format** — Run `ResultsExtractor` to get structured data, then map fields to Quoin components
2. **Use `pull-quote` for agent voices** — Direct quotes add authenticity to the brief
3. **Tables for scores/rankings** — Quoin handles Markdown tables well; use them for assessment panels
4. **Lead paragraphs for summaries** — `:::lead-paragraph` gives the executive summary visual weight
5. **Version your briefs** — Include date and version in cover-page for tracking iterations
6. **Automate the pipeline** — The simulation automation can generate `.qn.md`, run `quoin build`, and deliver the PDF as an artifact
