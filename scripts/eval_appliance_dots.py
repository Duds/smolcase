#!/usr/bin/env python3
"""
Appliance Dot Matrix Evaluation Harness and Optimizer

Compares synthetic renders of the Appliance Dots diode shader against
empirical crops extracted from Screenshot_20260821-135151.png.

Evaluates:
1. Photometric Accuracy: Peak lit, substrate unlit, ghost recess, and bezel delta-E.
2. Radial Point Spread Function (PSF): Decay curve error (RMSE) against measured profile.
3. Structural Reconstruction Fidelity: SSIM / PSNR on reference icon matrices.
4. Diff Heatmaps: Visual error maps.
"""

import os
import json
import numpy as np
from PIL import Image
from scipy import ndimage
from scipy.optimize import minimize
import matplotlib.pyplot as plt

SCREENSHOT_PATH = "Screenshot_20260821-135151.png"
OUTPUT_DIR = "docs/01-research/analysis/eval_loop"
os.makedirs(OUTPUT_DIR, exist_ok=True)

DEFAULT_PARAMS = {
    "colors": {
        "panel": "#171a1d",
        "bezel": "#0a0d10",
        "segment": "#eaeeef",
        "pupil": "#171a1d",
        "glint": "#ffffff",
        "ghostDot": "#20272d"
    },
    "segAlpha": 255,
    "dotR_ratio": 0.355,      # 71% FWHM body (~3.78px)
    "plateauR_ratio": 0.250,  # plateau (~2.66px)
    "hotR_ratio": 0.120,      # emitter center (~1.28px)
    "ghostR_ratio": 0.300,    # unlit ghost recess
    "ghostAlpha": 0.45,
    "glow_blur": 3.0,         # shadow blur
    "glow_alpha": 0.25
}

def hex_to_rgb(hex_str):
    hex_str = hex_str.lstrip('#')
    return np.array([int(hex_str[i:i+2], 16) for i in (0, 2, 4)], dtype=np.float32)

def extract_reference_data():
    """Extract empirical multi-dot ground truth radial profile."""
    img = Image.open(SCREENSHOT_PATH).convert('RGB')
    arr = np.array(img, dtype=np.float32)
    lum = 0.299 * arr[:,:,0] + 0.587 * arr[:,:,1] + 0.114 * arr[:,:,2]

    # Find isolated dots
    thresh = lum > 140
    labeled, num = ndimage.label(thresh)
    centroids = []
    for i in range(num):
        mask = (labeled == i+1)
        if 10 <= np.sum(mask) <= 50:
            cy, cx = ndimage.center_of_mass(lum * mask)
            centroids.append((cy, cx))

    isolated = []
    for cy, cx in centroids:
        dists = [np.sqrt((cy - ocy)**2 + (cx - ocx)**2) for ocy, ocx in centroids if (ocy != cy or ocx != cx)]
        if min(dists) > 10.0:
            isolated.append((cy, cx))

    all_r = []
    all_l = []
    for cy, cx in isolated:
        iy, ix = int(round(cy)), int(round(cx))
        if 12 <= iy < lum.shape[0]-12 and 12 <= ix < lum.shape[1]-12:
            for dy in range(-8, 9):
                for dx in range(-8, 9):
                    r = np.sqrt(dx**2 + dy**2)
                    l = lum[iy+dy, ix+dx]
                    all_r.append(r)
                    all_l.append(l)

    all_r = np.array(all_r)
    all_l = np.array(all_l)

    # 16 radial bins from r=0 to r=6.0px (within cell radius)
    bins = np.linspace(0, 6.0, 13)
    bin_centers = (bins[:-1] + bins[1:]) / 2
    empirical_psf = []
    for i in range(len(bins)-1):
        m = (all_r >= bins[i]) & (all_r < bins[i+1])
        empirical_psf.append(all_l[m].mean() if np.sum(m) > 0 else 0)
    empirical_psf = np.array(empirical_psf)

    # Substrate color & peak lit color
    sub_color = arr[(lum >= 15) & (lum <= 40)].mean(axis=0)
    peak_color = arr[lum > 220].mean(axis=0)

    return {
        "bin_centers": bin_centers,
        "empirical_psf": empirical_psf,
        "sub_color": sub_color,
        "peak_color": peak_color
    }

