#version 300 es
precision highp float;
in vec2 vUv;
out vec4 fragColor;
uniform vec2 uResolution;
uniform vec4 uEyeA;   // centre x, centre y, half width, half height
uniform vec4 uEyeB;
uniform vec4 uLidA;   // top lid, bottom lid, curve, outer direction
uniform vec4 uLidB;
uniform float uSlant;
uniform float uLidSlant;
uniform float uSoft;
uniform vec3 uColor;
uniform vec3 uShape;
uniform vec2 uArc;
uniform float uTopSlope;
uniform vec2 uPivot;
uniform float uRoll;
uniform float uBlink;
uniform float uSphereMode;
uniform vec2 uSphereCenter;
uniform float uSphereRadius;
uniform vec2 uGaze;
uniform vec3 uSphereColor;
uniform vec3 uHeartColor;
uniform vec3 uLightTint;
uniform float uSphereGlow;

float segmentDistance(vec2 p, vec2 a, vec2 b) {
  vec2 ab = b - a;
  return length(p - a - ab * clamp(dot(p - a, ab) / dot(ab, ab), 0.0, 1.0));
}

vec3 rotateVector(vec3 v, vec2 gaze) {
  float cy = cos(gaze.x), sy = sin(gaze.x);
  vec3 yawed = vec3(cy * v.x + sy * v.z, v.y, -sy * v.x + cy * v.z);
  float cx = cos(gaze.y), sx = sin(gaze.y);
  return vec3(yawed.x, cx * yawed.y - sx * yawed.z, sx * yawed.y + cx * yawed.z);
}

float heartDistance(vec2 p) {
  p.x = abs(p.x);
  if (p.x + p.y > 1.0) return length(p - vec2(0.25, 0.75)) - 0.35355339;
  vec2 tip = p - vec2(0.0, 1.0);
  vec2 side = p - vec2(0.5 * max(p.x + p.y, 0.0));
  return sqrt(min(dot(tip, tip), dot(side, side))) * sign(p.x - p.y);
}

// Returns x: normal shapes, y: hearts, so hearts can be coloured apart.
vec2 eyeMask(vec2 uv, vec4 eye, vec4 lid) {
  vec2 p = uv - eye.xy;
  // Squash every shape, including hearts and arcs, during a blink.
  p.y /= max(1.0 - uBlink, 0.03);
  p.x -= p.y * uSlant;
  vec2 q = p / max(eye.zw, vec2(1e-4));
  float distanceScreen = (length(q) - 1.0) * min(eye.z, eye.w);
  float xr = q.x;
  float topLimit = eye.w * (1.0 - lid.x) - lid.z * eye.w * xr * xr
                 + uTopSlope * eye.w * lid.w * xr;
  float botLimit = -eye.w * (1.0 - lid.y)
                 + lid.z * eye.w * xr * xr
                 + uLidSlant * eye.w * lid.w * xr;
  float solid = (1.0 - smoothstep(-uSoft, uSoft, distanceScreen))
              * (1.0 - smoothstep(topLimit - uSoft, topLimit + uSoft, p.y))
              * smoothstep(botLimit - uSoft, botLimit + uSoft, p.y);

  // Closed eyes: a bounded curved stroke, not a small open oval.
  float arcX = clamp(p.x, -eye.z * 0.78, eye.z * 0.78);
  float arcRatio = arcX / eye.z;
  float arcY = uArc.x * eye.w * (1.0 - arcRatio * arcRatio);
  float slope = -2.0 * uArc.x * eye.w * arcRatio / eye.z;
  float arcDistance = length(vec2(p.x - arcX,
    (p.y - arcY) / sqrt(1.0 + slope * slope))) - eye.w * uArc.y;
  float arch = 1.0 - smoothstep(-uSoft, uSoft, arcDistance);

  // Mirrored > < strokes, using a true segment distance in screen units.
  vec2 squeezed = vec2(p.x * -lid.w, p.y);
  vec2 tip = vec2(eye.z * 0.55, 0.0);
  float chevronDistance = min(
    segmentDistance(squeezed, vec2(-eye.z * 0.7, eye.w * 0.7), tip),
    segmentDistance(squeezed, vec2(-eye.z * 0.7, -eye.w * 0.7), tip));
  float chevron = 1.0 - smoothstep(eye.w * uArc.y - uSoft,
    eye.w * uArc.y + uSoft, chevronDistance);

  // Filled heart silhouette. No pupil or specular highlight.
  float heartField = heartDistance(q * 0.55 + vec2(0.0, 0.55));
  // Fixed epsilon instead of fwidth(), so the shader does not depend
  // on the derivatives extension.
  float heartEdge = max(uSoft * 0.55 / min(eye.z, eye.w), 1e-4);
  float heart = 1.0 - smoothstep(-heartEdge, heartEdge, heartField);
  float solidWeight = max(0.0, 1.0 - uShape.x - uShape.y - uShape.z);
  return vec2(solid * solidWeight + arch * uShape.x + chevron * uShape.y,
    heart * uShape.z);
}

void main() {
  float aspect = uResolution.x / max(uResolution.y, 1.0);
  vec2 uv = vec2(vUv.x * aspect, vUv.y);
  // Transform the pair around its centre, not the screen origin.
  vec2 p = uv - uPivot;
  float c = cos(uRoll), s = sin(uRoll);
  vec2 localUv = vec2(c * p.x + s * p.y, -s * p.x + c * p.y) + uPivot;
  vec2 mask = max(eyeMask(localUv, uEyeA, uLidA), eyeMask(localUv, uEyeB, uLidB));
  mask *= step(0.5, vUv.y); // Source eyes stay in the upper screen half.
  if (uSphereMode > 0.5) {
    // Project the screen point onto the sphere, then undo the gaze
    // rotation so the eye pattern stays fixed to the sphere surface.
    vec2 offset = (uv - uSphereCenter) / max(uSphereRadius, 1e-4);
    float radiusSquared = dot(offset, offset);
    if (radiusSquared > 1.0) {
      // Halo bleeding past the silhouette, so the sphere reads as lit.
      float halo = exp(-(sqrt(radiusSquared) - 1.0) * 5.0) * uSphereGlow;
      fragColor = vec4(uLightTint * halo, 1.0);
      return;
    }
    vec3 surface = vec3(offset, sqrt(max(1.0 - radiusSquared, 0.0)));
    vec3 pattern = rotateVector(surface, -uGaze);
    vec2 sphereMask = vec2(0.0);
    if (pattern.z > 0.0) {
      vec2 patternUv = uSphereCenter + pattern.xy * uSphereRadius;
      vec2 q = patternUv - uPivot;
      vec2 rolled = vec2(c * q.x + s * q.y, -s * q.x + c * q.y) + uPivot;
      sphereMask = max(eyeMask(rolled, uEyeA, uLidA), eyeMask(rolled, uEyeB, uLidB));
      sphereMask *= step(0.5, patternUv.y);
    }
    vec3 lightDirection = normalize(vec3(-0.45, -0.65, 0.61));
    float shading = 0.35 + 0.65 * max(dot(surface, lightDirection), 0.0);
    // Rim brightening towards the silhouette, the lit edge of the sphere.
    float rim = pow(1.0 - surface.z, 3.0) * uSphereGlow;
    fragColor = vec4(uSphereColor * shading * uLightTint + uLightTint * rim
      + uColor * sphereMask.x + uHeartColor * sphereMask.y, 1.0);
    return;
  }
  fragColor = vec4(uColor * mask.x + uHeartColor * mask.y, 1.0);
}
