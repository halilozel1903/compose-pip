package io.github.halilozel1903.pip.sample

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import io.github.halilozel1903.pip.PipState
import kotlinx.coroutines.delay

/** Logcat tag for `scripts/screenshots.sh`. */
const val SCENE_TAG = "PipSample"

/**
 * Logs `SCENE_READY player video left top right bottom` once the player has rendered video frames and played for
 * a few seconds, so the screenshot shows real decoded video, or `SCENE_FAILED player: <reason>` on a playback error
 * or timeout. The bounds are where the video picture sits on screen (the PiP source rect hint, moved by the
 * window's position); the screenshot script checks that the pixels there are video. They are left out when the
 * picture has no bounds yet.
 */
@Composable
fun ReportPlaybackScene(player: Player, pip: PipState) {
    val view = LocalView.current
    var firstFrame by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrame = true
            }

            override fun onPlayerError(e: PlaybackException) {
                error = "error ${e.errorCode}: ${e.message}"
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(player) {
        val start = System.currentTimeMillis()
        while (true) {
            error?.let {
                Log.e(SCENE_TAG, "SCENE_FAILED player: $it")
                return@LaunchedEffect
            }
            if (firstFrame && player.isPlaying && player.currentPosition >= 2_500) {
                delay(800)
                withFrameNanos { }
                val video = pip.sourceRectHint?.takeUnless { it.isEmpty }?.let {
                    val window = IntArray(2)
                    view.rootView.getLocationOnScreen(window)
                    " video ${window[0] + it.left} ${window[1] + it.top} ${window[0] + it.right} ${window[1] + it.bottom}"
                }
                Log.i(SCENE_TAG, "SCENE_READY player${video.orEmpty()}")
                return@LaunchedEffect
            }
            if (System.currentTimeMillis() - start > 90_000) {
                Log.e(SCENE_TAG, "SCENE_FAILED player: no video frame after 90 s (first frame: $firstFrame)")
                return@LaunchedEffect
            }
            delay(250)
        }
    }
}

/**
 * Logs `PIP_MODE true|false` on every change, and in PiP `PIP_BOUNDS left top right bottom`: where the PiP window
 * sits on screen once the enter animation has settled. The screenshot script checks the capture against them.
 */
@Composable
fun ReportPipMode(pip: PipState) {
    val view = LocalView.current
    LaunchedEffect(pip.isInPip) {
        Log.i(SCENE_TAG, "PIP_MODE ${pip.isInPip} (auto-enter: ${pip.autoEnterStrategy})")
        if (pip.isInPip) {
            delay(2_500)
            withFrameNanos { }
            val root = view.rootView
            val location = IntArray(2)
            root.getLocationOnScreen(location)
            Log.i(
                SCENE_TAG,
                "PIP_BOUNDS ${location[0]} ${location[1]} ${location[0] + root.width} ${location[1] + root.height}",
            )
        }
    }
}
