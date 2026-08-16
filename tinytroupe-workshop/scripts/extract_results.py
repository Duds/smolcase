#!/usr/bin/env python3
"""
TinyTroupe Results Extractor
Extracts and summarizes simulation results into structured formats.

Usage:
    python extract_results.py --world-cache workshop.cache.json --output report.md
    python extract_results.py --agent-logs ./logs/ --output summary.json --format json

Supports output formats: markdown, json, csv
"""

import argparse
import json
import sys
from pathlib import Path

try:
    from tinytroupe.extraction import ResultsExtractor, ResultsReducer
    from tinytroupe import control
except ImportError:
    print("ERROR: TinyTroupe not installed.")
    sys.exit(1)


def extract_from_world(world, objectives: list) -> dict:
    """Run multiple extraction objectives against a world."""
    extractor = ResultsExtractor()
    results = {}
    
    for obj in objectives:
        key = obj.get("name", obj["objective"][:30])
        print(f"   🔍 Extracting: {key}")
        try:
            result = extractor.extract_results_from_world(world, extraction_objective=obj["objective"])
            results[key] = result
        except Exception as e:
            results[key] = {"error": str(e)}
    
    return results


def format_markdown(results: dict, title: str = "Simulation Report") -> str:
    """Format extraction results as Markdown."""
    lines = [f"# {title}", ""]
    
    for key, value in results.items():
        lines.append(f"## {key}")
        lines.append("")
        if isinstance(value, dict) and "error" in value:
            lines.append(f"⚠️ Error: {value['error']}")
        else:
            lines.append(str(value))
        lines.append("")
    
    return "\n".join(lines)


def format_json(results: dict) -> str:
    """Format extraction results as JSON."""
    return json.dumps(results, indent=2, default=str)


def get_default_objectives(activity_type: str) -> list:
    """Get default extraction objectives for an activity type."""
    objectives = {
        "workshop": [
            {"name": "Ideas Generated", "objective": "Extract all ideas or solutions proposed during the session"},
            {"name": "Decisions Made", "objective": "Extract any decisions or agreements reached"},
            {"name": "Unresolved Items", "objective": "Extract disagreements or items left unresolved"}
        ],
        "focus_group": [
            {"name": "Sentiment Summary", "objective": "Summarize overall sentiment with evidence"},
            {"name": "Top Concerns", "objective": "Extract the top 5 concerns or objections raised"},
            {"name": "Top Appeals", "objective": "Extract the top 5 appealing aspects mentioned"}
        ],
        "assessment": [
            {"name": "Scores", "objective": "Extract numerical scores for each criterion from each evaluator"},
            {"name": "Verdicts", "objective": "Extract each evaluator's overall recommendation and key reason"}
        ],
        "brainstorm": [
            {"name": "Ideas List", "objective": "Extract every distinct idea proposed with attribution"},
            {"name": "Themes", "objective": "Identify common themes or clusters among the ideas"}
        ],
        "debate": [
            {"name": "Pro Arguments", "objective": "Extract the strongest arguments for the motion"},
            {"name": "Con Arguments", "objective": "Extract the strongest arguments against the motion"},
            {"name": "Verdict", "objective": "Extract any consensus or moderator conclusion"}
        ]
    }
    return objectives.get(activity_type, [
        {"name": "Key Takeaways", "objective": "Extract the main insights and conclusions from the simulation"},
        {"name": "Notable Quotes", "objective": "Extract the 5 most representative quotes with attribution"}
    ])


def main():
    parser = argparse.ArgumentParser(description="Extract results from TinyTroupe simulations")
    parser.add_argument("--world-cache", help="Path to simulation cache file")
    parser.add_argument("--activity-type", default="generic",
                        choices=["workshop", "focus_group", "assessment", "brainstorm", "debate", "interview", "generic"])
    parser.add_argument("--objectives", help="JSON file with custom extraction objectives")
    parser.add_argument("--output", required=True, help="Output file path")
    parser.add_argument("--format", default="markdown", choices=["markdown", "json", "csv"])
    args = parser.parse_args()
    
    # Load objectives
    if args.objectives:
        with open(args.objectives) as f:
            objectives = json.load(f)
    else:
        objectives = get_default_objectives(args.activity_type)
    
    print(f"📊 Extracting results for: {args.activity_type}")
    print(f"   Objectives: {len(objectives)}")
    
    # In a real scenario, we'd load the world from cache
    # For now, this is a scaffold that shows the intended flow
    # TODO: Implement world deserialization when TinyTroupe supports it
    
    print("⚠️  Note: Full world deserialization from cache requires TinyToupe's internal APIs.")
    print("   Use this script as a template and adapt to your simulation's output format.")
    
    # Placeholder results
    results = {
        "activity_type": args.activity_type,
        "objectives": [o["name"] for o in objectives],
        "note": "Integrate with your simulation output. See SKILL.md for extraction patterns."
    }
    
    # Format and save
    if args.format == "markdown":
        content = format_markdown(results, title=f"{args.activity_type.title()} Results")
    elif args.format == "json":
        content = format_json(results)
    else:
        content = "CSV format not yet implemented"
    
    output_path = Path(args.output)
    output_path.write_text(content)
    print(f"📄 Saved to: {output_path}")


if __name__ == "__main__":
    main()
