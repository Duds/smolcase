#!/usr/bin/env python3
"""Live simulation runner — runs step-by-step and writes real-time state for the dashboard."""
import json
import sys
import time
import argparse
from pathlib import Path
from datetime import datetime

from tinytroupe.agent import TinyPerson
from tinytroupe.environment import TinyWorld
from tinytroupe.extraction import ResultsExtractor
from tinytroupe import control


def load_persona(path: Path) -> TinyPerson:
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
    mod.define("occupation", {"title": "Moderator", "description": "You facilitate group discussions neutrally."})
    mod.define("personality", {"traits": [
        "You are neutral and do not lead participants.",
        "You ask open-ended follow-ups.",
        "You ensure quieter participants speak up."
    ]})
    return mod


def _named_agent(name: str, role_title: str, description: str = None) -> TinyPerson:
    """Create a lightweight persona from just a name + role (for debate/interview configs)."""
    tp = TinyPerson(name)
    tp.define("occupation", {"title": role_title,
                             "description": description or f"You participate as the {role_title}."})
    return tp


def build_agents(cfg: dict):
    """Return (agents, facilitator) from any activity config shape.

    Supports: personas list (focus_group/brainstorm/interview/assessment),
    pro/con/judge (debate), and facilitator/interviewer/moderator roles.
    """
    participants = [load_persona(Path(p)) for p in cfg.get("personas", [])]
    if cfg.get("pro_name"):
        participants.append(_named_agent(cfg["pro_name"], "Debater (Pro)",
                                         "You argue in favor of the motion with structured reasoning."))
    if cfg.get("con_name"):
        participants.append(_named_agent(cfg["con_name"], "Debater (Con)",
                                         "You argue against the motion with structured reasoning."))

    if cfg.get("moderator_name"):
        facilitator = create_moderator(cfg["moderator_name"])
    elif cfg.get("facilitator_name"):
        facilitator = _named_agent(cfg["facilitator_name"], "Facilitator",
                                   "You facilitate the session neutrally and keep it moving.")
    elif cfg.get("interviewer_name"):
        facilitator = _named_agent(cfg["interviewer_name"], "Interviewer",
                                   "You conduct the interview, asking open-ended follow-up questions.")
    elif cfg.get("judge_name"):
        facilitator = _named_agent(cfg["judge_name"], "Judge",
                                   "You moderate the debate, keep order, and evaluate both sides.")
    else:
        facilitator = create_moderator("Maya")

    # De-duplicate by name, preserving order
    seen = set()
    agents = []
    for a in participants + [facilitator]:
        if a.name not in seen:
            seen.add(a.name)
            agents.append(a)
    return agents, facilitator


def write_live_state(path: Path, state: dict):
    """Atomic write: temp file then rename to avoid half-written reads."""
    tmp = path.with_suffix(".tmp")
    tmp.write_text(json.dumps(state, indent=2, default=str))
    tmp.rename(path)


def extract_messages(world):
    """Extract recent conversation messages from the world or agents."""
    msgs = []
    
    # Try world._current_messages
    if hasattr(world, '_current_messages'):
        raw = world._current_messages
        for m in raw[-20:]:
            if isinstance(m, dict):
                sender = m.get('sender', m.get('from', 'Unknown'))
                content = m.get('content', m.get('message', m.get('text', '')))
                if content and len(content) > 10:
                    msgs.append({"agent": sender, "text": content[:500]})
    
    # Try world.messages
    if not msgs and hasattr(world, 'messages'):
        raw = world.messages
        for m in raw[-20:]:
            if isinstance(m, dict):
                sender = m.get('sender', m.get('from', 'Unknown'))
                content = m.get('content', m.get('message', m.get('text', '')))
                if content and len(content) > 10:
                    msgs.append({"agent": sender, "text": content[:500]})
    
    # Try agents' current messages
    if not msgs and hasattr(world, 'agents'):
        for agent in world.agents:
            if hasattr(agent, '_current_messages'):
                for m in agent._current_messages[-5:]:
                    if isinstance(m, dict):
                        sender = m.get('sender', agent.name)
                        content = m.get('content', m.get('message', m.get('text', '')))
                        if content and len(content) > 10:
                            msgs.append({"agent": sender, "text": content[:500]})
            elif hasattr(agent, 'messages'):
                for m in agent.messages[-5:]:
                    if isinstance(m, dict):
                        sender = m.get('sender', agent.name)
                        content = m.get('content', m.get('message', m.get('text', '')))
                        if content and len(content) > 10:
                            msgs.append({"agent": sender, "text": content[:500]})
    
    # Deduplicate while preserving order
    seen = set()
    unique = []
    for m in msgs:
        key = (m.get('agent', ''), m.get('text', '')[:100])
        if key not in seen:
            seen.add(key)
            unique.append(m)
    
    return unique
    """Extract recent conversation messages from the world."""
    msgs = []
    env = world
    if hasattr(env, '_current_messages'):
        raw = env._current_messages
        for m in raw[-20:]:
            if isinstance(m, dict):
                sender = m.get('sender', 'Unknown')
                content = m.get('content', '')
                if content and len(content) > 10:
                    msgs.append({"agent": sender, "text": content[:500]})
            elif isinstance(m, str):
                msgs.append({"agent": "Agent", "text": m[:500]})
    return msgs


