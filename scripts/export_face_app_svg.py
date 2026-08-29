#!/usr/bin/env python3
"""
Export the SMOLCASE Face Appliance dot matrix as an Illustrator-safe layered
SVG and generate its Kotlin aperture layout.

The SVG is the single source of truth for the physical dot grid. Illustrator
edits it (tune dot positions, hole sizes, panel dims); this script re-reads
it and emits:

  * mech/face-appliance-dots.svg        - layered master (rebuilt)
  * android/.../matrix/ApertureGrid.kt  - generated Kotlin, same geometry

Layers (paint order, back to front): bg, LEDs, Eyes Glow, Sample Eyes,
Light Baffle, Light Mask. Baffle/Mask are compound paths with real punched
holes (no <mask> elements) so Illustrator round-trips them cleanly.

Brightness policy (the off-LED / bleed complaint):
  * Off LED emitters sit near-panel (dark recess, low contrast).
  * Lit phosphor is a high-contrast narrow dot.
  * Glow bloom is tight and faint: short radius, low peak alpha, fast falloff.

Run:
    python3 scripts/export_face_appliance_svg.py                # default 38x68
    python3 scripts/export_face_appliance_svg.py --grid 45x80
"""

import argparse
import math
import os

# --- Grid / dot geometry (mirror ApplianceMatrixCanvas defaults) -------------
DOT_RADIUS_RATIO = 0.38   # dot radius = min(cellW, cellH) * ratio (TarsFaceView)
COLS_DFLT = 38
ROWS_DFLT = 68

# Physical cell size (mm) over the Pixel 8 active area (~63.6 x 141.3 mm).
CELL_W_MM = 1.674
CELL_H_MM = 2.078

# --- Palette (photometrically tuned) ----------------------------------------
# Panel substrate: near-black appliance plastic.
PANEL = "#171a1d"
PANEL_EDGE = "#0a0d10"
# OFF / unlit emitters: dark recessed aperture, barely lifted above panel.
LED_OFF = "#101419"
LED_OFF_OPACITY = 0.35
# Lit phosphor: bright warm ice-white (matches segment color from eval).
SEGMENT = "#eaf1f6"
# Baffle = fully opaque black honeycomb; Mask = dark front face.
BAFFLE = "#000000"
MASK = "#0c0e11"
MASK_OPACITY = 0.85

# Glow (bleed) tuning — intentionally subtle.
GLOW_INTENSITY = 0.35      # carried for reference; sample uses tuned alphas
GLOW_PEAK = 0.07           # peak alpha of the radial glow gradient
GLOW_RADIUS_CELLS = 1.2    # bloom radius in cells (was 1.6)


def eye_signed(nx, ny, **p):
    """Port of CozmoEyeParams.signedDistance (negative = inside eye)."""
    cx, cy = p["cx"], p["cy"]
    w, h = p["w"], p["h"]
    radius = p.get("radius", 0.0)
    slant = p.get("slant", 0.0)
    sxx = p.get("scale_x", 1.0)
    syy = p.get("scale_y", 1.0)
    top_l = p.get("top_lid", 0.0)
    top_a = p.get("top_angle", 0.0)
    top_c = p.get("top_curve", 0.0)
    bot_l = p.get("bot_lid", 0.0)
    bot_a = p.get("bot_angle", 0.0)
    bot_c = p.get("bot_curve", 0.0)
    maxb = p.get("max_boundary", 0.50)

    if ny > maxb:
        return (ny - maxb) + 0.01

    dx, dy = nx - cx, ny - cy
    ca, sa = math.cos(-slant), math.sin(-slant)
    rx, ry = dx * ca - dy * sa, dx * sa + dy * ca
    sx, sy = rx / max(sxx, 0.01), ry / max(syy, 0.01)

    hw = (w * 0.5) - radius
    hh = (h * 0.5) - radius
    qx, qy = abs(sx) - hw, abs(sy) - hh
    od = math.sqrt(max(qx, 0.0) ** 2 + max(qy, 0.0) ** 2)
    i_d = min(max(qx, qy), 0.0)
    base = od + i_d - radius

    ty = (h * 0.5) - (top_l * h)
    tp = (sy - ty) + (sx * math.sin(top_a)) - (top_c * (1 - (sx / (w * 0.5)) ** 2))

    by = -(h * 0.5) + (bot_l * h)
    bp = (by - sy) + (sx * math.sin(bot_a)) - (bot_c * (1 - (sx / (w * 0.5)) ** 2))

    return max(base, max(tp, bp))


