# Spec: Expressive Appliance Dot Matrix Eyes & Emotions

**Author:** Dale Rogers & Crush  
**Date:** 2026-08-22  
**Status:** Approved  
**Supersedes:** `docs/06-specs/2026-08-16-tars-face-persona-design.md` (eye & UI visual system only; TARS personality & humor/honesty dials remain as underlying behavioral engine)

---

## 1. Overview & Vision

This specification defines the complete refactor of SMOLCASE's visual expression system:
* **Aesthetic Foundation:** A full-screen physical appliance LED/VFD dot matrix display (monochrome phosphor green/amber/ice-white, unlit background ghost dots, subtle ambient breathing).
* **Eye Style & Anatomy:** Cozmo-inspired expressive kawaii robot eyes (solid glowing dot clusters, continuous eyelid curvature and deformation, squash & stretch, **no pupils, no glint**).
* **Screen Topography:** Eye apertures are strictly bounded to the **upper half** (above the horizontal centreline) to give ample space for 2D gaze roaming. Only the soft optical dot glow/bloom is permitted to bleed past the centreline.
* **Expression & Reaction System:** Internal honesty and humour dials drive emotive variations (lid slants, deadpan vs. bouncy reactions). Eyes can momentarily morph into vetted emotional reaction icons (e.g. hearts for care/fondness, radar/loading scan for thinking).
* **Appliance Telemetry:** No phone-style status bar or on-screen text readouts. Essential hardware state (battery, BLE/Wi-Fi connection) is rendered as subtle, minimalist appliance LED pips/bars at the very bottom edge of the matrix.

---

## 2. Visual Architecture & Layout

### 2.1 Full-Screen Dot Matrix Canvas
* **Grid Topology:** Uniform circular LED dot grid covering the entire Pixel 8 screen (e.g., $36 \times 64$ to $45 \times 80$ dots depending on dot pitch).
* **Dot Rendering:**
  * **Unlit / Ghost Dots:** Rendered at $3\% - 6\%$ alpha to define the physical retro hardware grid.
  * **Lit Dots:** Variable intensity ($0.0 \dots 1.0$) with radial bloom/halo for smooth anti-aliased shape edges.
  * **Ambient Breathing:** Low-frequency sine wave modulation ($0.08\text{ Hz} - 0.12\text{ Hz}$) subtly undulating unlit dot intensity ($2\% \dots 5\%$) across the lower matrix canvas.

### 2.2 Zonal Partitioning
```
+----------------------------------------------------+
|                                                    |
|            UPPER HALF: EYE ROAMING ZONE            |
|       (Left Eye Aperture)    (Right Eye Aperture)  |
|                                                    |
| - - - - - - - - -  CENTERLINE  - - - - - - - - - - |  <- Eye bodies clipped at centerline
|                                                    |      (Soft bloom permitted past)
|            LOWER HALF: AMBIENT CANVAS              |
|          (Pure unlit dots + breathing)             |
|                                                    |
| [o o o o]                             [o o o o]    |  <- Subtle appliance LED telemetry
+----------------------------------------------------+
```

1. **Upper Half (Eye Roaming Zone, $Y \in [0.0, 0.5]$):**
   * Two solid expressive eye clusters.
   * Full 2D gaze tracking (X and Y displacement) within this top bounding box.
   * Lid deformation (top and bottom lid cutoffs, angular slant, curved arcs).
2. **Lower Half (Ambient Canvas, $Y \in [0.5, 1.0]$):**
   * Clean, unlit dot canvas providing aesthetic weight and balance.
   * Continuous ambient breathing pulse.
3. **Bottom Edge (Telemetry Pips, $Y \in [0.95, 1.0]$):**
   * Minimalist hardware indicator dots (Battery level pips on left, BLE connection pip on right).
   * Wake on change/low battery, otherwise dim.

---

## 3. Cozmo-Style Expressive Eye Engine

