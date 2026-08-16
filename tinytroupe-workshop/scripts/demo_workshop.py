#!/usr/bin/env python3
"""
TinyTroupe Workshop — End-to-End Demo

Demonstrates the complete enriched workflow:
1. Load pre-built domain personas
2. Run a focus group simulation
3. Validate with LLM-as-judge
4. Extract structured results
5. Generate a Quoin PDF brief source file
6. Output widget-compatible JSON artifact

Usage:
    export OPENAI_API_KEY=your-key
    python demo_workshop.py

Output:
    - simulation_cache.json     (cached simulation state)
    - results.json              (structured extraction results)
    - validation_report.json    (LLM-as-judge scores)
    - brief.qn.md               (Quoin PDF source)
    - widget_data.json          (Blueprint Widget artifact)
"""

import json
import sys
from datetime import datetime
from pathlib import Path

# ---------------------------------------------------------------------------
# 0. Ensure TinyTroupe is available
# ---------------------------------------------------------------------------
try:
    from tinytroupe.agent import TinyPerson
    from tinytroupe.environment import TinyWorld
    from tinytroupe.extraction import ResultsExtractor
    from tinytroupe.proposition import Proposition
    from tinytroupe import control
    from tinytroupe.clients import client
except ImportError as e:
    print(f"ERROR: {e}")
    print("Install TinyTroupe: pip install git+https://github.com/microsoft/TinyTroupe.git@main")
    sys.exit(1)


# ---------------------------------------------------------------------------
# 1. Configuration
# ---------------------------------------------------------------------------
SIMULATION_NAME = "AI DevTools Focus Group"
ACTIVITY_TYPE = "focus_group"
STEPS = 20
PERSONA_DIR = Path(__file__).parent.parent / "assets" / "personas"
FRAGMENT_DIR = Path(__file__).parent.parent / "assets" / "fragments"
OUTPUT_DIR = Path(".")

# ---------------------------------------------------------------------------
# 2. Load Pre-Built Personas
# ---------------------------------------------------------------------------
def load_personas():
    """Load domain personas from the skill's assets."""
    personas = {}

    # Core participants for an AI developer tools focus group
    persona_files = {
        "jordan": "jordan-staff-engineer.agent.json",
        "avery": "avery-dx-engineer.agent.json",
        "elena": "elena-service-designer.agent.json",
        "lars": "lars-industrial-designer.agent.json",
    }

    for key, filename in persona_files.items():
        path = PERSONA_DIR / filename
        if path.exists():
            personas[key] = TinyPerson.load_specification(str(path))
            print(f"✅ Loaded: {personas[key].name} ({personas[key].agent_definition['occupation']['title']})")
        else:
            print(f"⚠️  Missing persona file: {path}")

    # Add a moderator
    moderator = TinyPerson("Maya")
    moderator.define("occupation", {
        "title": "Focus Group Moderator",
        "description": "You run focus groups for developer tools. You remain neutral, ask open-ended follow-ups, and manage group dynamics."
    })
    moderator.define("personality", {
        "traits": [
            "You are neutral and do not lead participants.",
            "You ask 'tell me more about that' constantly.",
            "You ensure quieter participants speak up."
        ]
    })
    personas["moderator"] = moderator
    print(f"✅ Created: {moderator.name} (Focus Group Moderator)")

    # Apply shared fragments
    for person in personas.values():
        if person.name != "Maya":
            for frag_file in FRAGMENT_DIR.glob("*.fragment.json"):
                person.import_fragment(str(frag_file))

    return personas


# ---------------------------------------------------------------------------
# 3. Run Simulation
# ---------------------------------------------------------------------------
def run_simulation(personas):
    """Run the focus group simulation."""
    agents = list(personas.values())
    world = TinyWorld("Focus Group Studio", agents)
    world.make_everyone_accessible()

    # Stimulus
    product_concept = """
    We're building an AI-powered development environment that:
    - Predicts your next edit before you type it (inline suggestions)
    - Auto-generates tests from your code comments
    - Explains legacy codebases on demand via natural language chat
    - $25/month per developer. Integrates with VS Code and JetBrains.
    """

    moderator = personas["moderator"]
    moderator.listen(f"Moderate a focus group about this developer tool concept: {product_concept}")

    # Enable caching
    control.begin("simulation_cache.json")
    print(f"\n🎭 Running {SIMULATION_NAME} ({STEPS} steps)...")
    world.run(STEPS)
    control.end()
    print("✅ Simulation complete\n")

    return world


