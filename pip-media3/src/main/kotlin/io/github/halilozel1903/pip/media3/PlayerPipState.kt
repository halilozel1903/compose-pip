package io.github.halilozel1903.pip.media3

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.Player
import io.github.halilozel1903.pip.PipActions
import io.github.halilozel1903.pip.PipState
import io.github.halilozel1903.pip.core.MediaPipActionLayout
import io.github.halilozel1903.pip.core.MediaPipActionPlanner
import io.github.halilozel1903.pip.core.PipAspectRatio
import io.github.halilozel1903.pip.rememberMaxPipActions
import io.github.halilozel1903.pip.rememberPipState

/**
 * Picture-in-picture for a Media3 [Player], in one call:
 *
 * - the window takes the video's aspect ratio (with its pixel aspect ratio) as soon as the size is known,
 * - auto-enter is on only while the player is playing, so a paused video never floats over the launcher,
 * - the PiP window shows previous / play-pause / next (or rewind / play-pause / fast forward) wired to the player,
 *   with disabled buttons kept in place and play/pause alone when the device allows fewer than three actions,
 * - the player pauses when the activity stops, which is also what happens when the user closes the PiP window.
 *
 * Attach `Modifier.pipSourceRect(state)` to your video surface, or use [PipPlayerSurface], which does it.
 *
 * @param autoEnterWhilePlaying enter PiP when the user leaves the app while the player is playing.
 * @param actionLayout which actions sit around play/pause.
 * @param pauseOnStop pause when the activity stops: the PiP window was closed, or the user left without PiP.
 * @param title defaults to the current item's `mediaMetadata.title`.
 */
@Composable
public fun rememberPlayerPipState(
    player: Player,
    autoEnterWhilePlaying: Boolean = true,
    actionLayout: MediaPipActionLayout = MediaPipActionLayout.Skip,
    seamlessResize: Boolean = true,
    fallbackAspectRatio: PipAspectRatio = PipAspectRatio.Widescreen,
    pauseOnStop: Boolean = true,
    title: String? = null,
    subtitle: String? = null,
): PipState {
    val snapshot = rememberPlayerPipSnapshot(player)
    val maxActions = rememberMaxPipActions()
    val actions = remember(snapshot, actionLayout, maxActions) {
        MediaPipActionPlanner.plan(
            isPlaying = snapshot.isPlaying,
            isEnded = snapshot.isEnded,
            canSkipPrevious = snapshot.canSkipPrevious,
            canSkipNext = snapshot.canSkipNext,
            canSeek = snapshot.canSeek,
            layout = actionLayout,
            maxActions = maxActions,
        ).map { PipActions.of(it) }
    }
    val state = rememberPipState(
        aspectRatio = snapshot.aspectRatio ?: fallbackAspectRatio,
        autoEnter = autoEnterWhilePlaying && snapshot.isPlaying,
        seamlessResize = seamlessResize,
        actions = actions,
        title = title ?: snapshot.title,
        subtitle = subtitle,
        onAction = { action -> PipActions.mediaActionOf(action)?.let(player::perform) },
    )
    if (pauseOnStop) PauseOnStop(player)
    return state
}

@Composable
private fun PauseOnStop(player: Player) {
    val activity = LocalContext.current.findComponentActivity()
    DisposableEffect(activity, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> error("Picture-in-picture needs a ComponentActivity, but this composition is hosted in $this")
}
