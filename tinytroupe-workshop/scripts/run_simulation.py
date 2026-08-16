#!/usr/bin/env python3
"""
TinyTroupe Simulation Runner
Reads a JSON config file and executes a multi-agent simulation.

Usage:
    python run_simulation.py --config workshop.json [--output results.json]

Config file schema:
{
    "activity_type": "workshop",      // workshop | focus_group | assessment | brainstorm | debate | interview
    "name": "Design Sprint",
    "steps": 25,
    "agents": [
        {
            "name": "Alex",
            "spec_file": "./personas/facilitator.agent.json",  // OR define inline below
            "role": "facilitator",
            "seed_message": "Lead a design sprint on onboarding redesign"
        }
    ],
    "environment": {
        "type": "TinyWorld",
        "name": "Workshop Room"
    },
    "interventions": [
        {
            "at_step": 10,
            "broadcast": "Time to switch from divergent to convergent thinking."
        }
    ],
    "cache_file": "workshop.cache.json"
}
"""

import argparse
import json
import sys
from pathlib import Path

# Ensure tinytroupe is available
try:
    from tinytroupe.agent import TinyPerson
    from tinytroupe.environment import TinyWorld
    from tinytroupe import control
except ImportError:
    print("ERROR: TinyTroupe not installed. Run:")
    print("  pip install git+https://github.com/microsoft/TinyTroupe.git@main")
    sys.exit(1)


def load_agent_from_config(agent_config: dict) -> TinyPerson:
    """Create a TinyPerson from config dict or spec file."""
    if "spec_file" in agent_config:
        spec_path = Path(agent_config["spec_file"])
        if not spec_path.exists():
            raise FileNotFoundError(f"Agent spec file not found: {spec_path}")
        person = TinyPerson.load_specification(str(spec_path))
    else:
        person = TinyPerson(agent_config["name"])
        
        # Apply inline definitions
        for key in ["age", "gender", "nationality", "residence", "education",
                    "occupation", "long_term_goals", "style", "personality",
                    "preferences", "beliefs", "behaviors"]:
            if key in agent_config:
                person.define(key, agent_config[key])
    
    return person


def build_environment(env_config: dict, agents: list) -> TinyWorld:
    """Create a TinyWorld from config."""
    env_type = env_config.get("type", "TinyWorld")
    name = env_config.get("name", "Simulation Environment")
    
    if env_type == "TinyWorld":
        world = TinyWorld(name, agents)
    else:
        # Could extend with custom environment classes
        world = TinyWorld(name, agents)
    
    world.make_everyone_accessible()
    return world


def apply_interventions(world: TinyWorld, interventions: list):
    """Apply interventions from config."""
    for interv in interventions:
        step = interv.get("at_step")
        message = interv.get("broadcast", "")
        
        if step is not None and message:
            # Simple step-based intervention
            from tinytroupe.intervention import Intervention
            intervention = Intervention(
                condition=lambda env, s=step: env.current_step == s,
                action=lambda env, msg=message: env.broadcast(msg)
            )
            world.add_intervention(intervention)


def run_simulation(config: dict) -> dict:
    """Execute simulation from config dict."""
    print(f"🎭 Starting simulation: {config.get('name', 'Untitled')}")
    print(f"   Activity type: {config.get('activity_type', 'generic')}")
    
    # Load agents
    agents = []
    seed_messages = {}
    for agent_conf in config.get("agents", []):
        agent = load_agent_from_config(agent_conf)
        agents.append(agent)
        if "seed_message" in agent_conf:
            seed_messages[agent] = agent_conf["seed_message"]
        print(f"   Loaded agent: {agent.name}")
    
    # Build environment
    env_conf = config.get("environment", {"type": "TinyWorld", "name": "Room"})
    world = build_environment(env_conf, agents)
    
    # Apply interventions
    apply_interventions(world, config.get("interventions", []))
    
    # Begin caching if configured
    cache_file = config.get("cache_file")
    if cache_file:
        control.begin(cache_file)
        print(f"   Caching enabled: {cache_file}")
    
    # Seed initial messages
    for agent, message in seed_messages.items():
        agent.listen(message)
        print(f"   Seeded {agent.name}: {message[:60]}...")
    
    # Run simulation
    steps = config.get("steps", 20)
    print(f"   Running {steps} steps...")
    world.run(steps)
    
    if cache_file:
        control.end()
    
    print("✅ Simulation complete")
    
    return {
        "world": world,
        "agents": agents,
        "config": config
    }


def main():
    parser = argparse.ArgumentParser(description="Run a TinyTroupe simulation from config")
    parser.add_argument("--config", required=True, help="Path to JSON config file")
    parser.add_argument("--output", help="Path to save raw simulation output (optional)")
    args = parser.parse_args()
    
    config_path = Path(args.config)
    if not config_path.exists():
        print(f"ERROR: Config file not found: {config_path}")
        sys.exit(1)
    
    with open(config_path) as f:
        config = json.load(f)
    
    result = run_simulation(config)
    
    # Optional: save raw output
    if args.output:
        # Note: This is a placeholder - actual serialization of TinyWorld is complex
        # In practice, you'd use ResultsExtractor and save structured data
        output = {
            "simulation_name": config.get("name"),
            "activity_type": config.get("activity_type"),
            "steps": config.get("steps"),
            "agent_names": [a.name for a in result["agents"]]
        }
        with open(args.output, "w") as f:
            json.dump(output, f, indent=2)
        print(f"📄 Saved summary to: {args.output}")


if __name__ == "__main__":
    main()
