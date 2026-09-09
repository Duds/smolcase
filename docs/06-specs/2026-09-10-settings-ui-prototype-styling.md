# Spec: Settings UI Styling Match (Cybernetic Companion Deck)

- **Date:** 2026-09-10
- **Status:** Approved
- **Task:** 20260910-003
- **Related:** [[_tasks/20260910-003-settings-compose-styling]], [[docs/07-plans/2026-09-10-settings-ui-styling]], [[AGENTS]] §6.1, [[docs/06-specs/2026-08-29-settings-expansion-design]]

## Goal

Style the Compose settings screen (SettingsCompose.kt + SettingsTheme.kt) to match the "Cybernetic Companion Deck" prototype (extracted to `docs/06-specs/assets/2026-09-10-settings-prototype/`). The prototype styling is: panel cards with cyan header ribbons, monospace telemetry labels, bordered fields, glow accents, and a dark OLED palette.

## Acceptance Criteria (BDD)

- **Given** the settings screen opens, **when** it renders, **then** each section renders as a panel card (background `#0E1420` panel body, 1px border `#1E2A3E`, corner radius) with a cyan header ribbon (uppercase subsystem label + code on the right).
- **Given** a text input field, **when** it renders, **then** it uses an OLED dark field background (`#05070B`) with a 1px border (`#1E2A3E`); **when** focused, **then** the border becomes active cyan (`#00F0FF`).
- **Given** a slider/dial, **when** rendered, **then** the active track and thumb are cyan (`#00F0FF`) with a recessed track background (`#31353E`).
- **Given** a toggle, **when** on, **then** the thumb/track are cyan with a subtle glow; **when** off, **then** dark slate with dim border.
- **Given** telemetry labels, **when** rendered, **then** they are uppercase monospace (`JetBrains Mono` fallback `Monospace`) with cyan or dim color per role.
- **Given** the palette, **then** contrast meets WCAG AA as established in task 20260831-009 (no regressions).
- **Given** the full build, **when** unit tests run, **then** the suite passes with zero failures.

## Out of scope

- Changing business logic, persistence keys, or draft/save flow.
- New backend fields beyond what already exists in SettingsDraft.

## Related

- [[docs/06-specs/2026-08-29-settings-expansion-design]] (WCAG AA baseline)
- [[_tasks/20260910-003-settings-compose-styling]]
- [[docs/07-plans/2026-09-10-settings-ui-styling]]
