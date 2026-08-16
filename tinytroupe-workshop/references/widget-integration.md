# Blueprint Widget & Canvas Integration

Connect TinyTroupe simulations to Kimi Dashboards (Canvases) for live visual monitoring and interactive results.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Widget Design Patterns](#widget-design-patterns)
3. [Automation Setup](#automation-setup)
4. [Live Simulation Dashboard](#live-simulation-dashboard)
5. [Results Widgets](#results-widgets)

---

## Architecture Overview

```
User Request → Kimi loads tinytroupe-workshop skill
                    ↓
        TinyTroupe simulation runs (Python/Bash)
                    ↓
        Results extracted as structured JSON artifact
                    ↓
        Automation artifact → Widget slot (Binding)
                    ↓
        Widget renders live on Dashboard (Canvas)
```

Key components:
- **Automation**: Background agent or code execution that runs the simulation
- **Widget**: HTML-based display surface on a Canvas
- **Binding**: Connects Automation artifact output to Widget data slot
- **Canvas**: Dashboard surface where Widgets are placed

---

## Widget Design Patterns

### Pattern 1: Static Results Display

For completed simulations, display structured results in a responsive widget.

**Artifact schema** (Automation `result.schema`):
```json
{
  "type": "object",
  "properties": {
    "activity_type": { "type": "string" },
    "simulation_name": { "type": "string" },
    "agents": { "type": "array", "items": { "type": "string" } },
    "key_findings": { "type": "array", "items": { "type": "string" } },
    "sentiment": { "type": "string", "enum": ["positive", "mixed", "negative"] },
    "top_ideas": { "type": "array" },
    "dissent": { "type": "array" },
    "cost_usd": { "type": "number" },
    "steps_run": { "type": "integer" }
  }
}
```

**Widget HTML** (`index.html`):
```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: system-ui, sans-serif; margin: 0; padding: 1rem; background: #0f0f0f; color: #e0e0e0; }
    .card { background: #1a1a1a; border-radius: 8px; padding: 1rem; margin-bottom: 0.75rem; }
    .badge { display: inline-block; padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.75rem; font-weight: 600; }
    .badge-workshop { background: #4a90d9; }
    .badge-focus { background: #d94a6a; }
    .badge-brainstorm { background: #4ad96e; }
    .sentiment-pos { color: #4ad96e; }
    .sentiment-mix { color: #d9c44a; }
    .sentiment-neg { color: #d94a6a; }
    .idea { border-left: 3px solid #4a90d9; padding-left: 0.75rem; margin: 0.5rem 0; }
    .dissent { border-left: 3px solid #d94a6a; padding-left: 0.75rem; margin: 0.5rem 0; color: #c0c0c0; }
    h2 { margin-top: 0; font-size: 1.1rem; }
    .meta { font-size: 0.8rem; color: #888; }
  </style>
</head>
<body>
  <div class="card">
    <span class="badge badge-workshop" id="type-badge">Workshop</span>
    <h2 id="sim-title">Loading...</h2>
    <div class="meta">Agents: <span id="agent-count">0</span> | Steps: <span id="step-count">0</span> | Cost: $<span id="cost">0.00</span></div>
  </div>
  
  <div class="card">
    <h2>Sentiment</h2>
    <div id="sentiment" class="sentiment-mix">Mixed</div>
  </div>
  
  <div class="card">
    <h2>Key Findings</h2>
    <div id="findings"></div>
  </div>
  
  <div class="card">
    <h2>Top Ideas</h2>
    <div id="ideas"></div>
  </div>
  
  <div class="card">
    <h2>Dissent / Unresolved</h2>
    <div id="dissent"></div>
  </div>

  <script>
    // Data injected by Automation via Binding
    function render(data) {
      document.getElementById('type-badge').textContent = data.activity_type;
      document.getElementById('type-badge').className = 'badge badge-' + data.activity_type.split('_')[0];
      document.getElementById('sim-title').textContent = data.simulation_name;
      document.getElementById('agent-count').textContent = data.agents.length;
      document.getElementById('step-count').textContent = data.steps_run;
      document.getElementById('cost').textContent = data.cost_usd.toFixed(2);
      
      const sent = document.getElementById('sentiment');
      sent.textContent = data.sentiment;
      sent.className = 'sentiment-' + data.sentiment.substring(0, 3);
      
      document.getElementById('findings').innerHTML = data.key_findings.map(f => `<p>• ${f}</p>`).join('');
      document.getElementById('ideas').innerHTML = data.top_ideas.map(i => `<div class="idea">${i}</div>`).join('');
      document.getElementById('dissent').innerHTML = data.dissent.map(d => `<div class="dissent">${d}</div>`).join('');
    }
    
    // If data is pre-injected by the Automation artifact
    if (typeof window.__widgetData !== 'undefined') {
      render(window.__widgetData);
    }
  </script>
</body>
</html>
```

---

### Pattern 2: Live Progress Widget

For long-running simulations, show real-time step count and agent activity.

**Automation** updates a status artifact periodically:
```python
# Inside the simulation loop
if step % 5 == 0:
    status = {
        "step": step,
        "total_steps": total,
        "active_agent": current_agent.name,
        "recent_topic": extract_topic(current_message),
        "cost_so_far": client().get_cost_stats()["total"]
    }
    # Emit as Automation artifact
```

**Widget**: Same HTML structure with a progress bar and live feed.

---

## Automation Setup

### Creating the Simulation Automation

```python
# Automation execution (background agent or code)
{
  "kind": "code",
  "runtime": "python",
  "entryRef": { "kind": "path", "base": "automation", "path": "run_workshop.py" }
}
```

The script must:
1. Run the TinyTroupe simulation
2. Extract results using `ResultsExtractor`
3. Format as JSON matching the Widget's `slots.main.schema`
4. Write to `automation-output` as artifact

### Creating the Widget

```python
# Widget with data slot for results
{
  "action": "create",
  "title": "Workshop Results",
  "type": "html",
  "slots": {
    "main": {
      "kind": "json",
      "schema": {
        "type": "object",
        "properties": {
          "activity_type": { "type": "string" },
          "simulation_name": { "type": "string" },
          "agents": { "type": "array", "items": { "type": "string" } },
          "key_findings": { "type": "array", "items": { "type": "string" } },
          "sentiment": { "type": "string" },
          "top_ideas": { "type": "array", "items": { "type": "string" } },
          "dissent": { "type": "array", "items": { "type": "string" } },
          "cost_usd": { "type": "number" },
          "steps_run": { "type": "integer" }
        }
      }
    }
  }
}
```

### Creating the Binding

```python
# Connect Automation output to Widget slot
{
  "action": "create",
  "kind": "automation_widget",
  "automationId": "<automation-id>",
  "widgetId": "<widget-id>"
}
```

### Placing on Canvas

```python
# Place the Widget on a Dashboard
{
  "action": "placeWidget",
  "canvasId": "<canvas-id>",
  "widgetId": "<widget-id>",
  "layout": { "mode": "grid", "x": 0, "y": 0, "w": 6, "h": 8 }
}
```

---

## Live Simulation Dashboard

A full Dashboard layout for simulation monitoring:

| Position | Widget | Purpose |
|----------|--------|---------|
| Top-left (6×4) | **Simulation Status** | Activity type, progress, cost |
| Top-right (6×4) | **Agent Panel** | List of agents, their roles, current activity |
| Middle-left (6×6) | **Key Findings** | Extracted insights (updates live) |
| Middle-right (6×6) | **Idea Cloud** | Top ideas with sentiment |
| Bottom (12×4) | **Transcript Feed** | Last N messages from the simulation |

Create the Canvas:
```python
{
  "action": "create",
  "title": "TinyTroupe Simulation",
  "purpose": "Live monitoring and results for multi-agent persona simulations"
}
```

---

## Results Widgets

### Focus Group Sentiment Widget

Displays aggregate sentiment across participants with representative quotes.

Schema:
```json
{
  "sentiment_distribution": { "positive": 3, "mixed": 2, "negative": 1 },
  "top_concerns": ["...", "..."],
  "top_appeals": ["...", "..."],
  "quotes": [{"speaker": "...", "text": "...", "sentiment": "..."}]
}
```

### Assessment Scoreboard Widget

Table of evaluator scores with heatmap coloring.

Schema:
```json
{
  "evaluators": ["Chen", "Aisha", "Elena"],
  "criteria": ["Innovation", "Feasibility", "Impact", "Clarity"],
  "scores": {
    "Chen": [4, 3, 5, 4],
    "Aisha": [5, 2, 5, 3],
    "Elena": [3, 4, 4, 5]
  },
  "verdicts": { "Chen": "Revise", "Aisha": "Approve", "Elena": "Approve" }
}
```

### Brainstorm Idea Cloud Widget

Tag-cloud or list view of all ideas with attribution.

Schema:
```json
{
  "ideas": [
    { "text": "Async standups", "by": "Jazz", "theme": "communication" },
    { "text": "Virtual coffee roulette", "by": "Blake", "theme": "social" }
  ],
  "themes": ["communication", "social", "recognition", "wellness"]
}
```

---

## Triggering from Conversation

When a user asks for a simulation, the skill can:

1. Run the simulation inline (for small, fast runs)
2. **Or** create an Automation + Widget + Binding + Canvas placement for durable, visual results

For the visual path:
```python
# In SKILL.md workflow, after extraction:
if user_wants_dashboard:
    # 1. Create Automation for the simulation run
    # 2. Create Widget with appropriate schema
    # 3. Create Binding (automation → widget)
    # 4. Create or update Canvas with the Widget placed
    # 5. Trigger the Automation run
    # 6. Return the Canvas link to the user
```

The user can then return to the Canvas to see updated results whenever the Automation reruns (e.g., on schedule or manually triggered).