def determine_agent_status(world, agents, last_speaker=None):
    statuses = {}
    for a in agents:
        statuses[a.name] = "idle"
    if last_speaker and last_speaker in statuses:
        statuses[last_speaker] = "speaking"
    return statuses


def run_live(config_path: Path, widget_workspace: Path, output_dir: Path):
    cfg = json.loads(config_path.read_text())
    activity = cfg.get("activity_type", "focus_group")
    name = cfg.get("name", "simulation")
    steps = cfg.get("steps", 15)
    
    agents, facilitator = build_agents(cfg)

    world = TinyWorld(cfg.get("world_name", "Focus Group Studio"), agents)
    world.make_everyone_accessible()
    facilitator.listen(cfg["stimulus"])
    
    widget_workspace.mkdir(parents=True, exist_ok=True)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    live_state_path = widget_workspace / "live_state.json"
    cache_path = output_dir / f"{name}_cache.json"
    
    # Write initial state
    initial_state = {
        "status": "running",
        "activity_type": activity,
        "simulation_name": name,
        "step": 0,
        "total_steps": steps,
        "agents": [{"name": a.name, "role": _get_role(a), "status": "idle", "initials": _initials(a.name), "color": _agent_color(a.name)} for a in agents],
        "messages": [],
        "sentiment": 0.0,
        "sentiment_by_agent": {a.name: 0.0 for a in agents},
        "turns": 0,
        "elapsed_seconds": 0,
        "timestamp": datetime.now().isoformat(),
    }
    write_live_state(live_state_path, initial_state)
    print(f"[Live] Dashboard state written to {live_state_path}")
    
    start_time = time.time()
    control.begin(str(cache_path))
    
    all_messages = []
    last_speaker = None
    
    for step in range(1, steps + 1):
        print(f"[Live] Step {step}/{steps}...", flush=True)
        
        world.run(1)
        
        step_messages = extract_messages(world)
        for m in step_messages:
            if m not in all_messages:
                all_messages.append(m)
                last_speaker = m["agent"]
        
        statuses = determine_agent_status(world, agents, last_speaker)
        sentiment = _estimate_sentiment(all_messages)
        sentiment_by_agent = {a.name: _estimate_sentiment(all_messages, a.name) for a in agents}
        elapsed = int(time.time() - start_time)
        
        state = {
            "status": "running" if step < steps else "complete",
            "activity_type": activity,
            "simulation_name": name,
            "step": step,
            "total_steps": steps,
            "agents": [
                {
                    "name": a.name,
                    "role": _get_role(a),
                    "status": statuses.get(a.name, "idle"),
                    "initials": _initials(a.name),
                    "color": _agent_color(a.name),
                }
                for a in agents
            ],
            "messages": all_messages[-15:],
            "sentiment": sentiment,
            "sentiment_by_agent": sentiment_by_agent,
            "turns": len(all_messages),
            "elapsed_seconds": elapsed,
            "timestamp": datetime.now().isoformat(),
        }
        
        write_live_state(live_state_path, state)
        time.sleep(0.5)
    
    control.end()
    
    print("[Live] Extracting final results...")
    extractor = ResultsExtractor()
    results = {
        "simulation_name": name,
        "activity_type": activity,
        "status": "complete",
        "agents": [a.name for a in agents],
        "steps_run": steps,
        "timestamp": datetime.now().isoformat(),
    }
    
    try:
        results["sentiment_text"] = extractor.extract_results_from_world(
            world, extraction_objective="Summarize overall sentiment as one word: positive, mixed, or negative"
        )
    except Exception as e:
        results["sentiment_text"] = f"extraction error: {e}"
    
    results_path = output_dir / f"{name}_results.json"
    results_path.write_text(json.dumps(results, indent=2, default=str))
    
    final_state = {
        "status": "complete",
        "activity_type": activity,
        "simulation_name": name,
        "step": steps,
        "total_steps": steps,
        "agents": [{"name": a.name, "role": _get_role(a), "status": "idle", "initials": _initials(a.name), "color": _agent_color(a.name)} for a in agents],
        "messages": all_messages[-20:],
        "sentiment": sentiment,
        "sentiment_by_agent": sentiment_by_agent,
        "turns": len(all_messages),
        "elapsed_seconds": int(time.time() - start_time),
        "timestamp": datetime.now().isoformat(),
        "results_file": str(results_path),
    }
    write_live_state(live_state_path, final_state)
    
    print(f"[Live] Complete! Results: {results_path}")
    print(f"[Live] Final state: {live_state_path}")
    return results


