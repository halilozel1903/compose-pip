package io.github.halilozel1903.pip.core

/** A media control that can be shown as a custom action of a picture-in-picture window. */
public enum class MediaPipAction {
    SkipPrevious,
    SeekBack,
    Play,
    Pause,
    Replay,
    SeekForward,
    SkipNext,
}

/** An action of a [MediaPipActionPlanner.plan]: shown greyed out when not [enabled], so the layout stays put. */
public data class PlannedPipAction(
    public val action: MediaPipAction,
    public val enabled: Boolean = true,
)

/** Which actions sit on either side of play/pause. */
public enum class MediaPipActionLayout {
    /** Previous and next item: playlists, episodes. */
    Skip,

    /** Seek back and forward: a single long video. */
    Seek,

    /** Play/pause only. */
    PlayPause,

    /** No custom actions. The system still shows its own expand and close buttons. */
    None,
}

/**
 * Decides which media actions a PiP window shows. Android limits the number of custom actions
 * (`Activity.getMaxNumPictureInPictureActions()`, 3 on most phones), so the plan puts play/pause in the middle,
 * keeps the side actions in fixed places and drops them first when there is no room.
 */
public object MediaPipActionPlanner {

    /**
     * Plans the actions for a player that [isPlaying] (or has [isEnded]), can or cannot skip to the previous and
     * next item and can or cannot seek, in [layout], with room for at most [maxActions].
     */
    public fun plan(
        isPlaying: Boolean,
        isEnded: Boolean = false,
        canSkipPrevious: Boolean = false,
        canSkipNext: Boolean = false,
        canSeek: Boolean = true,
        layout: MediaPipActionLayout = MediaPipActionLayout.Skip,
        maxActions: Int = 3,
    ): List<PlannedPipAction> {
        if (layout == MediaPipActionLayout.None || maxActions <= 0) return emptyList()
        val center = PlannedPipAction(
            when {
                isEnded -> MediaPipAction.Replay
                isPlaying -> MediaPipAction.Pause
                else -> MediaPipAction.Play
            },
        )
        if (layout == MediaPipActionLayout.PlayPause || maxActions < 3) return listOf(center)
        return when (layout) {
            MediaPipActionLayout.Skip -> listOf(
                PlannedPipAction(MediaPipAction.SkipPrevious, canSkipPrevious),
                center,
                PlannedPipAction(MediaPipAction.SkipNext, canSkipNext),
            )
            else -> listOf(
                PlannedPipAction(MediaPipAction.SeekBack, canSeek && !isEnded),
                center,
                PlannedPipAction(MediaPipAction.SeekForward, canSeek && !isEnded),
            )
        }
    }
}