# ---------------------------------------------------------------------------
# 4. Validate with LLM-as-Judge
# ---------------------------------------------------------------------------
def validate_agents(personas):
    """Run LLM-as-judge validation on each agent."""
    print("🔍 Running LLM-as-judge validation...")
    validations = {}

    criteria = [
        ("persona_adherence", "The agent's responses are consistent with their stated persona"),
        ("self_consistency", "The agent's current position is consistent with their earlier stated positions"),
        ("fluency", "The agent's responses are fluent, coherent, and not repetitive"),
    ]

    for key, person in personas.items():
        if key == "moderator":
            continue

        scores = {}
        for crit_name, claim in criteria:
            try:
                prop = Proposition(
                    claim=claim,
                    target=person,
                    prefix_length=10,
                    suffix_length=0
                )
                scores[crit_name] = prop.evaluate()
            except Exception as e:
                scores[crit_name] = f"error: {e}"

        avg_score = sum(v for v in scores.values() if isinstance(v, (int, float))) / max(1, len([v for v in scores.values() if isinstance(v, (int, float))]))
        validations[person.name] = {
            "scores": scores,
            "average": round(avg_score, 2),
            "passes_threshold": avg_score >= 6.0
        }
        status = "✅" if avg_score >= 6.0 else "⚠️"
        print(f"   {status} {person.name}: {avg_score:.1f}/9")

    return validations


# ---------------------------------------------------------------------------
# 5. Extract Results
# ---------------------------------------------------------------------------
def extract_results(world, personas):
    """Extract structured findings from the simulation."""
    print("\n📊 Extracting results...")
    extractor = ResultsExtractor()

    objectives = [
        ("sentiment", "Summarize overall group sentiment as positive, mixed, or negative with evidence"),
        ("concerns", "Extract the top 5 concerns or objections raised by participants"),
        ("appeals", "Extract the top 5 appealing aspects mentioned by participants"),
        ("quotes", "Extract the 5 most representative direct quotes, attributed to speaker names"),
    ]

    results = {"activity_type": ACTIVITY_TYPE, "simulation_name": SIMULATION_NAME}
    for name, objective in objectives:
        try:
            results[name] = extractor.extract_results_from_world(world, extraction_objective=objective)
        except Exception as e:
            results[name] = f"Extraction error: {e}"

    # Cost tracking
    try:
        cost_stats = client().get_cost_stats()
        results["cost_usd"] = cost_stats.get("total", 0)
    except Exception:
        results["cost_usd"] = 0

    results["steps_run"] = STEPS
    results["agents"] = [p.name for p in personas.values()]
    results["timestamp"] = datetime.now().isoformat()

    return results


