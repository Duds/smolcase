---
id: 20260910-002
title: "Multi-session routing for voice, vision, conversation and tooling"
type: task
status: open
priority: high
risk: medium
created: 2026-09-10
tags: [architecture, routing, voice, vision, conversation, tooling]
---

# Multi-Session Routing for Voice, Vision, Conversation and Tooling

## Question / Problem

Today each subsystem (VoiceEars, FaceTracker, ConversationEngine, AgentTool) runs as a
single implicit session bound to the app process. There is no explicit routing layer that
decides which subsystem an incoming event belongs to, or which active session it should be
delivered to. Adding multi-session support (e.g. wakeup/onboarding sessions, reset-to-scratch,
multiple concurrent conversation contexts, or background tool calls) requires a router.

## User Story

As a SMOLCASE user, when I start, pause, or switch an interaction session (voice, vision,
conversation, or tool call), each event is routed to the correct active session so that
responses, memory writes, and face/voice output stay consistent and never cross sessions.

## Acceptance Criteria (BDD)

- **Given** two logical sessions are open, **when** a voice utterance arrives, **then** it is
  routed to the active (foreground) session and the other session's state is untouched.
- **Given** a tool call completes while a TTS utterance is in flight, **when** the router
  serialises output, **then** voice and tool results never interleave or overlap on the speaker.
- **Given** a session ends, **when** any late events for it arrive, **then** they are dropped
  or logged, never delivered to a newer session.
- **Given** app restart, **when** sessions resume, **then** conversation context is restored
  per session from MemoryStore without cross-session bleed.
- Unit tests cover the router's event -> session mapping and output arbitration.

## Scope (subsystems)

1. **Voice** — SpeechRecognizer events and TTS output arbitration per session.
2. **Vision** — FaceTracker presence events attributed to the session that owns gaze.
3. **Conversation** — per-session Conversation/MemoryStore context, no cross-talk.
4. **Tooling** — AgentTool calls tagged with session ID; async results route back.

## Open Questions

- Is routing per-activity lifecycle or a long-lived app-scoped service?
- Does reset-to-scratch terminate all sessions or spawn a fresh one?
- Do background (non-visual) sessions get vision events at all?

## Related

- [[docs/06-specs/2026-08-30-reset-to-scratch-design]] (session teardown)
- [[docs/06-specs/2026-08-30-wakeup-onboarding-design]] (first-start session)
- `ConversationEngine.kt`, `VoiceEars.kt`, `AgentTool.kt`, `MemoryStore.kt`