### 3.1 Procedural Signed Distance Field (SDF) Eye Model
Eyes are rendered without pupils or glint. Each eye is defined parametrically:
* **Position:** Center $(C_x, C_y)$
* **Dimensions:** Width $W$, Height $H$, Corner Radius $R$ (default: rounded soft pill)
* **Rotation / Slant:** Angle $\theta$ (inward slant for determination/focus, outward slant for sadness/pleading)
* **Eyelid Sliders:**
  * Top Lid: Position $L_{top} \in [0, 1]$, Angle $\alpha_{top}$, Curvature $\kappa_{top}$
  * Bottom Lid: Position $L_{bottom} \in [0, 1]$, Angle $\alpha_{bottom}$, Curvature $\kappa_{bottom}$
* **Squash & Stretch:** Scale factors $(S_x, S_y)$ with volume conservation $(S_x \cdot S_y \approx 1)$.

### 3.2 Sub-Dot Rasterization
For each dot at screen coordinate $(p_x, p_y)$:
1. Transform point into local eye space.
2. Evaluate Signed Distance $d(p)$ against the deformed eye boundary.
3. Map distance to dot intensity $I = \text{clamp}(0.5 - d(p)/\text{dot\_radius}, 0.0, 1.0)$.
4. Add optical halo/bloom to adjacent neighbor dots.

---

## 4. Emotional Expressions & Reaction System

### 4.1 Emotion Catalog

| State / Mood | Eye Deformation & Motion | TARS Dial Influence |
|---|---|---|
| **Neutral / Deadpan** | Symmetrical rounded pills, steady gaze, slow rhythmic blinking. | High Honesty, Low Humor: Rigid symmetry. |
| **Sarcastic / Skeptical** | Asymmetrical eyelids: One eye squints flat, other eye raises with slight slant. | High Humor: Pronounced asymmetry on deadpan remarks. |
| **Happy / Delighted** | Upward curving lower crescents (`^ ^`), vertical bounce & squash. | High Humor: Faster spring bounce. |
| **Curious / Attentive** | Eyes widen (increased $H$), tilt slightly inward, saccade toward interest point. | Moderate/High Honesty. |
| **Sleepy / Drowsy** | Heavy top lids ($L_{top} \approx 0.6$), dim dot intensity, slow downward drift. | Universal idle decay. |
| **Alert / Surprised** | Tall expanded ovals, instantaneous widen without blink. | Reacts to sudden IMU acceleration / sound. |
| **Thinking / Processing**| Eyes morph into horizontal scanning pulse or circular radar sweep inside eye zone. | During LLM generation or Wi-Fi radar processing. |

### 4.2 Momentary Reaction Overlays (Vetted Emotes)
* **Heart (`♥`):** Eyes morph/dissolve into solid dot matrix hearts for 1.2–2.0s when expressing affection, gratitude, or praise, then morph smoothly back to neutral.
* **Strictly Excluded:** Generic Android emojis, thumbs-up, waving hands, cartoon character faces.

---

## 5. Interaction & Sensory Pipeline

1. **Touch Interaction:**
   * Direct screen taps/swipes trigger micro-reactions (e.g. gentle purr/squash animation on top screen, gaze follows finger).
2. **Accelerometer / IMU:**
   * Screen tilt and physical robot motion apply inertial lag to eye coordinates (spring-damper gaze lag).
   * Freefall / drop detection triggers alert/wide-eye state.
3. **Voice / Autonomous Gaze:**
   * Microphone activity animates subtle listening tension in eye lids.
   * Gaze wander engine keeps the robot feeling alive during conversation pauses.
4. **Settings Screen:**
   * Clean full-screen modal invoked via triple-tap or long-press; keeps the live robot face free of configuration UI.

---

## 6. Deprecation & Cleanup (What is Removed)

* ❌ **Removed:** `GlyphGrid.kt` and monospace ASCII/character font rendering.
* ❌ **Removed:** Monospace text stream, telemetry logs, and LLM transcript overlays on face.
* ❌ **Removed:** Internal pupil tracking and specular glints.
* ❌ **Removed:** Android-style top status bars and on-screen slider controls.