def _get_role(agent: TinyPerson) -> str:
    """Get role from agent, trying various internal attributes."""
    for attr in ["_configuration", "_config", "configuration", "config", "_persona", "persona", "_defined_attributes"]:
        if hasattr(agent, attr):
            data = getattr(agent, attr)
            if isinstance(data, dict):
                occ = data.get("occupation", {})
                if isinstance(occ, dict):
                    return occ.get("title", "Participant")
    return "Participant"


def _initials(name: str) -> str:
    parts = name.split()
    if len(parts) >= 2:
        return (parts[0][0] + parts[-1][0]).upper()
    return name[:2].upper()


def _agent_color(name: str) -> str:
    colors = {
        "Jordan": "linear-gradient(135deg,#a78bfa,#f472b6)",
        "Avery": "linear-gradient(135deg,#38bdf8,#4ade80)",
        "Elena": "linear-gradient(135deg,#fbbf24,#f87171)",
        "Maya": "linear-gradient(135deg,#94a3b8,#64748b)",
        "Marcus": "linear-gradient(135deg,#60a5fa,#a78bfa)",
        "Thomas": "linear-gradient(135deg,#f472b6,#fb923c)",
        "Lars": "linear-gradient(135deg,#4ade80,#38bdf8)",
        "Ana": "linear-gradient(135deg,#fbbf24,#a78bfa)",
        "Robert": "linear-gradient(135deg,#94a3b8,#38bdf8)",
        "Ingrid": "linear-gradient(135deg,#f87171,#fbbf24)",
    }
    for key, color in colors.items():
        if key in name:
            return color
    return "linear-gradient(135deg,#94a3b8,#64748b)"


def _estimate_sentiment(messages, agent_name: str = None):
    """Keyword-heuristic sentiment in [-1, 1]. Pass agent_name to score one persona."""
    if agent_name:
        needle = agent_name.lower()
        messages = [m for m in messages if needle in str(m.get("agent", "")).lower()]
    if not messages:
        return 0.0
    text = " ".join(m.get("text", "") for m in messages).lower()
    positive = ["great", "good", "excellent", "love", "like", "useful", "valuable", "agree", "yes", "benefit", "opportunity"]
    negative = ["bad", "worried", "concern", "risk", "danger", "problem", "failure", "wrong", "don't", "not", "harm", "negative"]
    p = sum(1 for w in positive if w in text)
    n = sum(1 for w in negative if w in text)
    total = p + n
    if total == 0:
        return 0.0
    return (p - n) / max(total, 5)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("config", help="Path to JSON config file")
    parser.add_argument("--widget-workspace", required=True, help="Path to widget workspace directory")
    parser.add_argument("--output", "-o", default="./output", help="Output directory")
    args = parser.parse_args()
    
    run_live(Path(args.config), Path(args.widget_workspace), Path(args.output))