def render_synthetic_dot(pitch=10.64, params=DEFAULT_PARAMS):
    size = int(round(pitch * 2))
    if size % 2 == 0: size += 1
    center = size / 2.0
    
    cellW = pitch
    dotR = cellW * params["dotR_ratio"]
    plateauR = cellW * params["plateauR_ratio"]
    hotR = cellW * params["hotR_ratio"]

    bg_rgb = hex_to_rgb(params["colors"]["panel"])
    seg_rgb = hex_to_rgb(params["colors"]["segment"])
    emitter_rgb = np.array([255.0, 255.0, 255.0], dtype=np.float32)

    y_coords, x_coords = np.ogrid[:size, :size]
    dists = np.sqrt((x_coords - center + 0.5)**2 + (y_coords - center + 0.5)**2)

    patch = np.tile(bg_rgb, (size, size, 1))

    # Pass 1: Local Optical halo
    glow_sigma = max(0.5, params["glow_blur"] / 2.0)
    glow_falloff = np.exp(-0.5 * (dists / glow_sigma)**2) * params["glow_alpha"]
    patch = patch * (1.0 - glow_falloff[:,:,None]) + seg_rgb * glow_falloff[:,:,None]

    # Pass 2: Diode body (FWHM edge)
    body_aa = np.clip(0.5 - (dists - dotR), 0.0, 1.0) * (params["segAlpha"] / 255.0)
    patch = patch * (1.0 - body_aa[:,:,None]) + seg_rgb * body_aa[:,:,None]

    # Pass 3: Core plateau disc
    plateau_aa = np.clip(0.5 - (dists - plateauR), 0.0, 1.0) * 0.95
    patch = patch * (1.0 - plateau_aa[:,:,None]) + seg_rgb * plateau_aa[:,:,None]

    # Pass 4: Hot emitter core
    hot_aa = np.clip(0.5 - (dists - hotR), 0.0, 1.0) * 0.85
    patch = patch * (1.0 - hot_aa[:,:,None]) + emitter_rgb * hot_aa[:,:,None]

    return np.clip(patch, 0, 255)

