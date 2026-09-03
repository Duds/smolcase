# Settings & Cloud Intelligence Expansion — Technical Specification

## Overview

> **Related:** [[docs/06-specs/2026-08-17-gemma-backend-design]] (Gemma backend), [[docs/03-decisions/001-pixel8-brain\|ADR-001]] (sensor suite), [[AGENTS]] §5 (Android architecture)
> See [[TASKS]] for implementation tracking.

SMOLCASE currently has a minimal programmatic Settings screen with LLM backend
selection (RULES / NANO / KIMI / GEMMA), Kimi API configuration, humor/honesty
dials, and a voice picker. This spec expands Settings into a full creature
configuration hub: WCAG AA-compliant UI, agnostic agent endpoint support,
cloud TTS, cloud Vision, cloud AI reply generation, and Pixel sensor
configuration.

## Goals

- **Settings page meets WCAG AA** — contrast ratios, touch targets ≥ 48dp,
  focus indicators, screen-reader-friendly labels on all controls.
- **Agnostic agent endpoints** — any OpenAI-compatible inference provider
  (OpenRouter, Together, Groq, Perplexity, local vLLM) configurable via
  Settings: base URL, API key, model override — same pattern as the
  existing Kimi backend, generalised.
- **Cloud features** — cloud TTS (ElevenLabs-level quality), cloud Vision
  (front camera analysis), cloud AI reply generation (feed cloud TTS, free
  Gemma/Nano for other tasks, enable richer analysis).
- **Sensor awareness** — Pixel sensors (magnetometer, IMU, ambient light,
  temperature, barometer, proximity) surfaced as creature senses,
  configure which sensors are active.
- **Good-enough defaults** — creature works out of the box; Settings is
  for power users, not required for daily use.

## Non-goals

- **No firmware/body integration** — leg motion, servo control, chassis
  are scoped elsewhere.
- **No layout XML migration** — the programmatic LinearLayout approach stays;
  visual polish comes via a helper utility, not a full re-architecture.
- **No multi-locale i18n** — English only for this phase.
- **No OAuth/SSO** — API key input only (Bearer token).

## Requirements

### Functional

#### FR-1: Settings Screen UX (WCAG AA baseline)