def rasterize_eyes(left, right, cols, rows, dot_norm):
    """Mirror CozmoEyeRasterizer.rasterizeEyes. Returns list of (r, c, core, glow)."""
    lit = []
    for r in range(rows):
        ny = (r + 0.5) / rows
        for c in range(cols):
            nx = (c + 0.5) / cols
            md = min(eye_signed(nx, ny, **left), eye_signed(nx, ny, **right))
            if md <= -dot_norm:
                core = 1.0
            elif md < dot_norm:
                core = 0.5 - (md / (2.0 * dot_norm))
            else:
                core = 0.0
            core = max(0.0, min(1.0, core))
            glow = GLOW_INTENSITY * math.exp(-md / (dot_norm * 2.8)) if md > 0 else 0.0
            final = min(core + glow, 1.0)
            if final > 0.001:
                lit.append((r, c, core, glow))
    return lit


def grid_centers(cols, rows, cw, ch):
    for r in range(rows):
        for c in range(cols):
            yield (c + 0.5) * cw, (r + 0.5) * ch, c, r


def svg_esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def build_svg(cols, rows, cw, ch):
    W = cols * cw
    H = rows * ch
    dot = min(cw, ch) * DOT_RADIUS_RATIO
    dot_norm = 0.5 / cols
    cy_eye = 0.23 * H

    # sample neutral eyes (CozmoEyeParams defaults, testbed positions)
    left = dict(cx=0.30, cy=0.23, w=0.26, h=0.20, radius=0.07)
    right = dict(cx=0.70, cy=0.23, w=0.26, h=0.20, radius=0.07)
    lit = rasterize_eyes(left, right, cols, rows, dot_norm)

    centers = list(grid_centers(cols, rows, cw, ch))

    # Compound path holes: outer rect + every aperture as a full circle,
    # filled with evenodd so holes read as transparent punches.
    def compound(hole_rad):
        parts = ["M 0,0 H {:.3f} V {:.3f} H 0 Z".format(W, H)]
        for cx, cy, c, r in centers:
            parts.append("M {:.3f} {:.3f} A {:.3f} {:.3f} 0 360 0 Z".format(cx, cy, hole_rad, hole_rad))
        return " ".join(parts)

    baffle_path = compound(dot)
    mask_path = compound(dot * 0.82)

    L = []
    a = L.append
    a('<?xml version="1.0" encoding="UTF-8"?>')
    a('<svg xmlns="http://www.w3.org/2000/svg"')
    a('     xmlns:inkscape="http://www.inkscape.org/namespaces/inkscape"')
    a('     width="{:.3f}mm" height="{:.3f}mm" viewBox="0 0 {:.3f} {:.3f}">'.format(W, H, W, H))
    a('  <defs>')
    a('    <radialGradient id="ledGlow" cx="50%" cy="50%" r="50%">')
    a('      <stop offset="18%" stop-color="#eaf1f6" stop-opacity="{}"/>'.format(GLOW_PEAK))
    a('      <stop offset="55%" stop-color="#dfe9ef" stop-opacity="{:.3f}"/>'.format(GLOW_PEAK * 0.35))
    a('      <stop offset="100%" stop-color="#dfe9ef" stop-opacity="0"/>')
    a('    </radialGradient>')
    a('  </defs>')
    a('')

    # (1) bg — opaque substrate + edge
    a('  <g id="bg-layer" inkscape:label="bg" inkscape:groupmode="layer">')
    a('    <rect x="0" y="0" width="{:.3f}" height="{:.3f}" rx="12" fill="{}"/>'.format(W, H, PANEL_EDGE))
    a('    <rect x="1.6" y="1.6" width="{:.3f}" height="{:.3f}" rx="9" fill="{}"/>'.format(W - 3.2, H - 3.2, PANEL))
    a('  </g>')
    a('')

    # (2) LEDs: full emitter plane, off emitters dark recess
    a('  <g id="leds-layer" inkscape:label="LEDs" inkscape:groupmode="layer">')
    a('    <g opacity="{}" fill="{}">'.format(LED_OFF_OPACITY, LED_OFF))
    for cx, cy, c, r in centers:
        a('      <circle cx="{:.3f}" cy="{:.3f}" r="{:.3f}"/>'.format(cx, cy, dot))
    a('    </g>')
    a('  </g>')
    a('')

    # (3) Eyes Glow: soft bleed halo over the two eye regions
    a('  <g id="eyes-glow-layer" inkscape:label="Eyes Glow" inkscape:groupmode="layer">')
    glow_r = cw * GLOW_RADIUS_CELLS
    seen = set()
    for (r, c, core, glow) in lit:
        if glow > 0.01:
            cx = (c + 0.5) * cw
            cy = (r + 0.5) * ch
            ded = (round(cx, 2), round(cy, 2))
            if ded in seen: continue
            seen.add(ded)
            a('    <circle cx="{:.3f}" cy="{:.3f}" r="{:.3f}" fill="url(#ledGlow)"/>'.format(cx, cy, glow_r))
    a('  </g>')
    a('')

    # (4) Sample Eyes: lit phosphor dots on the blerly tight
    a('  <g id="sample-eyes-layer" inkscape:label="Sample Eyes" inkscape:groupmode="layer">')
    for (r, c, core, glow) in lit:
        cx = (c + 0.5) * cw
        cy = (r + 0.5) * ch
        rr = dot * (0.50 + 0.35 * core)          # not oversized
        alpha = max(0.35, min(1.0, 0.42 + 0.58 * core))   # true lit only
        a('    <circle cx="{:.3f}" cy="{:.3f}" r="{:.3f}" fill="{}" fill-opacity="{:.2f}"/>'.format(
            cx, cy, rr, SEGMENT, alpha))
    a('  </g>')
    a('')

    # (5) Light Baffle: black perforated plate (real holes, no mask)
    a('  <g id="light-baffle-layer" inkscape:label="Light Baffle" inkscape:groupmode="layer">')
    a('    <path d="{}" fill="{}" fill-rule="evenodd"/>'.format(baffle_path, BAFFLE))
    a('  </g>')
    a('')

    # (6) Light Mask: front face, slightly smaller apertures
    a('  <g id="light-mask-layer" inkscape:label="Light Mask" inkscape:groupmode="layer">')
    a('    <path d="{}" fill="{}" fill-opacity="{}" fill-rule="evenodd"/>'.format(mask_path, MASK, MASK_OPACITY))
    a('  </g>')
    a('')
    a('</svg>')
    return "\n".join(L), lit, centers, dot


