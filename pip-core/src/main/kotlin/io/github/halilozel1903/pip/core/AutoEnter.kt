package io.github.halilozel1903.pip.core

/** How an activity enters picture-in-picture by itself when the user leaves it. */
public enum class AutoEnterStrategy {
    /**
     * Android 12 (API 31) and newer: `PictureInPictureParams.Builder.setAutoEnterEnabled(true)`. The system
     * enters PiP during the swipe home gesture, so the window animates out of the app smoothly.
     */
    Params,

    /** Android 8 to 11 (API 26 to 30): call `enterPictureInPictureMode` from `onUserLeaveHint`. */
    UserLeaveHint,

    /** Auto-enter is off or picture-in-picture is unavailable. */
    Off,
}

/** API levels that matter for picture-in-picture. */
public object PipApiLevels {
    /** Android 8.0: `PictureInPictureParams`, custom actions, aspect ratio, source rect hint. */
    public const val PARAMS: Int = 26

    /** Android 12: auto-enter and seamless resize. */
    public const val AUTO_ENTER: Int = 31

    /** Android 13: title and subtitle (shown on TV and in the PiP menu on some devices). */
    public const val TITLE: Int = 33

    /** The strategy for an app that wants to [autoEnter] on a device running [sdkInt], if PiP is [supported]. */
    public fun autoEnterStrategy(sdkInt: Int, autoEnter: Boolean, supported: Boolean = true): AutoEnterStrategy = when {
        !autoEnter || !supported || sdkInt < PARAMS -> AutoEnterStrategy.Off
        sdkInt >= AUTO_ENTER -> AutoEnterStrategy.Params
        else -> AutoEnterStrategy.UserLeaveHint
    }
}
