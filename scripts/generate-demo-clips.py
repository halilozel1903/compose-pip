#!/usr/bin/env python3
"""Generates the sample app's demo clips and their thumbnails.

The clips are drawn frame by frame with numpy (no footage, no third-party artwork) and encoded with ffmpeg as
H.264 MP4s that every Android device and emulator can decode in software:

    sample/src/main/assets/clips/{aurora,tide,lanterns}.mp4   1280x720, 30 fps, 10 s, no audio
    sample/src/main/res/drawable-nodpi/thumb_{name}.jpg       320x180 still from each clip

Usage (needs numpy and pillow; ffmpeg from PATH or from the imageio-ffmpeg package):

    python3 scripts/generate-demo-clips.py
"""
import io
import os
import shutil
import subprocess
import sys

import numpy as np
from PIL import Image

W, H, FPS, SECONDS = 1280, 720, 30, 10
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CLIPS = os.path.join(ROOT, "sample", "src", "main", "assets", "clips")
THUMBS = os.path.join(ROOT, "sample", "src", "main", "res", "drawable-nodpi")

ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
u = xs / W
v = ys / H


def ffmpeg():
    exe = shutil.which("ffmpeg")
    if exe:
        return exe
    try:
        import imageio_ffmpeg

        return imageio_ffmpeg.get_ffmpeg_exe()
    except ImportError:
        sys.exit("ffmpeg not found: install it or `pip install imageio-ffmpeg`.")


def lerp(a, b, t):
    t = np.clip(t, 0.0, 1.0)[..., None]
    return np.asarray(a, np.float32) * (1 - t) + np.asarray(b, np.float32) * t


def vertical(top, bottom):
    return lerp(top, bottom, v)


def glow(cx, cy, radius, color, strength=1.0):
    d = np.sqrt((xs - cx) ** 2 + (ys - cy) ** 2) / radius
    return np.exp(-d * d)[..., None] * np.asarray(color, np.float32) * strength


def add_glow(img, cx, cy, radius, color, strength=1.0, min_y=0):
    """Adds a soft glow in place, only within 3 radii of its center (and not above min_y)."""
    reach = radius * 3
    x0, x1 = max(0, int(cx - reach)), min(W, int(cx + reach) + 1)
    y0, y1 = max(int(min_y), int(cy - reach)), min(H, int(cy + reach) + 1)
    if x0 >= x1 or y0 >= y1:
        return
    d = np.sqrt((xs[y0:y1, x0:x1] - cx) ** 2 + (ys[y0:y1, x0:x1] - cy) ** 2) / radius
    img[y0:y1, x0:x1] += np.exp(-d * d)[..., None] * np.asarray(color, np.float32) * strength


rng = np.random.default_rng(7)
STARS = [(rng.uniform(0, W), rng.uniform(0, H * 0.6), rng.uniform(0.6, 1.8), rng.uniform(0, 6.28)) for _ in range(140)]


def stars(img, t, fade_to):
    for sx, sy, size, phase in STARS:
        if sy > fade_to:
            continue
        a = 0.55 + 0.45 * np.sin(t * 2.2 + phase)
        x0, y0 = int(sx), int(sy)
        r = int(np.ceil(size))
        img[max(0, y0 - r):y0 + r + 1, max(0, x0 - r):x0 + r + 1] += 140 * a * size / 1.8
    return img


def ridge(base, amp, freqs, phase=0.0):
    x = u * 6.28
    h = np.zeros(W, np.float32)
    for i, f in enumerate(freqs):
        h += np.sin(x[0] * f + phase + i * 1.7) / (i + 1)
    return base - amp * h


def aurora(t):
    img = vertical([4, 8, 28], [18, 34, 58])
    img = stars(img, t, H * 0.55)
    for k, (color, y0, speed) in enumerate([([40, 230, 150], 0.30, 0.35), ([120, 90, 255], 0.22, -0.25), ([30, 200, 220], 0.38, 0.5)]):
        wave = y0 + 0.07 * np.sin(u * 7.0 + t * speed * 3 + k) + 0.03 * np.sin(u * 17.0 - t * speed * 5)
        above = np.clip((wave - v) / 0.16, 0, 1)
        below = np.exp(-((v - wave) / 0.02) ** 2)
        band = np.where(v < wave, (1 - above) ** 1.5, below)
        rays = 0.72 + 0.28 * np.sin(u * 140 + np.sin(u * 9 + t) * 3 + k * 2)
        curtain = band * rays
        img += curtain[..., None] * np.asarray(color, np.float32) * 0.9
    far = ridge(H * 0.74, 34, [1.3, 3.1, 7.0], 0.4)
    near = ridge(H * 0.86, 46, [0.9, 2.3, 5.7], 2.0)
    img = np.where((ys > far[None, :])[..., None], lerp([16, 24, 44], [10, 14, 26], (v - 0.7) * 3), img)
    img = np.where((ys > near[None, :])[..., None], np.asarray([6, 8, 14], np.float32), img)
    return img


