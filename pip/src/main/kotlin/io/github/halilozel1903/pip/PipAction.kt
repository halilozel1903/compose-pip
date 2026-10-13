package io.github.halilozel1903.pip

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import io.github.halilozel1903.pip.core.MediaPipAction
import io.github.halilozel1903.pip.core.PlannedPipAction

/**
 * A custom action of the picture-in-picture window, shown as an icon button over the video (and in the PiP menu
 * on TV). It becomes a `RemoteAction` whose `PendingIntent` is delivered back to the app by [PipActionReceiver].
 *
 * @property id identifies the action when it is tapped; unique within one window.
 * @property icon a drawable resource, tinted white by the system. Vector drawables work.
 * @property title the label (used by accessibility services and the TV PiP menu).
 * @property enabled a disabled action is shown greyed out and cannot be tapped.
 */
@Immutable
public data class PipAction(
    public val id: String,
    @param:DrawableRes public val icon: Int,
    public val title: String,
    public val contentDescription: String = title,
    public val enabled: Boolean = true,
)

/** Ready-made [PipAction]s for media, with icons from this library. */
public object PipActions {
    public const val PLAY: String = "pip.play"
    public const val PAUSE: String = "pip.pause"
    public const val REPLAY: String = "pip.replay"
    public const val SKIP_PREVIOUS: String = "pip.skip_previous"
    public const val SKIP_NEXT: String = "pip.skip_next"
    public const val SEEK_BACK: String = "pip.seek_back"
    public const val SEEK_FORWARD: String = "pip.seek_forward"

    public fun play(title: String = "Play", enabled: Boolean = true): PipAction =
        PipAction(PLAY, R.drawable.pip_ic_play, title, enabled = enabled)

    public fun pause(title: String = "Pause", enabled: Boolean = true): PipAction =
        PipAction(PAUSE, R.drawable.pip_ic_pause, title, enabled = enabled)

    /** [pause] while playing, [play] otherwise. */
    public fun playPause(isPlaying: Boolean): PipAction = if (isPlaying) pause() else play()

    public fun replay(title: String = "Replay", enabled: Boolean = true): PipAction =
        PipAction(REPLAY, R.drawable.pip_ic_replay, title, enabled = enabled)

    public fun skipPrevious(title: String = "Previous", enabled: Boolean = true): PipAction =
        PipAction(SKIP_PREVIOUS, R.drawable.pip_ic_skip_previous, title, enabled = enabled)

    public fun skipNext(title: String = "Next", enabled: Boolean = true): PipAction =
        PipAction(SKIP_NEXT, R.drawable.pip_ic_skip_next, title, enabled = enabled)

    public fun seekBack(title: String = "Rewind", enabled: Boolean = true): PipAction =
        PipAction(SEEK_BACK, R.drawable.pip_ic_seek_back, title, enabled = enabled)

    public fun seekForward(title: String = "Fast forward", enabled: Boolean = true): PipAction =
        PipAction(SEEK_FORWARD, R.drawable.pip_ic_seek_forward, title, enabled = enabled)

    /** The action for a media control, as planned by `MediaPipActionPlanner`. */
    public fun of(action: MediaPipAction, enabled: Boolean = true): PipAction = when (action) {
        MediaPipAction.Play -> play(enabled = enabled)
        MediaPipAction.Pause -> pause(enabled = enabled)
        MediaPipAction.Replay -> replay(enabled = enabled)
        MediaPipAction.SkipPrevious -> skipPrevious(enabled = enabled)
        MediaPipAction.SkipNext -> skipNext(enabled = enabled)
        MediaPipAction.SeekBack -> seekBack(enabled = enabled)
        MediaPipAction.SeekForward -> seekForward(enabled = enabled)
    }

    /** The action for a planned media control. */
    public fun of(planned: PlannedPipAction): PipAction = of(planned.action, planned.enabled)

    /** The media control a [PipAction] made by this object stands for, or `null` for a custom action. */
    public fun mediaActionOf(action: PipAction): MediaPipAction? = when (action.id) {
        PLAY -> MediaPipAction.Play
        PAUSE -> MediaPipAction.Pause
        REPLAY -> MediaPipAction.Replay
        SKIP_PREVIOUS -> MediaPipAction.SkipPrevious
        SKIP_NEXT -> MediaPipAction.SkipNext
        SEEK_BACK -> MediaPipAction.SeekBack
        SEEK_FORWARD -> MediaPipAction.SeekForward
        else -> null
    }
}
