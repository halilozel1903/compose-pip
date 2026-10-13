#!/usr/bin/env bash
# Shared helpers for scripts/screenshots.sh. Expects PKG to be set by the caller's environment
# or falls back to the sample's application id from sample/build.gradle.kts.
PKG="${PKG:-$(grep -oE 'applicationId = "[^"]+"' sample/build.gradle.kts | cut -d'"' -f2)}"
OUT="docs/screenshots"
WORK="${RUNNER_TEMP:-/tmp}/pip-screenshots"
mkdir -p "$OUT" "$WORK"

# The sample logs under this tag: "SCENE_READY player" once decoded video is on screen, "SCENE_FAILED player: ..."
# on a playback error, "PIP_MODE true|false" on every PiP change and "PIP_BOUNDS l t r b" once the PiP window settled.
SCENE_TAG="${SCENE_TAG:-PipSample}"

# Full Wi-Fi and no mobile signal in the demo status bar. Sent as separate commands, and again after a rotation,
# because the tablet's System UI rebuilds its status bar and dropped the Wi-Fi icon otherwise.
demo_network() {
  adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile hide > /dev/null
  # "fully" must come with the Wi-Fi command, or the icon shows the no-internet mark.
  adb shell am broadcast -a com.android.systemui.demo -e command network -e fully true -e wifi show -e level 4 > /dev/null
}

install_sample() {
  adb install -r sample/build/outputs/apk/debug/sample-debug.apk
  # A freshly booted emulator often shows "Pixel Launcher isn't responding".
  # Hide error/ANR dialogs and give the system time to settle.
  adb shell settings put global hide_error_dialogs 1
  adb shell settings put global anr_show_background 0
  sleep 20
  dismiss_system_dialogs
  # Clean status bar via System UI demo mode.
  adb shell settings put global sysui_demo_allowed 1
  adb shell am broadcast -a com.android.systemui.demo -e command enter
  adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941
  adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
  demo_network
  adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
}

# The emulator runner unlocks the screen with a key press, which leaves the device out of touch mode, so
# Compose would draw keyboard focus highlights. A tap on the empty home screen puts it back in touch mode.
# Android TV has no touch screen and shows focus by design, so it is left alone.
enter_touch_mode() {
  local size w h
  if adb shell pm has-feature android.software.leanback | grep -q true; then
    return 0
  fi
  adb shell input keyevent KEYCODE_HOME
  sleep 1
  size="$(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1 | tr -d '\r')"
  w="${size%x*}"
  h="${size#*x}"
  adb shell input tap "$((w / 2))" "$((h * 2 / 5))"
  sleep 1
}

fresh_launch() {
  adb shell pm clear "$PKG" > /dev/null
  enter_touch_mode
  adb logcat -c || true
  adb shell am start -W -n "$PKG/.MainActivity" "$@" > /dev/null
}

# Prints the last line of the sample's log that contains $1, waiting up to $2 seconds. Fails the run on SCENE_FAILED.
wait_for_log() {
  local pattern="$1" timeout="${2:-60}" log=""
  for _ in $(seq 1 "$timeout"); do
    log="$(adb logcat -d -s "$SCENE_TAG:*" 2> /dev/null || true)"
    if printf '%s' "$log" | grep -q "SCENE_FAILED"; then
      echo "The sample reported a failure:" >&2
      printf '%s\n' "$log" | grep "SCENE_FAILED" >&2
      exit 1
    fi
    if printf '%s' "$log" | grep -q "$pattern"; then
      printf '%s\n' "$log" | grep "$pattern" | tail -1
      return 0
    fi
    sleep 1
  done
  return 1
}

# Waits until the player scene shows decoded video and has played for a few seconds.
wait_for_scene() {
  local scene="$1"
  if ! wait_for_log "SCENE_READY $scene" 150 > /dev/null; then
    echo "The $scene scene did not finish loading within 150 s." >&2
    adb logcat -d -t 200 >&2 || true
    exit 1
  fi
  echo "Scene $scene is ready"
  sleep 1
}

dismiss_system_dialogs() {
  for _ in 1 2 3; do
    if adb shell dumpsys window | grep -qiE "Application Not Responding|isn't responding"; then
      adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null
      adb shell input keyevent KEYCODE_ENTER
      sleep 2
    else
      return 0
    fi
  done
  if adb shell dumpsys window | grep -qiE "Application Not Responding|isn't responding"; then
    echo "A system dialog is still on screen; refusing to capture a broken screenshot." >&2
    exit 1
  fi
}

# Fails when the sample is not in front or the expected text is missing from the screen.
# A failing UI dump (it happens on busy emulators) only warns; the image checks below still run.
expect_on_screen() {
  local text="$1"
  local resumed
  resumed="$(adb shell dumpsys activity activities | grep -E "ResumedActivity" || true)"
  if [ -n "$resumed" ] && ! printf '%s' "$resumed" | grep -q "$PKG"; then
    echo "The sample app is not in front; refusing to capture." >&2
    exit 1
  fi
  local xml=""
  for _ in 1 2 3; do
    if adb shell uiautomator dump /sdcard/window.xml > /dev/null 2>&1; then
      xml="$(adb exec-out cat /sdcard/window.xml)"
      break
    fi
    sleep 2
  done
  if [ -z "$xml" ]; then
    echo "Warning: could not dump the UI to look for \"$text\"." >&2
  elif ! printf '%s' "$xml" | grep -qF "$text"; then
    echo "\"$text\" is not on screen; the scene did not appear." >&2
    exit 1
  fi
}

# Fails on a missing, tiny, blank or single colored PNG.
check_png() {
  python3 - "$1" <<'PY'
import struct, sys, zlib

path = sys.argv[1]
with open(path, "rb") as f:
    data = f.read()
if len(data) < 12_000 or not data.startswith(b"\x89PNG\r\n\x1a\n"):
    sys.exit(f"{path}: not a PNG or too small ({len(data)} bytes)")
width, height = struct.unpack(">II", data[16:24])
idat, pos = b"", 8
while pos < len(data):
    length, kind = struct.unpack(">I4s", data[pos:pos + 8])
    if kind == b"IDAT":
        idat += data[pos + 8:pos + 8 + length]
    pos += 12 + length
raw = zlib.decompress(idat)
# Filtered scanlines of a blank screen are almost all zeros; real content has many different bytes.
distinct = len(set(raw[::97]))
nonzero = sum(1 for b in raw[::211] if b) / max(1, len(raw[::211]))
if width < 300 or height < 300 or distinct < 24 or (nonzero < 0.01 and distinct < 120):
    sys.exit(f"{path}: looks blank ({width}x{height}, {distinct} distinct samples, {nonzero:.3f} non-zero)")
print(f"{path}: {width}x{height}, {len(data) // 1024} KB, {distinct} distinct samples")
PY
}

capture() {
  local name="$1" expected="$2"
  local file="${OUT:?}/${name:?}.png"
  dismiss_system_dialogs
  # Capture right away and check the screen afterwards: the UI dump takes seconds, and waiting for it let the
  # 10 second clip roll over to the next one, whose first frames were not drawn yet (a black video area).
  adb exec-out screencap -p > "$file"
  expect_on_screen "$expected"
  if ! check_png "$file"; then
    rm -f "$file"
    exit 1
  fi
  echo "Captured $name"
}

# Saves the bare home screen, so the PiP capture can be compared with it.
capture_home() {
  adb shell input keyevent KEYCODE_HOME
  sleep 4
  dismiss_system_dialogs
  adb exec-out screencap -p > "$WORK/home.png"
}

# Sends the playing sample home and captures the real picture-in-picture window floating over the launcher.
# Home with auto-enter (setAutoEnterEnabled on Android 12+) is tried first; if the system does not enter PiP,
# the app is brought back and asked to enter PiP itself (the same call as its PiP button). Either way the shot is
# only kept when the app reports PiP mode, the window bounds are PiP sized and the pixels there are video.
capture_pip() {
  local name="$1" fullscreen="$2" line bounds
  local file="${OUT:?}/${name:?}.png"
  adb logcat -c || true
  adb shell input keyevent KEYCODE_HOME
  if wait_for_log "PIP_MODE true" 20 > /dev/null; then
    echo "Entered PiP by auto-enter on Home"
  else
    echo "Auto-enter did not start PiP; entering through the sample's enter_pip extra."
    adb logcat -c || true
    adb shell am start -n "$PKG/.MainActivity" --ez enter_pip true > /dev/null
    if ! wait_for_log "PIP_MODE true" 40 > /dev/null; then
      echo "The sample never entered picture-in-picture; no PiP screenshot." >&2
      adb logcat -d -s "$SCENE_TAG:*" >&2 || true
      exit 1
    fi
    echo "Entered PiP from the app"
  fi
  if ! line="$(wait_for_log "PIP_BOUNDS" 40)"; then
    echo "The sample did not report its PiP window bounds." >&2
    exit 1
  fi
  bounds="$(printf '%s' "$line" | grep -oE 'PIP_BOUNDS -?[0-9]+ -?[0-9]+ -?[0-9]+ -?[0-9]+' | cut -d' ' -f2-)"
  echo "PiP window bounds: $bounds"
  if adb shell dumpsys activity activities | grep -E "mode=pinned" | grep -q "$PKG"; then
    echo "dumpsys: the sample's task is pinned"
  else
    echo "Warning: dumpsys did not show a pinned task for $PKG (format differs on this image); relying on the app's PiP callback." >&2
  fi
  sleep 2
  dismiss_system_dialogs
  adb exec-out screencap -p > "$file"
  # shellcheck disable=SC2086
  if ! python3 scripts/verify-pip.py "$file" $bounds "$fullscreen" "$WORK/home.png"; then
    rm -f "$file"
    exit 1
  fi
  echo "Captured $name"
}

# Locks the display in landscape (tablet shots). The Pixel Tablet's natural orientation is landscape, but some
# system images boot in portrait, so try both rotations and check the current display size.
ensure_landscape() {
  adb shell settings put system accelerometer_rotation 0
  local rotation size w h
  for rotation in 0 1; do
    adb shell settings put system user_rotation "$rotation"
    sleep 3
    size="$(adb shell dumpsys window displays | grep -oE 'cur=[0-9]+x[0-9]+' | head -1 | cut -d= -f2 | tr -d '\r' || true)"
    if [ -z "$size" ]; then
      echo "Warning: could not read the display size; keeping rotation $rotation." >&2
      return 0
    fi
    w="${size%x*}"
    h="${size#*x}"
    if [ "$w" -gt "$h" ]; then
      echo "Display is landscape ($size, rotation $rotation)"
      demo_network
      sleep 1
      return 0
    fi
  done
  echo "Could not put the display in landscape ($size)." >&2
  exit 1
}
