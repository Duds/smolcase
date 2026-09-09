# Findings

Related: [[spikes/20260909-face-expressions/README]], [[_tasks/20260909-003-face-expressions]].

## Expression-table iteration

The user-supplied table includes mouths, cheek marks, tears, sweat and head outlines. This iteration adapts its eye cues only. The lower three rows also suggest tilted variants, implemented as one shared face-tilt parameter rather than duplicate presets.

### Sphere mapping

The eye masks now also render onto the sphere model: a front hemisphere, lit from below-left, with the pattern fixed to the surface by applying the inverse gaze rotation per pixel. Gaze is mouse or WASD plus yaw/pitch range. This is an analytic shader projection, not the face spike's 3D mesh, and the face spike is untouched.

Verified: three presets render differently on the sphere, moving the pointer left versus right changes the render, and the lower-screen interior stays black. See [sphere heart](evidence/sphere-heart-raw.png), [sphere dots](evidence/sphere-neutral-dots.png) and the gaze pair.

Not verified: how well each preset reads on a curved, foreshortened surface. Sweep the pointer across the canvas and judge which survive at the silhouette.

### Changed

- Eight presets expanded to 16.
- HAPPY and SLEEPING now use rounded closed-eye curves, rather than smaller open ovals.
- LAUGHING uses mirrored chevrons. HEART uses filled silhouettes.
- ANGRY and SAD use opposite top-lid slopes. SHOCKED uses taller open eyes.
- EMBARRASSED, SMUG and EXHAUSTED add gentler arches, lowered lids and nearly flat strokes.
- Intensity zero now removes size scaling as well as lid and shape changes. Previously width/height multipliers ignored intensity.
- Tilt, shape weights and stroke settings are captured with the other parameters.
- Source pixels are clipped to the upper half; the existing emitter pass is unchanged.

### Verification

`test-expressions.cjs` uses Playwright through the page's controls. Blinking and micro-saccades are disabled, blend speed is set to 12, and transitions settle before comparisons.

| Check | Result |
|---|---|
| Preset buttons and selected state | 16 present and selected state updates |
| Raw captures | 16 different rendered images |
| Dot-matrix captures at 38 x 68 | 16 different rendered images |
| Intensity zero | All 16 equal the same neutral raw image |
| Capture/restore | Tilted HEART restores pixel-identically |
| Save PNG | Download received with the expression in its filename |
| Browser/shader errors | None captured |
| Source lower-screen interior | Black in 16 preset captures, the tilted-heart capture and the sphere captures (Python pixel check, excluding the CSS frame) |
| Sphere mapping | Three presets differ on the sphere; pointer left versus right changes the render |

Evidence: [raw contact sheet](evidence/contact-raw.png), [dot contact sheet](evidence/contact-dots.png), [test results](evidence/results.json).

### Visual observations, not recognition results

- Arches, chevrons, heart silhouettes and opposing lid slopes remain visible in the dot contact sheet.
- The shared viewport softens and reduces the detail of the source masks. Happy versus embarrassed, or smug versus drowsy, may still be confused.
- Testing different pixels does **not** establish recognisable emotions. No human recognition study has been run.
- Defaults retain the existing humor/honesty modifiers, so even nominally symmetric presets are somewhat asymmetric. These are experimental visual rules, not Android formula parity.
- No claims of real LED optical fidelity are made by this iteration. The shared pass still uses its existing five-tap approximation and cell-local emitter effect.

## Earlier sizing experiment

Earlier work increased base half-width/half-height from 0.045/0.032 to 0.065/0.046, approximately 11 x 6 dot cells for the larger eye at neutral before current modifiers. That is retained as a starting point, not a validated optimum. Earlier mood-area tables measured the previous shape implementation and do not establish the readability of these new presets.

## Next decision

Dale should compare the raw and dot sheets, identify which intended expressions actually read correctly, and capture the preferred parameter sets. Only then select presets for on-sphere testing or Android implementation.
