package io.github.halilozel1903.pip

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import io.github.halilozel1903.pip.core.AutoEnterStrategy
import io.github.halilozel1903.pip.core.PipAspectRatio
import io.github.halilozel1903.pip.core.PipRect
import kotlinx.coroutines.flow.StateFlow

/**
 * Picture-in-picture state of a Compose screen, made by [rememberPipState]. Read [isInPip] to change the UI (hide
 * controls with [HideInPip]), call [enter] from a PiP button, and attach `Modifier.pipSourceRect(state)` to the
 * video so the window animates out of it.
 */
@Stable
public class PipState internal constructor(
    /** The controller behind this state, for anything not covered here. */
    public val controller: PipController,
) {
    /** Whether the activity is in picture-in-picture. Backed by snapshot state, so reading it recomposes. */
    public var isInPip: Boolean by mutableStateOf(controller.isInPip.value)
        internal set

    /** The same as [isInPip], as a flow, for use outside composition. */
    public val isInPipFlow: StateFlow<Boolean> get() = controller.isInPip

    /** `false` on devices without picture-in-picture. Hide your PiP button then. */
    public val isSupported: Boolean get() = controller.isSupported

    /** How many custom actions the PiP window shows on this device. */
    public val maxActions: Int get() = controller.maxActions

    /** The parameters in use, including the current source rect hint. */
    public var config: PipConfig by mutableStateOf(controller.config)
        private set

    /** How auto-enter works on this device with the current configuration. */
    public val autoEnterStrategy: AutoEnterStrategy get() = controller.autoEnterStrategy

    /** The source rect hint in use, in window pixels, or `null` when no `Modifier.pipSourceRect` is attached. */
    public val sourceRectHint: PipRect? get() = config.sourceRectHint

    private var sourceBounds: PipRect? = null
    private var fitSourceToAspectRatio = true

    /** Enters picture-in-picture now. Returns `false` when the system refused or PiP is not supported. */
    public fun enter(): Boolean = controller.enter()

    internal fun update(transform: (PipConfig) -> PipConfig) {
        controller.update(transform)
        config = controller.config
    }

    internal fun reportSourceBounds(bounds: PipRect?, fitToAspectRatio: Boolean) {
        if (bounds == sourceBounds && fitToAspectRatio == fitSourceToAspectRatio) return
        sourceBounds = bounds
        fitSourceToAspectRatio = fitToAspectRatio
        update { it.copy(sourceRectHint = sourceRectHintFor(it.aspectRatio)) }
    }

    internal fun sourceRectHintFor(aspectRatio: PipAspectRatio?): PipRect? {
        val bounds = sourceBounds ?: return null
        return if (fitSourceToAspectRatio && aspectRatio != null) bounds.fit(aspectRatio) else bounds
    }
}

/**
 * Remembers picture-in-picture for the activity hosting this composition, and keeps its parameters in sync with
 * the arguments on every recomposition. Use it once per screen, near the top of the video screen.
 *
 * ```
 * val pip = rememberPipState(
 *     aspectRatio = PipAspectRatio.Widescreen,
 *     autoEnter = isPlaying,
 *     actions = listOf(PipActions.playPause(isPlaying)),
 *     onAction = { if (it.id == PipActions.PAUSE) player.pause() else player.play() },
 * )
 * VideoSurface(Modifier.pipSourceRect(pip))
 * HideInPip(pip) { Controls() }
 * ```
 *
 * @param aspectRatio the PiP window's aspect ratio; pass the video's (clamped by [PipAspectRatio] to what
 *   Android accepts).
 * @param autoEnter enter PiP when the user leaves the app. Tie it to "is playing".
 * @param seamlessResize keep `true` for video, `false` for non-video content.
 * @param actions custom buttons in the PiP window; see [PipActions].
 * @param title shown by the system on Android 13+ where it has room (TV, the PiP menu on some devices).
 * @param onAction called when the user taps one of [actions].
 */
@Composable
public fun rememberPipState(
    aspectRatio: PipAspectRatio? = PipAspectRatio.Widescreen,
    autoEnter: Boolean = false,
    seamlessResize: Boolean = true,
    actions: List<PipAction> = emptyList(),
    title: String? = null,
    subtitle: String? = null,
    onAction: (PipAction) -> Unit = {},
): PipState {
    val activity = LocalContext.current.findComponentActivity()
    val state = remember(activity) { PipState(PipController(activity)) }
    val currentOnAction by rememberUpdatedState(onAction)

    DisposableEffect(state) {
        val listener: (Boolean) -> Unit = { state.isInPip = it }
        state.controller.addOnPipModeChangedListener(listener)
        state.controller.onAction = { currentOnAction(it) }
        state.isInPip = state.controller.isInPip.value
        onDispose { state.controller.release() }
    }

    SideEffect {
        state.update {
            PipConfig(
                aspectRatio = aspectRatio,
                sourceRectHint = state.sourceRectHintFor(aspectRatio),
                autoEnter = autoEnter,
                seamlessResize = seamlessResize,
                actions = actions,
                title = title,
                subtitle = subtitle,
            )
        }
    }
    return state
}

/**
 * Whether the activity hosting this composition is in picture-in-picture, for composables deep in the tree that do
 * not get the [PipState]. It only listens; it does not change any PiP parameters.
 */
@Composable
public fun rememberIsInPip(): State<Boolean> {
    val activity = LocalContext.current.findComponentActivity()
    val inPip = remember(activity) { mutableStateOf(activity.isInPictureInPictureMode) }
    DisposableEffect(activity) {
        val listener = Consumer<PictureInPictureModeChangedInfo> {
            inPip.value = it.isInPictureInPictureMode
        }
        activity.addOnPictureInPictureModeChangedListener(listener)
        inPip.value = activity.isInPictureInPictureMode
        onDispose { activity.removeOnPictureInPictureModeChangedListener(listener) }
    }
    return inPip
}

/** How many custom actions a PiP window can show on this device, to plan actions before [rememberPipState]. */
@Composable
public fun rememberMaxPipActions(): Int {
    val activity = LocalContext.current.findComponentActivity()
    return remember(activity) {
        val supported = activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        if (supported) runCatching { activity.maxNumPictureInPictureActions }.getOrDefault(3) else 0
    }
}

internal tailrec fun Context.findComponentActivity(): ComponentActivity = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> error("Picture-in-picture needs a ComponentActivity, but this composition is hosted in $this")
}