def tide(t):
    sky = vertical([38, 30, 92], [255, 150, 110])
    sun_y = H * (0.47 + 0.05 * t / SECONDS)
    sky += glow(W * 0.62, sun_y, 260, [255, 120, 60], 0.55)
    sun = (np.sqrt((xs - W * 0.62) ** 2 + (ys - sun_y) ** 2) < 70)[..., None]
    sky = np.where(sun, np.asarray([255, 226, 170], np.float32), sky)
    horizon = H * 0.58
    depth = np.clip((ys - horizon) / (H - horizon), 0, 1)
    sea = lerp([214, 112, 112], [24, 22, 70], depth ** 0.6)
    waves = np.sin(xs / (18 + 120 * depth) + t * 2.5 + ys * 0.08) * np.sin(xs * 0.011 - t * 1.3 + ys * 0.03)
    sea += (waves * 26 * (0.3 + depth))[..., None]
    shimmer = np.exp(-((xs - W * 0.62) / (40 + 160 * depth)) ** 2) * (0.5 + 0.5 * np.sin(ys * 0.9 - t * 9 + xs * 0.05))
    sea += (shimmer * 150 * (1 - depth * 0.6))[..., None] * np.asarray([1.0, 0.75, 0.5], np.float32)
    img = np.where((ys > horizon)[..., None], sea, sky)
    for k, (bx, by, s) in enumerate([(0.2, 0.25, 1.0), (0.27, 0.2, 0.8), (0.33, 0.27, 0.7)]):
        cx = W * ((bx + t * 0.025) % 1.1)
        cy = H * by + 6 * np.sin(t * 3 + k)
        d = np.abs(ys - (cy + np.abs(xs - cx) * 0.35 * s)) < 2.2 * s
        img = np.where((d & (np.abs(xs - cx) < 22 * s))[..., None], np.asarray([40, 26, 60], np.float32), img)
    return img


LANTERNS = [(rng.uniform(0, W), rng.uniform(0, 1), rng.uniform(0.5, 1.4), rng.uniform(0, 6.28)) for _ in range(46)]


def lanterns(t):
    img = vertical([10, 14, 46], [66, 40, 96])
    img = stars(img, t, H * 0.35)
    lake = H * 0.7
    for lx, start, depth, phase in LANTERNS:
        y = H * 0.72 - ((start + t * 0.035 * depth) % 1.0) * H * 0.85
        x = lx + 18 * np.sin(t * 0.8 + phase)
        r = 6 + 8 * depth
        add_glow(img, x, y, r * 3.2, [255, 150, 60], 0.55 * depth)
        add_glow(img, x, y, r, [255, 220, 150], 0.9)
        add_glow(img, x, 2 * lake - y, r * 2.5, [255, 150, 70], 0.35 * depth, min_y=lake + 1)
    hills = ridge(H * 0.69, 22, [1.1, 2.7, 6.3], 1.1)
    img = np.where(((ys > hills[None, :]) & (ys <= lake))[..., None], np.asarray([18, 14, 34], np.float32), img)
    ripple = 0.85 + 0.15 * np.sin(ys * 0.7 + t * 4 + np.sin(xs * 0.02) * 2)
    img = np.where((ys > lake)[..., None], img * 0.55 * ripple[..., None] + np.asarray([6, 8, 20], np.float32), img)
    return img


SCENES = {"aurora": aurora, "tide": tide, "lanterns": lanterns}


def render(name, fn):
    os.makedirs(CLIPS, exist_ok=True)
    os.makedirs(THUMBS, exist_ok=True)
    out = os.path.join(CLIPS, f"{name}.mp4")
    cmd = [
        ffmpeg(), "-y", "-loglevel", "error",
        "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-",
        "-c:v", "libx264", "-profile:v", "main", "-level", "3.1", "-pix_fmt", "yuv420p",
        "-preset", "slow", "-crf", "27", "-g", str(FPS), "-movflags", "+faststart",
        "-metadata", f"title={name.capitalize()} (generated demo clip)", out,
    ]
    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE)
    thumb = None
    for i in range(FPS * SECONDS):
        frame = np.clip(fn(i / FPS), 0, 255).astype(np.uint8)
        if i == FPS * 4:
            thumb = frame
        proc.stdin.write(frame.tobytes())
    proc.stdin.close()
    if proc.wait() != 0:
        sys.exit(f"ffmpeg failed for {name}")
    buf = io.BytesIO()
    Image.fromarray(thumb).resize((320, 180), Image.LANCZOS).save(buf, "JPEG", quality=86)
    with open(os.path.join(THUMBS, f"thumb_{name}.jpg"), "wb") as f:
        f.write(buf.getvalue())
    print(f"{out}: {os.path.getsize(out) // 1024} KB")


if __name__ == "__main__":
    for scene_name, scene in SCENES.items():
        render(scene_name, scene)