def emit_kotlin(path, cols, rows, cw, ch, lit):
    """Generate the Kotlin aperture layout from the SVG grid geometry."""
    dot = min(cw, ch) * DOT_RADIUS_RATIO
    L = []
    a = L.append
    a('package com.smolcase.companion.matrix')
    a('')
    a('/**')
    a(' * Aperture grid layout generated from me/face-appliance-dots.svg.')
    a(' *')
    a(' * Source geometry: {}x{} dot grid, {:.3f}mm x {:.3f}mm cell.'.format(cols, rows, cw, ch))
    a(' * Do not hand-edit - regenerate with:')
    a(' *   python3 scripts/export_face_app_svg.py --grid {}x{}'.format(cols, rows))
    a(' */')
    a('')
    a('object ApertureGrid {')
    a('    const val COLS = {}'.format(cols))
    a('    const val ROWS = {}'.format(rows))
    a('    const val CELL_W_MM = {:.3f}f'.format(cw))
    a('    const val CELL_H_MM = {:.3f}f'.format(ch))
    a('    const val DOT_RADIUS_MM = {:.3f}f'.format(dot))
    a('')
    a('    /** Project a normalized aperture (0..1) onto the canvas buffer column. */')
    a('    fun canvasCol(nx: Float, bufferCols: Int = COLS): Int =')
    a('        ((nx * bufferCols).toInt()).coerceIn(0, bufferCols - 1)')
    a('')
    a('    /** Project a normalized aperture (0..1) onto the canvas buffer row. */')
    a('    fun canvasRow(ny: Float, bufferRows: Int = ROWS): Int =')
    a('        ((ny * bufferRows).toInt()).coerceIn(0, bufferRows - 1)')
    a('')
    a('    /** Physical mm -> normalized aperture coordinate. */')
    a('    fun apertureX(xMm: Float): Float = (xMm / (COLS * CELL_W_MM)).coerceIn(0f, 1f)')
    a('    fun apertureY(yMm: Float): Float = (yMm / (ROWS * CELL_H_MM)).coerceIn(0f, 1f)')
    a('')
    a('    private val litCells = intArrayOf(')
    a('       // (canvasRow * COLS + canvasCol) for each lit aperture of the sample frame')
    cells = []
    for (r, c, core, glow) in lit:
        idx = r * cols + c
        cells.append(str(idx))
    a('       ' + ', '.join(cells))
    a('    )')
    a('')
    a('    /** True if the given buffer index lies under a lit aperture. */')
    a('    fun isLitAperture(bufferIndex: Int): Boolean = litCells.binarySearch(bufferIndex) >= 0')
    a('')
    a('    /** True if the given buffer (row, col) lies under a lit aperture. */')
    a('    fun isLitAperture(row: Int, col: Int): Boolean =')
    a('        isLitAperture(row * COLS + col)')
    a('}')
    code = "\n".join(L)
    with open(path, "w") as f:
        f.write(code)
    return path


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("-o", "--out", default="mech/face-appliance-dots.svg")
    ap.add_argument("--kt", default="android/app/src/main/java/com/smolcase/companion/matrix/ApertureGrid.kt")
    ap.add_argument("--grid", default="38x68")
    ap.add_argument("--cell", type=float, nargs=2, default=[CELL_W_MM, CELL_H_MM])
    ap.add_argument("--no-kt", action="store_true", help="only write the SVG, skip Kotlin")
    args = ap.parse_args()

    cols, rows = (int(v) for v in args.grid.split("x"))
    cw, ch = args.cell

    root = os.path.dirname(os.path.dirname(__file__))
    svg_path = args.out if os.path.isabs(args.out) else os.path.join(root, args.out)
    svg, lit, centers, dot = build_svg(cols, rows, cw, ch)
    os.makedirs(os.path.dirname(svg_path), exist_ok=True)
    with open(svg_path, "w") as f:
        f.write(svg)
        f.write("\n")
    print("Wrote SVG: {}".format(svg_path))
    print("  {}x{} grid, {:.3f}mm x {:.3f}mm cell, {:.3f}mm aperture".format(cols, rows, cw, ch, dot))
    print("  {} lit apertures in sample frame".format(len(lit)))

    if not args.no_kt:
        kt_path = args.kt if os.path.isabs(args.kt) else os.path.join(root, args.kt)
        emit_kotlin(kt_path, cols, rows, cw, ch, lit)
        print("Wrote KT: {}".format(kt_path))


if __name__ == "__main__":
    main()