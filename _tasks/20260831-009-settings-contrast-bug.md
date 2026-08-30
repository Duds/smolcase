---
id: 20260831-009
title: "Settings EditText fields fail WCAG AA contrast"
type: bug
severity: 🟡 Medium
status: 🔴 Open
date: 2026-08-31
---

## Problem

Text input fields in Settings (API key, base URL, TTS voice ID, etc.) have
low-contrast boundaries and controls against the black canvas.

## Suspected causes

| Issue | Current value | Contrast on black | Verdict |
|-------|--------------|-------------------|---------|
| Bottom border line (`FIELD_BORDER_COLOR`) | `#555555` | 1.8:1 | ✗ Fails — serves as only field boundary indicator |
| Button background (`BUTTON_BG_COLOR`) | `#2A2A2A` | 1.3:1 | ✗ Fails — Save button nearly invisible |
| EditText caret (cursor) color | System default (white) | Depends on theme | May be invisible against certain field states |
| SeekBar track/thumb | System default Pixel 8 dark theme | Unknown | Likely too dim on black canvas |

## Acceptance Criteria

1. EditText bottom border is visible at ≥3:1 against black
2. Button background is visible at ≥3:1 against black
3. EditText caret/cursor is visible at ≥3:1 against the field background
4. SeekBar track has visible contrast against black canvas
5. All fixes tested with TalkBack enabled on-device