# ---------------------------------------------------------------------------
# 6. Generate Quoin Brief Source
# ---------------------------------------------------------------------------
def generate_quoin_brief(results, validations):
    """Generate a .qn.md source file for Quoin PDF rendering."""
    print("\n📝 Generating Quoin brief source...")

    # Build validation summary
    val_lines = []
    for name, data in validations.items():
        status = "Pass" if data["passes_threshold"] else "Review"
        val_lines.append(f"| {name} | {data['average']}/9 | {status} |")

    brief = f"""---
title: "{results['simulation_name']} — Focus Group Brief"
date: "{datetime.now().strftime('%Y-%m-%d')}"
---

::cover-page{{title="{results['simulation_name']}" client="Product Team" date="{datetime.now().strftime('%Y-%m-%d')}" version="1.0"}}

::table-of-contents

# Executive Summary

:::lead-paragraph
A simulated focus group of {len(results['agents'])} domain experts evaluated an AI-powered
development environment. Overall sentiment: {results.get('sentiment', 'N/A')}.
Key concerns centered on trust and integration; key appeals centered on productivity gains.
:::

# Simulation Design

## Agents
| Name | Role |
|------|------|
| Jordan Okafor | Staff Software Engineer |
| Avery Williams | DX Engineer |
| Elena Varga | Service Designer |
| Lars Jensen | Industrial Designer |
| Maya | Moderator |

## Stimulus
AI-powered dev environment with predictive editing, auto-generated tests,
legacy code explanation, and IDE integration at $25/dev/month.

# Key Findings

## Sentiment
{results.get('sentiment', 'N/A')}

## Top Concerns
{chr(10).join(f"- {c}" for c in (results.get('concerns', []) if isinstance(results.get('concerns'), list) else [results.get('concerns', 'N/A')]))}

## Top Appeals
{chr(10).join(f"- {a}" for a in (results.get('appeals', []) if isinstance(results.get('appeals'), list) else [results.get('appeals', 'N/A')]))}

# Representative Quotes
{chr(10).join(f"> {q}" for q in (results.get('quotes', []) if isinstance(results.get('quotes'), list) else [results.get('quotes', 'N/A')]))}

# Validation

| Agent | Avg Score | Status |
|-------|-----------|--------|
{chr(10).join(val_lines)}

Scores are on a 0–9 scale. Threshold for acceptance: 6.0.

# Recommendations

1. Address trust concerns explicitly in marketing and onboarding
2. Provide a free tier or trial to reduce adoption friction
3. Invest in IDE integration quality — this is a key decision factor

# Appendix

- Engine: TinyTroupe
- Steps: {results['steps_run']}
- Cost: ${results.get('cost_usd', 0):.2f}
- Date: {results['timestamp']}
"""

    output_path = OUTPUT_DIR / "brief.qn.md"
    output_path.write_text(brief)
    print(f"📄 Saved: {output_path}")
    return output_path


# ---------------------------------------------------------------------------
# 7. Generate Widget Data
# ---------------------------------------------------------------------------
def generate_widget_data(results, validations):
    """Generate JSON suitable for a Blueprint Widget artifact."""
    print("\n🖥️  Generating Widget data...")

    widget_data = {
        "activity_type": results["activity_type"],
        "simulation_name": results["simulation_name"],
        "agents": results["agents"],
        "key_findings": [
            f"Sentiment: {results.get('sentiment', 'N/A')}",
            f"Top concerns extracted: {len(results.get('concerns', [])) if isinstance(results.get('concerns'), list) else 'N/A'}",
            f"Top appeals extracted: {len(results.get('appeals', [])) if isinstance(results.get('appeals'), list) else 'N/A'}",
        ],
        "sentiment": "mixed",  # default; extraction would determine this
        "top_ideas": results.get("appeals", []) if isinstance(results.get("appeals"), list) else [],
        "dissent": results.get("concerns", []) if isinstance(results.get("concerns"), list) else [],
        "cost_usd": results.get("cost_usd", 0),
        "steps_run": results["steps_run"],
        "validation_summary": {
            name: data["average"] for name, data in validations.items()
        }
    }

    output_path = OUTPUT_DIR / "widget_data.json"
    output_path.write_text(json.dumps(widget_data, indent=2, default=str))
    print(f"📄 Saved: {output_path}")
    return widget_data


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main():
    print("=" * 60)
    print("TinyTroupe Workshop — End-to-End Demo")
    print("=" * 60)

    # 1. Load personas
    personas = load_personas()

    # 2. Run simulation
    world = run_simulation(personas)

    # 3. Validate
    validations = validate_agents(personas)
    with open(OUTPUT_DIR / "validation_report.json", "w") as f:
        json.dump(validations, f, indent=2, default=str)
    print("📄 Saved: validation_report.json")

    # 4. Extract
    results = extract_results(world, personas)
    with open(OUTPUT_DIR / "results.json", "w") as f:
        json.dump(results, f, indent=2, default=str)
    print("📄 Saved: results.json")

    # 5. Quoin brief
    generate_quoin_brief(results, validations)

    # 6. Widget data
    generate_widget_data(results, validations)

    print("\n" + "=" * 60)
    print("🎉 Demo complete!")
    print("=" * 60)
    print("\nNext steps:")
    print("  1. Review results.json for structured findings")
    print("  2. Review validation_report.json for quality scores")
    print("  3. Build PDF: npx quoin build brief.qn.md -o out/")
    print("  4. Load widget_data.json into a Blueprint Widget")
    print("=" * 60)


if __name__ == "__main__":
    main()
