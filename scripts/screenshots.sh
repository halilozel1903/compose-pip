#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running emulator. The sample plays the bundled demo clips
# with Media3 ExoPlayer; every shot shows real decoded video and is taken only after the app logs that frames are
# on screen and playback has run for a few seconds. Every capture is also checked for the expected text and for a
# blank image, and the PiP shot is checked against the window bounds the app reports.
#
#   bash scripts/screenshots.sh phone    # pixel_7: the player, then the real PiP window over the launcher
#   bash scripts/screenshots.sh tablet   # pixel_tablet in landscape: the player with playlist and PiP panel
#   bash scripts/screenshots.sh tv       # Android TV 1080p: the full screen player
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

device="${1:-phone}"

install_sample
case "$device" in
  phone)
    capture_home
    fresh_launch --es scene player
    wait_for_scene player
    capture phone-player "Enter PiP"
    capture_pip phone-pip "$OUT/phone-player.png"
    ;;
  tablet)
    ensure_landscape
    fresh_launch --es scene player
    wait_for_scene player
    capture tablet-player "Enter PiP"
    ;;
  tv)
    fresh_launch --es scene player
    wait_for_scene player
    capture tv-player "NOW PLAYING"
    ;;
  *)
    echo "Unknown device $device (phone, tablet or tv)" >&2
    exit 1
    ;;
esac
