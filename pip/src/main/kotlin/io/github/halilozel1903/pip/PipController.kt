package io.github.halilozel1903.pip

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import io.github.halilozel1903.pip.core.AutoEnterStrategy
import io.github.halilozel1903.pip.core.PipApiLevels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Picture-in-picture for one activity, without Compose.
 *
 * It keeps the activity's `PictureInPictureParams` in sync with [config], reports the PiP mode as a [StateFlow]
 * (through `OnPictureInPictureModeChangedProvider`), routes taps on custom actions to [onAction] and makes
 * [PipConfig.autoEnter] work on every version: `setAutoEnterEnabled` on Android 12+, `onUserLeaveHint` before.
 *
 * The activity must declare `android:supportsPictureInPicture="true"` in the manifest. Use one controller per
 * activity and call [release] when the screen goes away (`rememberPipState` does both for you in Compose).
 */
public class PipController(
    private val activity: ComponentActivity,
    config: PipConfig = PipConfig(),
) {
    /** `false` on devices without picture-in-picture (some TVs, Android Go devices, or when the user turned it off). */
    public val isSupported: Boolean =
        activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    /** How many custom actions the PiP window shows on this device (0 when PiP is not supported). */
    public val maxActions: Int = if (isSupported) runCatching { activity.maxNumPictureInPictureActions }.getOrDefault(3) else 0

    private val inPip = MutableStateFlow(activity.isInPictureInPictureMode)

    /** Whether the activity is in picture-in-picture right now. */
    public val isInPip: StateFlow<Boolean> = inPip.asStateFlow()

    /** Called on the main thread when the user taps an enabled custom action of the PiP window. */
    public var onAction: ((PipAction) -> Unit)? = null

    /** The parameters the activity uses for PiP. Setting a different value updates the activity right away. */
    public var config: PipConfig = config
        set(value) {
            if (field == value) return
            field = value
            applyParams()
        }

    /** How auto-enter works with the current [config] on this device. */
    public val autoEnterStrategy: AutoEnterStrategy
        get() = PipApiLevels.autoEnterStrategy(Build.VERSION.SDK_INT, config.autoEnter, isSupported)

    private val modeListeners = CopyOnWriteArrayList<(Boolean) -> Unit>()
    private var released = false
    private var warned = false

    private val receiver = PipActionReceiver(activity) { id ->
        val action = config.actions.firstOrNull { it.id == id }
        if (action != null && action.enabled) onAction?.invoke(action)
    }

    private val modeChanged = Consumer<PictureInPictureModeChangedInfo> { info ->
        val value = info.isInPictureInPictureMode
        inPip.value = value
        modeListeners.forEach { it(value) }
    }

    private val userLeaveHint = Runnable {
        // Android 12+ enters by itself through setAutoEnterEnabled; calling enter here as well would animate twice.
        if (autoEnterStrategy == AutoEnterStrategy.UserLeaveHint && !activity.isInPictureInPictureMode) enter()
    }

    init {
        activity.addOnPictureInPictureModeChangedListener(modeChanged)
        activity.addOnUserLeaveHintListener(userLeaveHint)
        if (isSupported) receiver.register()
        applyParams()
    }

    /** Replaces [config] with [transform] of it. */
    public fun update(transform: (PipConfig) -> PipConfig) {
        config = transform(config)
    }

    /**
     * Enters picture-in-picture now, for an in-app PiP button. Returns `false` when PiP is not supported, the
     * activity does not declare `supportsPictureInPicture`, or the system refused (for example while the screen is
     * locked).
     */
    public fun enter(): Boolean {
        if (!isSupported || released) return false
        return try {
            activity.enterPictureInPictureMode(buildParams())
        } catch (e: IllegalStateException) {
            warn(e)
            false
        } catch (e: IllegalArgumentException) {
            warn(e)
            false
        }
    }

    /** Listens to PiP mode changes on the main thread, in addition to [isInPip]. */
    public fun addOnPipModeChangedListener(listener: (isInPip: Boolean) -> Unit) {
        modeListeners += listener
    }

    public fun removeOnPipModeChangedListener(listener: (isInPip: Boolean) -> Unit) {
        modeListeners -= listener
    }

    /**
     * Stops listening to the activity and to action taps. Auto-enter is turned off first, so the activity does
     * not enter PiP on its own after this screen is gone.
     */
    public fun release() {
        if (released) return
        if (config.autoEnter) config = config.copy(autoEnter = false, actions = emptyList())
        released = true
        activity.removeOnPictureInPictureModeChangedListener(modeChanged)
        activity.removeOnUserLeaveHintListener(userLeaveHint)
        modeListeners.clear()
        receiver.close()
    }

    private fun applyParams() {
        if (!isSupported || released) return
        try {
            activity.setPictureInPictureParams(buildParams())
        } catch (e: IllegalStateException) {
            warn(e)
        } catch (e: IllegalArgumentException) {
            warn(e)
        }
    }

    private fun buildParams(): PictureInPictureParams {
        val config = config
        val builder = PictureInPictureParams.Builder()
        config.aspectRatio?.let { builder.setAspectRatio(Rational(it.width, it.height)) }
        config.sourceRectHint?.takeUnless { it.isEmpty }?.let {
            builder.setSourceRectHint(Rect(it.left, it.top, it.right, it.bottom))
        }
        builder.setActions(config.actions.take(maxActions).map(receiver::remoteAction))
        if (Build.VERSION.SDK_INT >= PipApiLevels.AUTO_ENTER) {
            builder.setAutoEnterEnabled(config.autoEnter)
            builder.setSeamlessResizeEnabled(config.seamlessResize)
        }
        if (Build.VERSION.SDK_INT >= PipApiLevels.TITLE) {
            builder.setTitle(config.title)
            builder.setSubtitle(config.subtitle)
        }
        return builder.build()
    }

    private fun warn(e: RuntimeException) {
        if (warned) return
        warned = true
        Log.w(
            TAG,
            "Picture-in-picture is unavailable for ${activity.javaClass.simpleName}. Declare " +
                "android:supportsPictureInPicture=\"true\" on the activity in AndroidManifest.xml.",
            e,
        )
    }

    private companion object {
        const val TAG = "PipController"
    }
}
