#!/usr/bin/env python3
"""Checks that a screenshot really shows the sample floating in a picture-in-picture window.

    python3 scripts/verify-pip.py <pip.png> <left> <top> <right> <bottom> <fullscreen.png> [home.png]

<left> .. <bottom> are the PiP window bounds the app reported. The check fails when the window is not PiP sized,
when it does not contain video (many colors, not black), or when the screenshot looks like the full screen player.
With the bare home screen, it also reports how much of the change from it lies inside the window. Needs Pillow.
"""
import sys

from PIL import Image, ImageChops, ImageStat


def fail(message):
    sys.exit(f"PiP check failed: {message}")


def main():
    if len(sys.argv) < 7:
        fail(__doc__)
    path = sys.argv[1]
    left, top, right, bottom = (int(v) for v in sys.argv[2:6])
    fullscreen_path = sys.argv[6]
    home_path = sys.argv[7] if len(sys.argv) > 7 else None

    shot = Image.open(path).convert("RGB")
    width, height = shot.size
    w, h = right - left, bottom - top
    if w <= 0 or h <= 0 or left < -2 or top < -2 or right > width + 2 or bottom > height + 2:
        fail(f"bounds {left},{top},{right},{bottom} are not inside the {width}x{height} screen")
    share = (w * h) / (width * height)
    if not 0.005 <= share <= 0.45:
        fail(f"the window covers {share:.1%} of the screen; that is not a PiP window")
    print(f"PiP window {w}x{h} at {left},{top}: {share:.1%} of the screen")

    # Inside: decoded video, many colors and not black. Inset to skip rounded corners and the shadow.
    inset = max(4, min(w, h) // 20)
    window = shot.crop((max(0, left + inset), max(0, top + inset), min(width, right - inset), min(height, bottom - inset)))
    small = window.resize((160, max(1, round(160 * window.height / window.width))))
    raw = small.tobytes()
    pixels = [raw[i:i + 3] for i in range(0, len(raw), 3)]
    colors = len(set(pixels))
    lit = sum(1 for p in pixels if max(p) > 40) / len(pixels)
    if colors < 150 or lit < 0.08:
        fail(f"the window does not look like video ({colors} colors, {lit:.0%} lit pixels)")
    print(f"Window content: {colors} colors, {lit:.0%} lit pixels")

    # Outside: not the full screen player any more.
    fullscreen = Image.open(fullscreen_path).convert("RGB").resize((width, height))
    difference = ImageStat.Stat(ImageChops.difference(shot.resize((216, 480)), fullscreen.resize((216, 480)))).mean
    mean_difference = sum(difference) / 3
    if mean_difference < 10:
        fail(f"the screenshot is too close to the full screen player (mean difference {mean_difference:.1f})")
    print(f"Differs from the full screen player by {mean_difference:.1f} on average")

    if home_path:
        try:
            home = Image.open(home_path).convert("RGB").resize((width, height))
        except OSError as error:
            print(f"Warning: no home screen to compare with ({error})", file=sys.stderr)
            return
        changed = ImageChops.difference(shot, home).convert("L").point(lambda v: 255 if v > 48 else 0)
        total = changed.size[0] * changed.size[1] - changed.histogram()[0]
        box = (max(0, left - 24), max(0, top - 24), min(width, right + 24), min(height, bottom + 24))
        crop = changed.crop(box)
        inside = crop.size[0] * crop.size[1] - crop.histogram()[0]
        if total:
            print(f"Changes from the home screen: {inside / total:.0%} inside the PiP window")
        else:
            print("Warning: no change from the home screen at all", file=sys.stderr)


if __name__ == "__main__":
    main()
