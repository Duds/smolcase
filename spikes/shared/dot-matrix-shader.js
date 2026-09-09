// Shared dot-matrix viewport translation.
//
// Both the dot-matrix viewport spike and the expression spike render a source
// offscreen, then quantise it through this pass. Keeping one copy here stops the
// two spikes drifting apart.
//
// Import THREE from the same URL as the caller so the module is deduped.

import * as THREE from 'https://cdn.jsdelivr.net/npm/three@0.180.0/build/three.module.js';

export const DOT_COLOR = 0x8de8ff;

export const DOT_MATRIX_DEFAULTS = {
  cols: 38,
  rows: 68,
  dotRadius: 0.36,
  dotSoftness: 0.3,
  cellFill: 0.82,
  glow: 1.8,
  glowSpread: 0.5,
  halation: 0.6,
  ghostFloor: 0.03,
  exposure: 4
};

const VERTEX_SHADER = `
  varying vec2 vUv;
  void main() {
    // Fullscreen pass: the plane spans clip space directly.
    vUv = uv;
    gl_Position = vec4(position.xy * 2.0, 0.0, 1.0);
  }
`;

const FRAGMENT_SHADER = `
  precision highp float;
  varying vec2 vUv;
  uniform sampler2D tSource;
  uniform vec2 uResolution;
  uniform vec2 uGrid;
  uniform float uDotRadius;
  uniform float uDotSoftness;
  uniform float uCellFill;
  uniform float uGlow;
  uniform float uGlowSpread;
  uniform float uHalation;
  uniform float uGhostFloor;
  uniform float uExposure;
  uniform float uSourceTint;
  uniform vec3 uDotColor;

  vec3 sampleCell(vec2 cell) {
    // Average a few taps across the cell so each dot carries the mean
    // luminance of the region it represents, not a single point sample.
    vec2 cellUv = (cell + vec2(0.5)) / uGrid;
    vec2 stepUv = vec2(0.25) / uGrid;
    vec3 sum = texture2D(tSource, cellUv).rgb;
    sum += texture2D(tSource, cellUv + vec2(stepUv.x, stepUv.y)).rgb;
    sum += texture2D(tSource, cellUv + vec2(-stepUv.x, stepUv.y)).rgb;
    sum += texture2D(tSource, cellUv + vec2(stepUv.x, -stepUv.y)).rgb;
    sum += texture2D(tSource, cellUv + vec2(-stepUv.x, -stepUv.y)).rgb;
    return sum / 5.0;
  }

  void main() {
    vec2 pixel = vUv * uResolution;
    vec2 cellSize = uResolution / uGrid;
    vec2 cell = floor(vUv * uGrid);
    vec2 cellCenter = (cell + vec2(0.5)) * cellSize;

    float cellRadius = min(cellSize.x, cellSize.y) * 0.5;
    float distancePixels = length(pixel - cellCenter);
    float d = distancePixels / max(cellRadius, 1.0);

    vec3 source = sampleCell(cell);
    float luminance = dot(source, vec3(0.2126, 0.7152, 0.0722));
    float energy = clamp(luminance * uExposure, 0.0, 1.0);

    // Emitter core, a filled disc with a soft rim.
    float radius = uDotRadius * uCellFill;
    float core = 1.0 - smoothstep(radius * (1.0 - uDotSoftness), radius, d);

    // Per-dot glow and wider halation, both scaled by that dot's energy.
    float glow = exp(-(d * d) / max(uGlowSpread * uGlowSpread, 1e-4)) * uGlow;
    float halo = exp(-(d * d) / max(uGlowSpread * uGlowSpread * 4.0, 1e-4)) * uHalation;

    // Unlit emitters are still faintly visible, the ghost dot substrate.
    float floorTerm = uGhostFloor * (1.0 - smoothstep(radius * 0.9, radius * 1.3, d));

    // Optional hue pass-through. At 0 every dot uses uDotColor, which is the
    // historical behaviour. At 1 the dot takes the source hue, so a red
    // source emits red dots. A black source has no hue, so it falls back to
    // uDotColor, and the ghost floor always uses uDotColor. Otherwise the
    // substrate would be multiplied by black and disappear.
    float peak = max(source.r, max(source.g, source.b));
    vec3 chromaticity = peak > 1e-3 ? source / peak : uDotColor;
    vec3 emitter = mix(uDotColor, chromaticity, uSourceTint);
    vec3 litTerm = emitter * (core + glow + halo) * energy;
    vec3 ghostTerm = uDotColor * floorTerm;
    gl_FragColor = vec4(litTerm + ghostTerm, 1.0);
  }
`;

export function createDotMatrixUniforms(texture, overrides = {}) {
  const options = { ...DOT_MATRIX_DEFAULTS, ...overrides };
  return {
    tSource: { value: texture },
    uResolution: { value: new THREE.Vector2(1, 1) },
    uGrid: { value: new THREE.Vector2(options.cols, options.rows) },
    uDotRadius: { value: options.dotRadius },
    uDotSoftness: { value: options.dotSoftness },
    uCellFill: { value: options.cellFill },
    uGlow: { value: options.glow },
    uGlowSpread: { value: options.glowSpread },
    uHalation: { value: options.halation },
    uGhostFloor: { value: options.ghostFloor },
    uExposure: { value: options.exposure },
    uSourceTint: { value: 0 },
    uDotColor: { value: new THREE.Color(DOT_COLOR) }
  };
}

export function createDotMatrixMaterial(uniforms) {
  return new THREE.ShaderMaterial({
    uniforms,
    vertexShader: VERTEX_SHADER,
    fragmentShader: FRAGMENT_SHADER
  });
}

// Renders a source scene into a target, then quantises it to the screen.
export function createDotMatrixPass(renderer, sourceScene, camera) {
  const renderTarget = new THREE.WebGLRenderTarget(1, 1, {
    minFilter: THREE.LinearFilter,
    magFilter: THREE.LinearFilter,
    colorSpace: THREE.SRGBColorSpace,
    depthBuffer: true
  });
  const uniforms = createDotMatrixUniforms(renderTarget.texture);
  const scene = new THREE.Scene();
  scene.add(new THREE.Mesh(new THREE.PlaneGeometry(1, 1), createDotMatrixMaterial(uniforms)));
  const passCamera = new THREE.Camera();

  return {
    renderTarget,
    uniforms,
    resize(width, height) {
      renderTarget.setSize(Math.max(width, 1), Math.max(height, 1));
      uniforms.uResolution.value.set(width, height);
    },
    render(bypass = false) {
      if (bypass) {
        renderer.setRenderTarget(null);
        renderer.render(sourceScene, camera);
        return;
      }
      renderer.setRenderTarget(renderTarget);
      renderer.clear();
      renderer.render(sourceScene, camera);
      renderer.setRenderTarget(null);
      renderer.render(scene, passCamera);
    }
  };
}
