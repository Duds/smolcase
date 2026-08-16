#!/usr/bin/env python3
"""Live TinyTroupe Focus Group Demo — run in Kimi Work"""
import json
import sys
from datetime import datetime
from pathlib import Path

# TinyTroupe imports
from tinytroupe.agent import TinyPerson
from tinytroupe.environment import TinyWorld
from tinytroupe.extraction import ResultsExtractor
from tinytroupe import control
from tinytroupe.clients import client

# Paths
OUTPUT = Path("/Users/dalerogers/20-INDIE/products/SMOLCASE/tinytroupe-workshop/output")
OUTPUT.mkdir(exist_ok=True)

print("=" * 60)
print("🎭 TinyTroupe Live Focus Group Demo")
print("=" * 60)

# ---------------------------------------------------------------------------
# 1. Define personas programmatically (avoids load_specification registry issues)
# ---------------------------------------------------------------------------
print("\n📦 Defining personas...")

jordan = TinyPerson("Jordan Okafor")
jordan.define("age", 36)
jordan.define("nationality", "Nigerian-British")
jordan.define("occupation", {
    "title": "Staff Software Engineer",
    "organization": "Platform team at a fintech",
    "description": "You design systems that handle millions of transactions. You review architecture RFCs, mentor senior engineers, and still write code. You are the person people call when prod is on fire."
})
jordan.define("personality", {
    "traits": [
        "You ask 'what happens at 10x scale?' in every review.",
        "You prefer deleting code to writing it.",
        "You are calm during incidents and ruthless in post-mortems.",
        "You value observability over optimism."
    ],
    "big_five": {
        "openness": "High. Explores new paradigms but demands evidence.",
        "conscientiousness": "Very high. Nothing ships without tests and runbooks.",
        "extraversion": "Medium. Leads meetings but prefers deep work.",
        "agreeableness": "Medium. Kind but will block a bad design.",
        "neuroticism": "Low. Incidents are puzzles, not panic."
    }
})
jordan.define("preferences", {
    "interests": ["Distributed systems", "Event-driven architecture", "Rust", "Observability"],
    "likes": ["Clear error messages", "Good tracing", "Backward compatibility", "Idempotent APIs"],
    "dislikes": ["Microservices for microservices' sake", "Magic frameworks", "Undocumented assumptions"]
})
print(f"   ✅ {jordan.name} — Staff Software Engineer")

avery = TinyPerson("Avery Williams")
avery.define("age", 31)
avery.define("nationality", "American")
avery.define("occupation", {
    "title": "Developer Experience Engineer",
    "organization": "Developer tools company",
    "description": "You build the tools, docs, and workflows that make engineers productive. You measure time-to-first-PR, API error rates, and documentation findability. You are obsessed with developer joy."
})
avery.define("personality", {
    "traits": [
        "You treat every API like a user interface.",
        "You get angry at 404s in documentation.",
        "You measure onboarding friction in minutes, not days.",
        "You believe great DX is a competitive moat."
    ],
    "big_five": {
        "openness": "High. Tries every new developer tool.",
        "conscientiousness": "High. Tracks every metric.",
        "extraversion": "Medium. Runs demos and workshops.",
        "agreeableness": "High. Advocates for developer needs.",
        "neuroticism": "Medium. Frustrated by bad tooling."
    }
})
avery.define("preferences", {
    "interests": ["CLI design", "API ergonomics", "Documentation systems", "Developer metrics"],
    "likes": ["Great error messages", "Interactive tutorials", "Fast feedback loops", "Open APIs"],
    "dislikes": ["Broken quickstarts", "Undocumented breaking changes", "Gatekept documentation"]
})
print(f"   ✅ {avery.name} — DX Engineer")

elena = TinyPerson("Elena Varga")
elena.define("age", 38)
elena.define("nationality", "Hungarian")
elena.define("occupation", {
    "title": "Lead Service Designer",
    "organization": "Government digital service",
    "description": "You design end-to-end services across channels — online, phone, in-person. You map journeys, identify failure demand, and redesign bureaucratic processes. You care about equity and accessibility."
})
elena.define("personality", {
    "traits": [
        "You see systems, not screens.",
        "You get angry about services that offload work to users.",
        "You interview frontline staff as much as end users.",
        "You measure success by reduced call volume, not NPS."
    ],
    "big_five": {
        "openness": "High. Always mapping new system boundaries.",
        "conscientiousness": "High. Documents every design decision.",
        "extraversion": "Medium. Facilitates workshops naturally.",
        "agreeableness": "High. Builds consensus across silos.",
        "neuroticism": "Low. Bureaucracy is a design problem, not a personal affront."
    }
})
elena.define("preferences", {
    "interests": ["Service blueprints", "Policy design", "Accessibility", "Public sector innovation"],
    "likes": ["Journey maps", "Co-design workshops", "Reduced failure demand", "Plain language"],
    "dislikes": ["Digital-only thinking", "Assumption-based design", "Vanity metrics"]
})
print(f"   ✅ {elena.name} — Service Designer")

