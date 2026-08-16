#!/usr/bin/env python3
"""Unified TinyTroupe Activity Runner — run any simulation from a JSON config."""
import json
import sys
import argparse
from pathlib import Path
from datetime import datetime

from tinytroupe.agent import TinyPerson
from tinytroupe.environment import TinyWorld
from tinytroupe.extraction import ResultsExtractor
from tinytroupe import control


def load_persona_from_json(path: Path) -> TinyPerson:
    """Load a persona from a .agent.json file, avoiding registry duplicates."""
    spec = json.loads(path.read_text())
    persona = spec.get("persona", spec)
    name = persona.get("name", "Agent")
    tp = TinyPerson(name)
    for key, value in persona.items():
        if key == "name":
            continue
        tp.define(key, value)
    return tp


def create_moderator(name="Maya") -> TinyPerson:
    mod = TinyPerson(name)
    mod.define("occupation", {
        "title": "Moderator",
        "description": "You facilitate group discussions neutrally."
    })
    mod.define("personality", {"traits": [
        "You are neutral and do not lead participants.",
        "You ask open-ended follow-ups.",
        "You ensure quieter participants speak up."
    ]})
    return mod


def create_facilitator(name="Alex") -> TinyPerson:
    fac = TinyPerson(name)
    fac.define("occupation", {
        "title": "Workshop Facilitator",
        "description": "You lead structured workshops and keep time."
    })
    fac.define("personality", {"traits": [
        "You are energetic and keep momentum high.",
        "You are direct but kind when cutting off rambling.",
        "You synthesize ideas in real-time."
    ]})
    return fac


def run_focus_group(cfg: dict, output_dir: Path) -> dict:
    """Run a focus group simulation."""
    personas = [load_persona_from_json(Path(p)) for p in cfg["personas"]]
    moderator = create_moderator(cfg.get("moderator_name", "Maya"))
    agents = personas + [moderator]
    world = TinyWorld(cfg.get("world_name", "Focus Group Studio"), agents)
    world.make_everyone_accessible()
    moderator.listen(cfg["stimulus"])
    steps = cfg.get("steps", 20)
    cache = output_dir / f"{cfg['name']}_cache.json"
    control.begin(str(cache))
    world.run(steps)
    control.end()
    return {"world": world, "agents": agents, "steps": steps}


def run_brainstorm(cfg: dict, output_dir: Path) -> dict:
    """Run a brainstorming session."""
    personas = [load_persona_from_json(Path(p)) for p in cfg["personas"]]
    facilitator = create_facilitator(cfg.get("facilitator_name", "Alex"))
    agents = personas + [facilitator]
    world = TinyWorld(cfg.get("world_name", "Brainstorm Studio"), agents)
    world.make_everyone_accessible()
    facilitator.listen(cfg["stimulus"])
    steps = cfg.get("steps", 15)
    cache = output_dir / f"{cfg['name']}_cache.json"
    control.begin(str(cache))
    world.run(steps)
    control.end()
    return {"world": world, "agents": agents, "steps": steps}


def run_debate(cfg: dict, output_dir: Path) -> dict:
    """Run a structured debate."""
    pro = TinyPerson(cfg.get("pro_name", "Advocate"))
    pro.define("personality", {"traits": [
        "You are passionate about the proposed change and find creative benefits."
    ]})
    con = TinyPerson(cfg.get("con_name", "Skeptic"))
    con.define("personality", {"traits": [
        "You are cautious and focus on risks, edge cases, and unintended consequences."
    ]})
    judge = TinyPerson(cfg.get("judge_name", "Arbiter"))
    judge.define("personality", {"traits": [
        "You are fair, weigh evidence carefully, and synthesize both sides."
    ]})
    agents = [pro, con, judge]
    world = TinyWorld(cfg.get("world_name", "Debate Chamber"), agents)
    world.make_everyone_accessible()
    motion = cfg["stimulus"]
    pro.listen(f"Argue FOR this motion: {motion}")
    con.listen(f"Argue AGAINST this motion: {motion}")
    judge.listen(f"Moderate this debate and prepare a balanced summary: {motion}")
    steps = cfg.get("steps", 20)
    cache = output_dir / f"{cfg['name']}_cache.json"
    control.begin(str(cache))
    world.run(steps)
    control.end()
    return {"world": world, "agents": agents, "steps": steps}


def run_assessment(cfg: dict, output_dir: Path) -> dict:
    """Run an assessment panel."""
    personas = [load_persona_from_json(Path(p)) for p in cfg["personas"]]
    rubric = cfg.get("rubric", "Evaluate on: Innovation, Feasibility, Impact, Clarity (1-5 each).")
    for p in personas:
        p.listen(f"You are on a review panel. {rubric}\n\nEvaluate this: {cfg['stimulus']}")
    world = TinyWorld(cfg.get("world_name", "Review Panel"), personas)
    world.make_everyone_accessible()
    steps = cfg.get("steps", 20)
    cache = output_dir / f"{cfg['name']}_cache.json"
    control.begin(str(cache))
    world.run(steps)
    control.end()
    return {"world": world, "agents": personas, "steps": steps}


