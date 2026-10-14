package io.github.halilozel1903.pip.sample

import android.content.res.Configuration
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import io.github.halilozel1903.pip.PipState
import io.github.halilozel1903.pip.core.MediaPipActionLayout
import io.github.halilozel1903.pip.media3.rememberPlayerPipState
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay

/** The PiP options the user can change in the sample. */
class PipOptions(
    val autoEnter: Boolean,
    val seamlessResize: Boolean,
    val actionLayout: MediaPipActionLayout,
    val onAutoEnterChange: (Boolean) -> Unit,
    val onSeamlessResizeChange: (Boolean) -> Unit,
    val onActionLayoutChange: (MediaPipActionLayout) -> Unit,
)

/** What is playing, updated on every player event. */
data class NowPlaying(
    val index: Int,
    val isPlaying: Boolean,
    val videoWidth: Int,
    val videoHeight: Int,
) {
    val clip: DemoClip get() = DemoClips.all[index.coerceIn(0, DemoClips.all.lastIndex)]
}

private enum class LayoutKind { Phone, Tablet, Tv }

@Composable
fun PlayerScreen(player: Player, pipRequests: ReceiveChannel<Unit>, reportScene: Boolean) {
    var autoEnter by rememberSaveable { mutableStateOf(true) }
    var seamlessResize by rememberSaveable { mutableStateOf(true) }
    var actionLayout by rememberSaveable { mutableStateOf(MediaPipActionLayout.Skip) }

    // Aspect ratio from the video, auto-enter while playing, previous / play-pause / next in the PiP window.
    val pip = rememberPlayerPipState(
        player = player,
        autoEnterWhilePlaying = autoEnter,
        actionLayout = actionLayout,
        seamlessResize = seamlessResize,
        subtitle = "compose-pip sample",
    )
    val nowPlaying = rememberNowPlaying(player)
    val options = PipOptions(
        autoEnter = autoEnter,
        seamlessResize = seamlessResize,
        actionLayout = actionLayout,
        onAutoEnterChange = { autoEnter = it },
        onSeamlessResizeChange = { seamlessResize = it },
        onActionLayoutChange = { actionLayout = it },
    )

    LaunchedEffect(pip) {
        for (request in pipRequests) {
            // Let the activity finish resuming after onNewIntent before asking for PiP.
            delay(600)
            pip.enter()
        }
    }
    ReportPipMode(pip)
    if (reportScene) ReportPlaybackScene(player, pip)

    val isTv = (LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION
    // The PiP window is small, so its size says nothing about the device: keep the layout chosen full screen.
    // Switching layouts would also recreate the video surface in the middle of the PiP transition.
    val lastLayout = remember { arrayOfNulls<LayoutKind>(1) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val measured = when {
            isTv -> LayoutKind.Tv
            maxWidth >= 840.dp && maxWidth > maxHeight -> LayoutKind.Tablet
            else -> LayoutKind.Phone
        }
        val layout = if (pip.isInPip) lastLayout[0] ?: measured else measured.also { lastLayout[0] = it }
        when (layout) {
            LayoutKind.Tv -> TvPlayerLayout(player, pip, nowPlaying, options)
            LayoutKind.Tablet -> TabletPlayerLayout(player, pip, nowPlaying, options)
            LayoutKind.Phone -> PhonePlayerLayout(player, pip, nowPlaying, options)
        }
    }
}

@Composable
fun rememberNowPlaying(player: Player): NowPlaying {
    fun read() = NowPlaying(
        index = player.currentMediaItemIndex,
        isPlaying = player.isPlaying,
        videoWidth = player.videoSize.width,
        videoHeight = player.videoSize.height,
    )
    var nowPlaying by remember(player) { mutableStateOf(read()) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                nowPlaying = read()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    return nowPlaying
}

/** Playback progress of the current clip, 0 to 1. */
@Composable
fun rememberProgress(player: Player): Float {
    var progress by remember(player) { mutableStateOf(0f) }
    LaunchedEffect(player) {
        while (true) {
            val duration = player.duration
            progress = if (duration > 0) (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
            delay(200)
        }
    }
    return progress
}

/** A one-line description of the live PiP configuration, for the status panels. */
fun PipState.describeActions(): String {
    val actions = config.actions
    if (actions.isEmpty()) return "None"
    return actions.joinToString(" · ") { if (it.enabled) it.title else "${it.title} (off)" }
}
