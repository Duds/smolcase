#version 300 es
precision highp float;
in vec2 vUv;
out vec4 fragColor;
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
  vec3 sum = texture(tSource, cellUv).rgb;
  sum += texture(tSource, cellUv + vec2(stepUv.x, stepUv.y)).rgb;
  sum += texture(tSource, cellUv + vec2(-stepUv.x, stepUv.y)).rgb;
  sum += texture(tSource, cellUv + vec2(stepUv.x, -stepUv.y)).rgb;
  sum += texture(tSource, cellUv + vec2(-stepUv.x, -stepUv.y)).rgb;
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
  fragColor = vec4(litTerm + ghostTerm, 1.0);
}
