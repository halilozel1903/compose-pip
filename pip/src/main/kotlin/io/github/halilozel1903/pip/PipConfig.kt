package io.github.halilozel1903.pip

import androidx.compose.runtime.Immutable
import io.github.halilozel1903.pip.core.PipAspectRatio
import io.github.halilozel1903.pip.core.PipRect

/**
 * Everything that goes into the activity's `PictureInPictureParams`. Fields the running Android version does not
 * support are ignored: [autoEnter] and [seamlessResize] need Android 12 (API 31), [title] and [subtitle]
 * Android 13 (API 33). Below Android 12, [PipController] makes [autoEnter] work through `onUserLeaveHint`.
 *
 * @property aspectRatio the window's aspect ratio, usually the video's. `null` leaves the system default.
 * @property sourceRectHint the part of the window, in window pixels, that the PiP window animates out of and back
 *   into: the video. Set it with `Modifier.pipSourceRect` instead of by hand.
 * @property autoEnter enter PiP by itself when the user goes home or switches apps. Turn it on only while video
 *   plays, so a paused or finished video does not float over the launcher.
 * @property seamlessResize keep `true` for video. For other content (a map, a call grid) set `false` so the
 *   system cross-fades instead of stretching the old frame while the window resizes.
 * @property actions custom buttons, at most `Activity.getMaxNumPictureInPictureActions()` (3 on most phones);
 *   extra actions are dropped.
 */
@Immutable
public data class PipConfig(
    public val aspectRatio: PipAspectRatio? = PipAspectRatio.Widescreen,
    public val sourceRectHint: PipRect? = null,
    public val autoEnter: Boolean = false,
    public val seamlessResize: Boolean = true,
    public val actions: List<PipAction> = emptyList(),
    public val title: String? = null,
    public val subtitle: String? = null,
)
