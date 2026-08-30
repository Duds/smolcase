---
id: 20260831-010
title: "Settings UX redesign spike — multi-page, tabs, styling"
type: spike
status: 🔵 Open
date: 2026-08-31
---

## Brief

The current Settings UI is a single scrollable programmatic layout with
collapsible sections. It works but has accumulated UX debt:
- Every setting on one screen = endless scrolling
- No visual hierarchy beyond section headers
- Programmatic layout (no XML) makes iteration slow
- Dark-on-black contrast issues (see [[_tasks/20260831-009-settings-contrast-bug]])
- No search, no grouping by use case
- The agent endpoint form (API key, URL, model) is the most critical page
  yet buried inside a collapsible section

## Brainstorm Areas

### 1. Navigation model

- Single-page with sections (current — no change)
- Multi-page with bottom navigation (e.g. LLM | Personality | Voice | System)
- Tabbed layout (horizontal scroll tabs)
- Hierarchy: top-level categories → sub-pages for details
- Drawer navigation for many sections

### 2. Multi-page breakdown (if adopted)

| Page | Contents |
|------|----------|
| **Thinking** | Backend selector (Agent/Nano/Rules), endpoint form, model, temp, max tokens |
| **Personality** | Humor dial, honesty dial, voice picker |
| **Voice & Audio** | TTS engine, cloud TTS config, voice selection, mute state |
| **Vision** | Cloud vision enable/disable, endpoint config |
| **Memory & Soul** | Reset to scratch, wakeup/re-onboarding, conversation log |
| **Sensors** | Pixel sensor toggles (proximity, light, step counter), morning report |
| **System** | Build info, debug mode, telemetry, about |

### 3. Visual redesign

- Switch from programmatic UI to Jetpack Compose or XML layouts
- If staying programmatic: extract a proper widget toolkit with consistent
  spacing, focus rings, error states
- Better section card styling (rounded corners, subtle background fills)
- Iconography for each section
- Proper focus ordering for keyboard/navigation

### 4. Contrast & accessibility fixes

- Tracked separately in [[_tasks/20260831-009-settings-contrast-bug]]
- But structural changes affect what needs fixing

### 5. API key management UX

- Masked input with show/hide toggle
- "Test connection" button that does a model list API call
- Status indicator (green/red dot) for configured endpoints
- Pre-built presets: OpenRouter, Together, Groq, Ollama (local)

### 6. Backend selector improvements

- Show model status inline (e.g. "Gemini Nano — downloading...")
- Detect and warn about unconfigured backends
- Fallback chain: if primary fails → try secondary
- Provider-specific help text when selected

## Approach

This is a spike/brainstorm — no code expected yet. Outcome should be:
1. Design direction recommendation (multi-page vs single-page, Compose vs XML)
2. Wireframe sketches or mockup descriptions for key screens
3. Implementation plan with estimated effort
4. Decision gate: adopt and implement, or iterate

## References

- `SettingsActivity.kt` — current single-page implementation
- `SettingsTheme.kt` — current styling constants
- `AgentEndpointForm.kt` — current agent config form
- `SettingsSection.kt` — current collapsible section widget