def run_interview(cfg: dict, output_dir: Path) -> dict:
    """Run a 1:1 customer interview."""
    interviewer = TinyPerson(cfg.get("interviewer_name", "Researcher"))
    interviewer.define("occupation", {
        "title": "User Researcher",
        "description": "You conduct discovery interviews."
    })
    customer = load_persona_from_json(Path(cfg["personas"][0]))
    interviewer.listen(cfg["stimulus"])
    world = TinyWorld(cfg.get("world_name", "Interview Room"), [interviewer, customer])
    world.make_everyone_accessible()
    steps = cfg.get("steps", 16)
    cache = output_dir / f"{cfg['name']}_cache.json"
    control.begin(str(cache))
    world.run(steps)
    control.end()
    return {"world": world, "agents": [interviewer, customer], "steps": steps}


ACTIVITY_RUNNERS = {
    "focus_group": run_focus_group,
    "brainstorm": run_brainstorm,
    "debate": run_debate,
    "assessment": run_assessment,
    "interview": run_interview,
}


def extract_results(world, activity_type: str) -> dict:
    """Extract structured results based on activity type."""
    extractor = ResultsExtractor()
    results = {
        "timestamp": datetime.now().isoformat(),
        "activity_type": activity_type,
    }

    if activity_type == "focus_group":
        results["sentiment"] = extractor.extract_results_from_world(
            world, extraction_objective="Summarize overall sentiment as one word: positive, mixed, or negative"
        )
        results["findings"] = extractor.extract_results_from_world(
            world, extraction_objective="Extract the top 5 key findings or insights"
        )
        results["ideas"] = extractor.extract_results_from_world(
            world, extraction_objective="Extract the top 5 appealing aspects or ideas"
        )
        results["dissent"] = extractor.extract_results_from_world(
            world, extraction_objective="Extract the top 5 concerns or objections"
        )

    elif activity_type == "brainstorm":
        results["ideas"] = extractor.extract_results_from_world(
            world, extraction_objective="Extract every distinct idea proposed, who proposed it, and any tagline"
        )
        results["themes"] = extractor.extract_results_from_world(
            world, extraction_objective="Group the ideas into 3-5 thematic clusters"
        )

    elif activity_type == "debate":
        results["pro_args"] = extractor.extract_results_from_world(
            world, extraction_objective="Extract the 5 strongest pro arguments and evidence"
        )
        results["con_args"] = extractor.extract_results_from_world(
            world, extraction_objective="Extract the 5 strongest con arguments and evidence"
        )
        results["verdict"] = extractor.extract_results_from_world(
            world, extraction_objective="Extract the moderator's balanced summary and any recommended compromises"
        )

    elif activity_type == "assessment":
        results["scores"] = extractor.extract_results_from_world(
            world, extraction_objective="Extract each evaluator's name and their numerical scores for each criterion"
        )
        results["recommendations"] = extractor.extract_results_from_world(
            world, extraction_objective="Extract each evaluator's overall recommendation and key reason"
        )

    elif activity_type == "interview":
        results["pain_points"] = extractor.extract_results_from_world(
            world, extraction_objective="Extract all pain points, workarounds, and unmet needs"
        )
        results["jobs_to_be_done"] = extractor.extract_results_from_world(
            world, extraction_objective="Extract the jobs-to-be-done the customer is trying to accomplish"
        )

    return results


def main():
    parser = argparse.ArgumentParser(description="Run a TinyTroupe simulation from a JSON config")
    parser.add_argument("config", help="Path to JSON config file")
    parser.add_argument("--output", "-o", default="./output", help="Output directory")
    parser.add_argument("--no-extract", action="store_true", help="Skip result extraction")
    args = parser.parse_args()

    cfg_path = Path(args.config)
    if not cfg_path.exists():
        print(f"Config not found: {cfg_path}")
        sys.exit(1)

    cfg = json.loads(cfg_path.read_text())
    activity = cfg.get("activity_type", "focus_group")
    name = cfg.get("name", "simulation")

    if activity not in ACTIVITY_RUNNERS:
        print(f"Unknown activity type: {activity}. Supported: {list(ACTIVITY_RUNNERS.keys())}")
        sys.exit(1)

    output_dir = Path(args.output)
    output_dir.mkdir(parents=True, exist_ok=True)

    print("=" * 60)
    print(f"🎭 TinyTroupe Activity Runner")
    print(f"   Activity: {activity}")
    print(f"   Name: {name}")
    print(f"   Output: {output_dir}")
    print("=" * 60)

    # Run simulation
    print(f"\n🚀 Running {activity} simulation...")
    result = ACTIVITY_RUNNERS[activity](cfg, output_dir)
    print("   ✅ Simulation complete")

    # Extract results
    if not args.no_extract:
        print("\n📊 Extracting results...")
        extracted = extract_results(result["world"], activity)
        extracted["simulation_name"] = name
        extracted["agents"] = [a.name for a in result["agents"]]
        extracted["steps_run"] = result["steps"]

        results_path = output_dir / f"{name}_results.json"
        results_path.write_text(json.dumps(extracted, indent=2, default=str))
        print(f"   💾 Saved: {results_path}")

        # Print summary
        print("\n" + "=" * 60)
        print("📋 SUMMARY")
        print("=" * 60)
        for key, value in extracted.items():
            if key in ("timestamp", "activity_type", "simulation_name", "agents", "steps_run"):
                continue
            print(f"\n🔹 {key}:")
            if isinstance(value, list):
                for item in value:
                    print(f"   • {item}")
            else:
                print(f"   {value}")

    print("\n✅ Done!")


if __name__ == "__main__":
    main()
