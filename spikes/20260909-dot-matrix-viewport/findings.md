# Findings

## Observation log

| Test | Result | Notes |
|---|---|---|
| Source renders offscreen only | Pass | Debug bypass toggle confirms the raw source renders correctly on its own. |
| Dot grid quantises the source | Pass | Coarse 16 x 28 grid gives 8 distinct dot runs per row at ~14px spacing, matching a 38px cell pitch. |
| Dots over eyes brighter than dots over sphere | Pass | Brightest viewport dot at (200, 332) matches the brightest raw source pixel at (208, 340). |
| Per-dot glow and halation respond to parameters | Pass | Raising glow and exposure increases lit pixel count from 3981 to 5507. |
| Ghost floor makes unlit dots faintly visible | Pass | Unlit regions are faintly lit, giving the black substrate a visible diode texture. |
| Grid matches configured columns and rows | Pass at coarse settings | Verified with 16 x 28. Default 38 x 68 is too fine to count reliably at this screenshot scale. |

## Verdict

The translation layer works. Rendering the source offscreen, quantising to per-dot luminance, and emitting each dot with its own glow produces a genuinely dot-aware image, which the earlier CSS mask could not.

Two observations worth carrying forward:

1. The source is very dark. Mean raw luminance is about 4/255, so exposure does most of the work. The sphere needs a brighter material or a stronger key light before the dot matrix reads well.
2. The measured dot diameter is smaller than the geometric prediction. At 16 columns the dots measure about 14px wide, where the cell pitch is about 38px. The emitter radius model needs checking against the intended aperture geometry.

## Lighting stack

The source was too dark for the dot matrix to read. Root cause was inverse-square falloff: the key light sat 9.25 units from the sphere, so intensity 40 arrived as 0.47, then met a dark albedo.

Measured on the sphere region, eyes excluded:

| Config | Sphere mean | Pure black |
|---|---|---|
| Old: point light, decay 2 | 4.6 overall | most of the shadow side dead |
| Key 2.2, no falloff | 24.4 | 33.2% |
| Key 2.2, fill 0.9, ambient 0.2 | 37.8 | 3.4% |
| Key 2.2, fill 1.2, ambient 0.2 | 41.5 | 0% |
| Key 2.2, fill 0.9, ambient 0.2, lift 1.4 | 45.9 | 0% |

Findings:

- Removing falloff was the single biggest win. It is the root cause, not brightness.
- Fill at 0.9 removes almost all dead shadow. It had no effect until it was given a position, since it defaulted to the origin, just under the sphere.
- Ambient works as a floor but flattens. At 0.4 it lifted everything to a narrow 33 to 59 band and removed the form.
- Fog was rejected. It is depth attenuation, not scattering, and cannot produce diffraction.

## Decision to capture

Record whether this per-dot emitter model is the right basis for the Android implementation, and note the two follow-ups above.