# Create moderator
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
print(f"   ✅ {moderator.name} — Moderator")

personas = {"jordan": jordan, "avery": avery, "elena": elena, "moderator": moderator}

# ---------------------------------------------------------------------------
# 2. Setup environment
# ---------------------------------------------------------------------------
print("\n🏟️  Setting up environment...")
agents = list(personas.values())
world = TinyWorld("Focus Group Studio", agents)
world.make_everyone_accessible()

# Stimulus
stimulus = """
We're building an AI-powered development environment that:
- Predicts your next edit before you type it (inline suggestions)
- Auto-generates tests from your code comments
- Explains legacy codebases on demand via natural language chat
- Costs $25/month per developer
- Integrates with VS Code and JetBrains IDEs

Please evaluate this concept from your professional perspective.
What aspects appeal to you? What concerns do you have? Would you advocate for or against adopting this tool on your team?
"""

moderator.listen(f"Moderate a focus group about this developer tool concept. Ask each participant for their perspective on: value proposition, concerns about adoption, and whether they'd recommend it. Keep the conversation flowing naturally.\n\nConcept: {stimulus}")

# ---------------------------------------------------------------------------
# 3. Run simulation (8 steps to keep cost/time manageable)
# ---------------------------------------------------------------------------
STEPS = 8
print(f"\n🚀 Running simulation ({STEPS} steps)...")
print("   (This may take 3-5 minutes)")

control.begin(str(OUTPUT / "demo_cache.json"))
world.run(STEPS)
control.end()

print("   ✅ Simulation complete")

# ---------------------------------------------------------------------------
# 4. Extract results
# ---------------------------------------------------------------------------
print("\n📊 Extracting results...")
extractor = ResultsExtractor()

objectives = [
    ("sentiment", "Summarize overall group sentiment as exactly one word: positive, mixed, or negative. Then explain briefly."),
    ("findings", "Extract the top 4 key findings or insights from the discussion. Each as one concise sentence."),
    ("ideas", "Extract the top 4 ideas, suggestions, or appealing aspects mentioned by participants. Each as one concise sentence."),
    ("dissent", "Extract the top 4 concerns, objections, or dissenting points. Each as one concise sentence."),
]

results = {
    "simulation_name": "AI DevTools Focus Group",
    "activity_type": "focus_group",
    "status": "complete",
    "agents": [p.name for p in agents],
    "steps_run": STEPS,
    "timestamp": datetime.now().isoformat(),
}

for name, objective in objectives:
    try:
        result = extractor.extract_results_from_world(world, extraction_objective=objective)
        # Parse as list
        if isinstance(result, str):
            lines = [l.strip("- •\t ") for l in result.split("\n") if l.strip() and not l.strip().startswith("#")]
            results[name] = [l for l in lines if l and len(l) > 10][:5]
        elif isinstance(result, list):
            results[name] = [str(r) for r in result][:5]
        else:
            results[name] = [str(result)]
    except Exception as e:
        results[name] = [f"Extraction error: {e}"]
        print(f"   ⚠️  {name} extraction issue: {e}")

# Determine sentiment
sentiment_text = " ".join(results.get("sentiment", ["mixed"]))
if "positive" in sentiment_text.lower():
    results["sentiment"] = "positive"
elif "negative" in sentiment_text.lower():
    results["sentiment"] = "negative"
else:
    results["sentiment"] = "mixed"

# Cost
try:
    cost_stats = client().get_cost_stats()
    results["cost_usd"] = cost_stats.get("total", 0)
except:
    results["cost_usd"] = 0

# ---------------------------------------------------------------------------
# 5. Save outputs
# ---------------------------------------------------------------------------
results_path = OUTPUT / "demo_results.json"
results_path.write_text(json.dumps(results, indent=2, default=str))
print(f"\n📄 Saved: {results_path}")

# ---------------------------------------------------------------------------
# 6. Print summary
# ---------------------------------------------------------------------------
print("\n" + "=" * 60)
print("📋 RESULTS SUMMARY")
print("=" * 60)
print(f"\n🎭 Activity: {results['simulation_name']}")
print(f"👤 Agents: {', '.join(results['agents'])}")
print(f"⚡ Steps: {results['steps_run']}")
print(f"💰 Cost: ${results['cost_usd']:.4f}")
print(f"📊 Sentiment: {results['sentiment'].upper()}")

print(f"\n🔑 Key Findings:")
for f in results.get("findings", [])[:4]:
    print(f"   • {f}")

print(f"\n💡 Top Ideas/Appeals:")
for i in results.get("ideas", [])[:4]:
    print(f"   • {i}")

print(f"\n⚠️  Dissent/Concerns:")
for d in results.get("dissent", [])[:4]:
    print(f"   • {d}")

print("\n" + "=" * 60)
print("✅ Demo complete!")
print("=" * 60)
