#!/usr/bin/env python3
"""Checks that a region of a screenshot shows decoded video: many colors and not black.

    python3 scripts/video_check.py <shot.png> <left> <top> <right> <bottom>

Used by scripts/screenshots.sh on the player shots (the bounds come from the sample's SCENE_READY line) and, as a
module, by scripts/verify-pip.py on the PiP window. Exits non-zero when the region does not look like video.
Needs Pillow.
"""
import sys

from PIL import Image

MIN_COLORS = 150
MIN_LIT = 0.08


def video_content(shot, left, top, right, bottom):
    """Returns (colors, lit share) of the region, inset to skip rounded corners, shadows and overlay edges."""
    width, height = shot.size
    inset = max(4, min(right - left, bottom - top) // 20)
    window = shot.crop((max(0, left + inset), max(0, top + inset), min(width, right - inset), min(height, bottom - inset)))
    small = window.resize((160, max(1, round(160 * window.height / window.width))))
    raw = small.tobytes()
    pixels = [raw[i:i + 3] for i in range(0, len(raw), 3)]
    colors = len(set(pixels))
    lit = sum(1 for p in pixels if max(p) > 40) / len(pixels)
    return colors, lit


def looks_like_video(colors, lit):
    return colors >= MIN_COLORS and lit >= MIN_LIT


def main():
    if len(sys.argv) != 6:
        sys.exit(__doc__)
    shot = Image.open(sys.argv[1]).convert("RGB")
    left, top, right, bottom = (int(v) for v in sys.argv[2:6])
    if right - left < 40 or bottom - top < 40:
        sys.exit(f"Video check failed: bounds {left},{top},{right},{bottom} are too small")
    colors, lit = video_content(shot, left, top, right, bottom)
    if not looks_like_video(colors, lit):
        sys.exit(f"Video check failed: the video area does not look like video ({colors} colors, {lit:.0%} lit pixels)")
    print(f"Video area: {colors} colors, {lit:.0%} lit pixels")


if __name__ == "__main__":
    main()