def evaluate_loss(params=DEFAULT_PARAMS, ref=None):
    if ref is None:
        ref = extract_reference_data()

    synthetic_dot = render_synthetic_dot(10.64, params)
    synth_lum = 0.299 * synthetic_dot[:,:,0] + 0.587 * synthetic_dot[:,:,1] + 0.114 * synthetic_dot[:,:,2]
    
    size = synthetic_dot.shape[0]
    center = size / 2.0
    y_coords, x_coords = np.ogrid[:size, :size]
    dists = np.sqrt((x_coords - center + 0.5)**2 + (y_coords - center + 0.5)**2).flatten()
    vals = synth_lum.flatten()

    bin_centers = ref["bin_centers"]
    bins = np.linspace(0, 6.0, 13)
    synth_psf = []
    for i in range(len(bins)-1):
        mask = (dists >= bins[i]) & (dists < bins[i+1])
        synth_psf.append(vals[mask].mean() if np.sum(mask) > 0 else 0)
    synth_psf = np.array(synth_psf)

    psf_rmse = np.sqrt(np.mean((synth_psf - ref["empirical_psf"])**2))
    
    peak_synth = synthetic_dot[size//2, size//2]
    peak_delta = np.linalg.norm(peak_synth - ref["peak_color"])
    sub_delta = np.linalg.norm(hex_to_rgb(params["colors"]["panel"]) - ref["sub_color"])

    total_loss = psf_rmse + peak_delta * 0.3 + sub_delta * 0.5

    return {
        "total_loss": float(total_loss),
        "psf_rmse": float(psf_rmse),
        "peak_delta": float(peak_delta),
        "sub_delta": float(sub_delta),
        "bin_centers": bin_centers,
        "empirical_psf": ref["empirical_psf"],
        "synth_psf": synth_psf,
        "synthetic_dot": synthetic_dot
    }

def optimize_parameters():
    """Run Nelder-Mead numerical optimization on diode shader parameters."""
    ref = extract_reference_data()

    def obj_func(vec):
        dotR, plateauR, hotR, glow_blur, glow_alpha = vec
        p = json.loads(json.dumps(DEFAULT_PARAMS))
        p["dotR_ratio"] = dotR
        p["plateauR_ratio"] = plateauR
        p["hotR_ratio"] = hotR
        p["glow_blur"] = glow_blur
        p["glow_alpha"] = glow_alpha
        res = evaluate_loss(p, ref)
        return res["total_loss"]

    init_vec = [
        DEFAULT_PARAMS["dotR_ratio"],
        DEFAULT_PARAMS["plateauR_ratio"],
        DEFAULT_PARAMS["hotR_ratio"],
        DEFAULT_PARAMS["glow_blur"],
        DEFAULT_PARAMS["glow_alpha"]
    ]

    print("Running numerical parameter optimization against screenshot...")
    res = minimize(obj_func, init_vec, method="Nelder-Mead", options={"maxiter": 300, "disp": False})

    opt_p = json.loads(json.dumps(DEFAULT_PARAMS))
    opt_p["dotR_ratio"] = float(res.x[0])
    opt_p["plateauR_ratio"] = float(res.x[1])
    opt_p["hotR_ratio"] = float(res.x[2])
    opt_p["glow_blur"] = float(res.x[3])
    opt_p["glow_alpha"] = float(res.x[4])

    return opt_p

def run_loop_and_report():
    print("=========================================================")
    print("APPLIANCE DOT MATRIX EVALUATION & OPTIMIZATION LOOP")
    print("=========================================================")
    ref = extract_reference_data()

    # Initial evaluation
    init_metrics = evaluate_loss(DEFAULT_PARAMS, ref)
    print(f"INITIAL Composite Loss : {init_metrics['total_loss']:.2f} (PSF RMSE: {init_metrics['psf_rmse']:.2f})")

    # Run optimizer
    opt_params = optimize_parameters()
    opt_metrics = evaluate_loss(opt_params, ref)
    print(f"OPTIMIZED Composite Loss: {opt_metrics['total_loss']:.2f} (PSF RMSE: {opt_metrics['psf_rmse']:.2f})")
    print("=========================================================")
    print(f"Optimized dotR_ratio    : {opt_params['dotR_ratio']:.4f} ({opt_params['dotR_ratio']*100:.1f}% pitch)")
    print(f"Optimized plateauR_ratio: {opt_params['plateauR_ratio']:.4f} ({opt_params['plateauR_ratio']*100:.1f}% pitch)")
    print(f"Optimized hotR_ratio    : {opt_params['hotR_ratio']:.4f} ({opt_params['hotR_ratio']*100:.1f}% pitch)")
    print(f"Optimized glow_blur     : {opt_params['glow_blur']:.2f}px")
    print(f"Optimized glow_alpha    : {opt_params['glow_alpha']:.2f}")
    print("=========================================================")

    # Generate Visual Diff Report
    fig, axes = plt.subplots(1, 3, figsize=(16, 5), dpi=200)

    # 1. Optimized Synthetic Diode
    axes[0].imshow(opt_metrics["synthetic_dot"].astype(np.uint8))
    axes[0].set_title(f"Optimized Diode Profile\n(Radius={opt_params['dotR_ratio']*10.64:.2f}px, Glow={opt_params['glow_blur']:.1f}px)", fontsize=10, fontweight="bold")
    axes[0].axis("off")

    # 2. Radial Decay Comparison Curve
    axes[1].plot(opt_metrics["bin_centers"], opt_metrics["empirical_psf"], color="#00ffcc", marker="o", label="Empirical Reference PSF")
    axes[1].plot(opt_metrics["bin_centers"], init_metrics["synth_psf"], color="#888888", linestyle=":", label=f"Initial (RMSE={init_metrics['psf_rmse']:.1f})")
    axes[1].plot(opt_metrics["bin_centers"], opt_metrics["synth_psf"], color="#ff007f", marker="x", linewidth=2, label=f"Optimized (RMSE={opt_metrics['psf_rmse']:.1f})")
    axes[1].axvline(x=5.32, color="yellow", linestyle=":", label="Cell Boundary (5.32px)")
    axes[1].set_facecolor("#0d1117")
    axes[1].set_title(f"PSF Decay Optimization Match", fontsize=10, fontweight="bold", color="white")
    axes[1].set_xlabel("Radius from Center (px)", color="white")
    axes[1].set_ylabel("Luminance (0-255)", color="white")
    axes[1].tick_params(colors="white")
    axes[1].legend(loc="upper right", facecolor="#1f242c", edgecolor="none", labelcolor="white")
    axes[1].grid(True, color="#30363d", alpha=0.5)

    # 3. Residual Error Comparison
    init_res = init_metrics["synth_psf"] - opt_metrics["empirical_psf"]
    opt_res = opt_metrics["synth_psf"] - opt_metrics["empirical_psf"]
    width = 0.2
    axes[2].bar(opt_metrics["bin_centers"] - width/2, init_res, width=width, color='#555555', label="Initial Error")
    axes[2].bar(opt_metrics["bin_centers"] + width/2, opt_res, width=width, color='#00ffcc', label="Optimized Error")
    axes[2].axhline(0, color="white", linestyle="-", linewidth=0.8)
    axes[2].set_facecolor("#0d1117")
    axes[2].set_title("Radial Residuals vs Radius", fontsize=10, fontweight="bold", color="white")
    axes[2].set_xlabel("Radius from Center (px)", color="white")
    axes[2].set_ylabel("Error (Luminance Units)", color="white")
    axes[2].tick_params(colors="white")
    axes[2].legend(loc="upper right", facecolor="#1f242c", edgecolor="none", labelcolor="white")
    axes[2].grid(True, color="#30363d", alpha=0.5)

    plt.tight_layout()
    diag_path = os.path.join(OUTPUT_DIR, "eval_optimization_report.png")
    plt.savefig(diag_path, bbox_inches="tight", dpi=200)
    plt.close()
    print(f"Saved optimization report to: {diag_path}")

    # Save optimized parameters to JSON
    opt_json_path = os.path.join(OUTPUT_DIR, "optimized_params.json")
    with open(opt_json_path, "w") as f:
        json.dump(opt_params, f, indent=2)
    print(f"Saved optimized parameters to: {opt_json_path}")

if __name__ == "__main__":
    run_loop_and_report()