- All text labels meet 4.5:1 contrast ratio against the black background
  (white text is 13.5:1; gray hints need #A0A0A0 or lighter).
- All touch targets (SeekBars, RadioButtons, Buttons, EditTexts) are
  minimum 48dp in touchable height.
- Every control has a matching `contentDescription` for TalkBack.
- Sections are visually separated by a `View` divider (thin gray line).
- A new `CreatureSettings` model class aggregates all settings
  (LLM, cloud, dials, sensor, UI) into one `SharedPreferences` namespace
  for atomic save/load.

#### FR-2: Agnostic Agent Endpoint

- Replace the Kimi-specific backend config with a generalised
  `AgentBackend` with fields:
  - **Provider label** (human-readable, e.g. "OpenRouter", "Together")
  - **Base URL** — defaults to OpenRouter (`https://openrouter.ai/api/v1`)
  - **API key** — masked input (`sk-…`)
  - **Model override** — free text (e.g. `mistralai/mixtral-8x22b-instruct`)
  - **Max tokens** — slider (64..1024, default 200)
  - **Temperature** — seekbar 0..200 (displayed as 0.00..2.00, default 0.80)
- The existing Kimi backend (`KimiBackend.kt`) is refactored into a
  generic `AgentBackend` implementing `LlmBackend` — the same
  HTTP POST JSON contract works with any OpenAI-compatible endpoint.
- `LlmSettings.Backend` adds an `AGENT` value (renamed from KIMI).
- Existing persisted `kimi_*` SharedPreferences keys are migrated
  to `agent_*` keys on first read (backward-compatible migration).

#### FR-3: Cloud TTS

- Toggle to enable cloud TTS routing (default: off = local Android TTS).
- Configuration fields:
  - **Cloud TTS provider** — free text label (default "ElevenLabs", but
    any OpenAI-compatible TTS endpoint supported via configurable URL)
  - **Base URL** — default `https://api.elevenlabs.io/v1`
  - **API key** — masked input
  - **Voice ID** — free text (ElevenLabs voice UUID, or custom path)
  - **Stability** — seekbar 0..100 (default 50) [ElevenLabs-specific]
  - **Similarity** — seekbar 0..100 (default 75) [ElevenLabs-specific]
- `CloudTtsBackend` class: fetches audio binary, plays via `MediaPlayer`.
- Falls back to local TTS on network failure.
- `CreatureVoice` gains an optional `CloudTtsBackend` — if configured,
  it tries cloud first, falls back to local.

#### FR-4: Cloud Vision

- Toggle to enable cloud Vision analysis (default: off).
- Configuration fields:
  - **Vision endpoint** — free text (default OpenAI-compatible Vision endpoint)
  - **API key** — masked input
  - **Vision model** — free text (default `gpt-4o` or `llava` equivalent)
- `CloudVision` class: captures a single frame from CameraX,
  base64-encodes it, sends to endpoint, returns text description.
- Camera permission already granted — no new permissions needed.
- Vision results flow to `ConversationEngine` as additional context
  ("What do you see?" / "Describe this object").
- Rate limiting: max 1 analysis per 10 seconds to avoid token waste.

#### FR-5: Cloud AI Reply Generation

- Independent toggle to route reply generation through a cloud LLM
  endpoint (separate from the thinking backend).
- Configuration shares the same agent endpoint fields but stores
  a separate provider selection (or same as thinking backend).
- `ConversationEngine` logic: if cloud reply generation is enabled,
  use the cloud endpoint for `reply()` and free the on-device backend
  (Gemma/Nano) for background tasks (summarisation, memory compaction).
- If cloud reply generation is *not* enabled, the existing backend
  selection rules apply.

#### FR-6: Sensor Configuration

- Toggle group for each available Pixel sensor:
  - **Magnetometer** (compass heading — which way the creature faces)
  - **IMU** (accelerometer + gyro — was the creature picked up/shaken)
  - **Ambient light** (is the desk lit / is it night)
  - **Barometer** (altitude / weather changes)
  - **Proximity** (something close — "is someone petting me")
  - **Ambient temperature** (is it hot/cold in the room)
- Each sensor has a label + short description of what the creature
  learns from it.
- Sensor status indicators in Settings showing live values (optional
  readout).
- `CreatureSenses` class: wraps Android `SensorManager`, polls at
  configurable intervals, stores latest readings in-memory, accessible
  by `ConversationEngine` for creature awareness.

### Non-functional

- **NFR-1 (Performance):** No sensor polling on the main thread.
  `CreatureSenses` uses a background `HandlerThread`.
- **NFR-2 (Privacy):** API keys are stored in `SharedPreferences`
  (`MODE_PRIVATE`), same as existing. Cloud Vision captures only when
  explicitly triggered — no continuous streaming to cloud.
- **NFR-3 (Offline resilience):** Cloud features degrade gracefully.
  Every cloud path has a local fallback (local TTS, "I can't see
  anything right now", fallback backend).
- **NFR-4 (Settings state):** "Save" button persists all sections
  atomically. Unsaved changes show a discard warning if back is pressed.

## Design notes

### Settings code organisation

```
SettingsActivity.kt          — entry point, scroll container, save/load orchestrator
ui/SettingsTheme.kt          — WCAG helper functions (contrast-safe colors, spacing consts)
ui/SettingsSection.kt        — collapsible section component
ui/AgentEndpointForm.kt      — reusable endpoint config (base URL, key, model, params)
ui/SensorToggleGroup.kt      — sensor list with live readout
llm/AgentBackend.kt          — generic OpenAI-compatible backend (replaces KimiBackend)
llm/LlmSettings.kt           — expanded with AGENT, cloud TTS/Vision keys
cloud/CloudTtsBackend.kt     — cloud TTS fetch + play
cloud/CloudVision.kt         — single-shot vision analysis
sensors/CreatureSenses.kt    — sensor manager wrapper
sensors/SensorConfig.kt      — which sensors active + poll rates
```

### Agent endpoint abstraction

The existing `KimiBackend` already sends the right OpenAI-compatible
POST body. The refactor is:
1. Rename `KimiBackend` → `AgentBackend`.
2. Read base URL, key, model from `agent_*` keys instead of `kimi_*`.
3. Add `temperature` and `maxTokens` to the request body.
4. `LlmSettings.Backend.KIMI` → `LlmSettings.Backend.AGENT` (with
   backwards-compat migration reading old `kimi_*` keys).

### Settings migration path

| Old key                | New key         | Notes                                    |
|------------------------|-----------------|------------------------------------------|
| `kimi_base_url`        | `agent_base_url`| Read old if new missing, then write new  |
| `kimi_api_key`         | `agent_api_key` | Same migration logic                     |
| `kimi_model`           | `agent_model`   | Same migration logic                     |
| `backend` value `KIMI` | `AGENT`         | Read old enum name, convert on first load|

### WCAG AA implementation

- Helper functions in `SettingsTheme.kt` provide standardised colors:
  - `LABEL_COLOR = Color.parseColor("#FFFFFF")` (13.5:1 on black)
  - `HINT_COLOR = Color.parseColor("#A0A0A0")` (6.6:1 on black)
  - `VALUE_COLOR = Color.parseColor("#E0E0E0")` (9.3:1 on black)
  - `DIVIDER_COLOR = Color.parseColor("#333333")` (1.75:1 — decorative only)
  - `FOCUS_COLOR = Color.parseColor("#4FC3F7")` (4.8:1 on black when shown)
  - `ERROR_COLOR = Color.parseColor("#EF5350")` (4.9:1 on black)
- All `TextView`, `EditText`, `SeekBar`, `Button`, `RadioButton` set
  `contentDescription` from their label text.
- `SeekBar` uses 48dp min height via layout params.
- Section dividers: `View` with `DIVIDER_COLOR`, height 1dp, margin 16dp.

## Open questions

- [ ] ElevenLabs vs generic OpenAI-compatible TTS — spec assumes URL-configurable
      to handle either. Confirm ElevenLabs TTS API returns raw audio or JSON wrapper.
- [ ] Cloud Vision: OpenAI-compatible Vision endpoints vary in how they accept
      base64 images. Spec assumes `data:image/jpeg;base64,...` content parts.
      May need provider-specific adapters if formats differ.
- [ ] Sensor polling rates: what default interval for each sensor?
      Magnetometer ~500ms, IMU ~100ms, ambient ~2000ms, proximity ~500ms.
- [ ] Should cloud AI reply generation default to the same provider as the
      thinking backend, or be independently configured? Spec assumes independent
      for flexibility, but same-provider shortcut would simplify UX.

## Definition of done

- [ ] Build passes, all existing tests green.
- [ ] Spec written to `docs/06-specs/`.
- [ ] Implementation plan written to `docs/07-plans/`.
- [ ] SettingsActivity UI renders with WCAG AA spacing, contrast, labels.
- [ ] AgentBackend works with OpenRouter (basic chat completion).
- [ ] Old `kimi_*` preferences migrate to `agent_*` without data loss.
- [ ] Cloud TTS toggle + config renders, cloud TTS plays through speaker.
- [ ] Cloud Vision toggle + config renders, single-shot analysis returns a description.
- [ ] Cloud reply generation toggle + config routes `ConversationEngine.reply()` through it.
- [ ] Sensor toggles render, active sensors poll in background, readings accessible.
- [ ] "Save" persists all sections; back button warns on unsaved changes.
- [ ] All cloud features gracefully fall back when offline.