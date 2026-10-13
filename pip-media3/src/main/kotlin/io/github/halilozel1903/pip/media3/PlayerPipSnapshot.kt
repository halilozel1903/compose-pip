package io.github.halilozel1903.pip.media3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import io.github.halilozel1903.pip.core.MediaPipAction
import io.github.halilozel1903.pip.core.PipAspectRatio

/** What picture-in-picture needs to know about a [Player], read in one go. */
@Immutable
public data class PlayerPipSnapshot(
    public val isPlaying: Boolean,
    public val isEnded: Boolean,
    public val canSkipPrevious: Boolean,
    public val canSkipNext: Boolean,
    public val canSeek: Boolean,
    /** The video's display aspect ratio, clamped to PiP limits; `null` until the first video size is known. */
    public val aspectRatio: PipAspectRatio?,
    /** The video's display aspect ratio, not clamped, for laying out the surface; `null` while unknown. */
    public val videoAspectRatio: Float?,
    /** `mediaMetadata.title` of the current item. */
    public val title: String?,
)

/** Reads a [PlayerPipSnapshot] of this player now. */
public fun Player.pipSnapshot(): PlayerPipSnapshot {
    val size = videoSize
    val pixelRatio = size.pixelWidthHeightRatio.takeIf { it > 0f && it.isFinite() } ?: 1f
    val videoRatio = if (size.width > 0 && size.height > 0) size.width * pixelRatio / size.height else null
    return PlayerPipSnapshot(
        isPlaying = isPlaying,
        isEnded = playbackState == Player.STATE_ENDED,
        canSkipPrevious = isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) && hasPreviousMediaItem(),
        canSkipNext = isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) && hasNextMediaItem(),
        canSeek = isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) && isCurrentMediaItemSeekable,
        aspectRatio = pipAspectRatio(),
        videoAspectRatio = videoRatio,
        title = if (isCommandAvailable(Player.COMMAND_GET_METADATA)) mediaMetadata.title?.toString() else null,
    )
}

/**
 * The PiP aspect ratio of the current video, from `videoSize` including its pixel aspect ratio, clamped to what
 * Android accepts. `null` until the player knows the video size.
 */
public fun Player.pipAspectRatio(): PipAspectRatio? {
    val size = videoSize
    return PipAspectRatio.fromVideoSize(size.width, size.height, size.pixelWidthHeightRatio)
}

/** Performs a media control from the PiP window on this player. */
public fun Player.perform(action: MediaPipAction) {
    when (action) {
        MediaPipAction.Play -> {
            if (playbackState == Player.STATE_IDLE) prepare()
            play()
        }
        MediaPipAction.Pause -> pause()
        MediaPipAction.Replay -> {
            seekToDefaultPosition()
            play()
        }
        MediaPipAction.SkipPrevious -> seekToPreviousMediaItem()
        MediaPipAction.SkipNext -> seekToNextMediaItem()
        MediaPipAction.SeekBack -> seekBack()
        MediaPipAction.SeekForward -> seekForward()
    }
}

/** A [PlayerPipSnapshot] of [player] that updates on every player event. */
@Composable
public fun rememberPlayerPipSnapshot(player: Player): PlayerPipSnapshot {
    var snapshot by remember(player) { mutableStateOf(player.pipSnapshot()) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                snapshot = player.pipSnapshot()
            }
        }
        player.addListener(listener)
        snapshot = player.pipSnapshot()
        onDispose { player.removeListener(listener) }
    }
    return snapshot
}
