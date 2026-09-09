# Plan: Settings UI Styling Match (2026-09-10)

- **Task:** 20260910-003
- **Spec:** [[docs/06-specs/2026-09-10-settings-ui-prototype-styling]]
- **Related:** [[_tasks/20260910-003-settings-compose-styling]]

## Steps

1. Extract design tokens from the prototype (`docs/06-specs/assets/2026-09-10-settings-prototype/smolcase_settings/code.html` + `docs/06-specs/assets/2026-09-10-settings-prototype/cybernetic_companion_deck/DESIGN.md`): colors, radii, typography roles, glow spec.
2. Refactor `SettingsTheme.kt` into a Compose-first token object (panel, field, border, cyan, coral, dim, telemetry styles) plus a `GlowBox`/glow modifier helper.
3. Update `SettingsCompose.kt` components:
   - `Section` -> panel card with cyan header ribbon (icon + uppercase title, subsystem code right-aligned).
   - `Field` -> bordered OLED dark field with cyan focus border.
   - `Dial` -> slider with cyan track/thumb, recessed track color, telemetry readout.
   - `Toggle` -> Switch with cyan on-state colors.
   - `BackendPicker` -> segmented rail style, active segment cyan.
   - `LiveMirror`, `NamespaceBadges`, `Note` -> monospace telemetry styling.
4. Verify WCAG AA contrast is preserved (borders >= 4.1:1 behavior from task 20260831-009).
5. Run unit tests, commit.

## Acceptance

- Settings screen visually matches prototype panel/ribbon/telemetry style.
- Unit tests pass with zero failures.
- No business-logic changes.
