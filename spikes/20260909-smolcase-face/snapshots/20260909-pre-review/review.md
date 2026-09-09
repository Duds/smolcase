# Spike Review: Objective and Code

Scope reviewed: `spikes/20260909-smolcase-face/index.html`, `README.md`, `findings.md`.

## 1. Objective versus implementation

The stated objective has two parts:

1. Test whether the SMOLCASE face can be modelled as a centre-pinned sphere with animated eyes.
2. Treat the dot-matrix display as a later viewport translation layer.

### What holds up

- The sphere-plus-eyes model is clearly expressed and easy to reason about.
- Centre pinning and rotation-only gaze are implemented correctly.
- The dot-matrix substrate is implemented as a separate CSS layer, so the seam is visible.
- The prototype is genuinely throwaway and trivial to run.

### Where the objective drifts

The original instruction was explicit that the dot-matrix layer should **not** be part of the spike. The dot-matrix mask and blur layers were subsequently added. This is defensible as a viewport experiment, but it changes the spike's claim. The prototype now tests two things at once:

- The sphere gaze model.
- A specific CSS viewport implementation.

That makes any finding ambiguous. If the face looks wrong, we cannot tell whether the cause is the sphere model or the CSS mask. This is the single most important design problem in the current spike.

## 2. Critical code findings

### Blocker 1: Dot-matrix substrate is invisible by construction

The dot-matrix layer is:

```css
background: #000;
```

over a black scene, inside a black phone screen. The substrate is therefore indistinguishable from the background. The only observable effect is the apertures, which appear as holes punched in nothing.

The declared requirement was a black substrate with circular apertures acting as a clipping mask. The effect only becomes meaningful when there is non-black content behind it, such as the lit sphere.

### Blocker 2: Blur is not dot-aware

The blur layer applies `backdrop-filter: blur()` across the entire screen. The dot-matrix mask is applied afterwards as a separate layer.

Consequence: the effect is not a per-aperture optical blur. It is a uniform blur of the whole image, then a sparse sampling of that blur. That is not what an LED or VFD appliance does.

### Blocker 3: No luminance quantisation

The declared architecture treats the dot matrix as a viewport translation layer. A translation layer should quantise the source image into discrete emitters. There is currently no quantisation. Continuous shading is sampled through apertures, and nothing reduces it to per-dot intensity.

### Blocker 4: The eyes bypass the spatial model

```js
new THREE.MeshBasicMaterial({ color: 0x8de8ff, depthTest: false });
leftEye.renderOrder = 2;
```

Disabling depth testing forces the eyes to render in front of everything. This hides a real problem: the eyes are inside the opaque sphere at `z = 2.15`, while the sphere radius is `2.4`. Depth correction was chosen over geometric correction.

### Blocker 5: Light intensity is defined twice, inconsistently

```js
const keyLight = new THREE.PointLight(0xb8e7ff, 18, 20);
```

but the parameter object declares:

```js
lightIntensity: 40
```

`applyParameters()` overwrites the initial value on load, so the literal `18` is dead and misleading.

### Blocker 6: Comment says aimed, code says static

```js
// Key light: in front of the face, below and to the left, aimed back at the sphere
```

A `PointLight` is not aimed. If directional control matters, use a `DirectionalLight` or `SpotLight`. Otherwise remove the word aimed.

### Blocker 7: Eye size control is a scale hack

```js
const eyeScale = parameters.eye / 0.52;
```

Changing eye size by scaling the existing mesh couples the control to a magic number. It also collides with the blink scale on `scale.y`.

### Blocker 8: Invalid material property

```js
new THREE.LineBasicMaterial({ color: 0x5b7688, opaque: true, ... })
```

`opaque` is not a Three.js material property. It is silently ignored and reads as if it does something.

### Blocker 9: Output formatting is unmaintainable

The `applyParameters()` output block is a single nested ternary chain covering units and precision. It is already difficult to read and will break as controls are added.

### Blocker 10: Parameter wiring is stringly typed

Control IDs are translated to parameter names with:

```js
name.replace(/-([a-z])/g, (_, letter) => letter.toUpperCase())
```

and toggle IDs with:

```js
name.replace('show', 'show-').toLowerCase()
```

This couples HTML naming conventions to JavaScript behaviour. It is fragile and hard to follow.

### Blocker 11: Layout and interaction issues

- The controls panel is `position: fixed` and can extend beyond the viewport, which made automated clicks fail until the browser window was enlarged.
- `resizeScreen()` is bound to window resize but not observed with a `ResizeObserver`, so container-only changes are missed.
- `resizeScreen()` is called after `applyParameters()`, and both touch camera and renderer state, so initialisation order is implicit.
- The HUD is fixed to the viewport, not the phone screen.
- The closing `</div>` for `#phone-screen` sits after the script, which is valid but easy to break.

### Blocker 12: Findings file was never filled in

`findings.md` still records every test as "Not tested". The spike has generated real results, and none are recorded. Verdict remains "Pending prototype run".

## 3. Architectural assessment

The layered stack itself is sound and worth keeping:

```text
Sphere model → lighting → blur → dot-matrix mask → display
```

The problem is that the last two layers are implemented in a way that cannot express the intended optics.

A more faithful implementation would:

1. Render the sphere and eyes to an offscreen render target.
2. Quantise that target into a dot grid, one luminance value per cell.
3. Render dots as emitters, with per-dot glow and halation.
4. Keep the aperture mask as a presentation concern, not as the sole optical model.

That is a real viewport translation layer. The current CSS mask is a sampling mask, not a translation layer.

## 4. Verdict

| Question | Verdict |
|---|---|
| Is the sphere-plus-eyes spatial model viable? | Yes, with corrections. |
| Is centre-pinned rotation correct? | Yes. |
| Is the dot-matrix viewport correctly implemented? | No. |
| Does the prototype answer a single unambiguous question? | No. |
| Is the spike ready to be closed or promoted? | No. |

## 5. Recommended next actions, in order

1. Split the spike into two questions: sphere model, then viewport translation.
2. Remove the CSS-mask viewport from the sphere-model spike, or promote it to its own spike.
3. Fix depth-correct eye geometry and remove `depthTest: false`.
4. Make the dot-matrix substrate visible against a non-black background, or declare black-on-black intentional.
5. Record actual findings in `findings.md`, including what failed.
6. Replace the nested ternary output chain with a per-control formatter table.
7. Remove the dead light intensity literal and the invalid `opaque` property.
8. Add a `ResizeObserver` and make the controls panel scrollable.
