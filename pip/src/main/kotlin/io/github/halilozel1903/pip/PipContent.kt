package io.github.halilozel1903.pip

import androidx.compose.runtime.Composable

/**
 * Shows [content] only while the activity is not in picture-in-picture. Wrap controls, titles and anything else
 * that does not belong in the small PiP window: everything but the video. Taps never reach a PiP window's content
 * anyway (the system handles them), so buttons there are only clutter.
 */
@Composable
public fun HideInPip(state: PipState, content: @Composable () -> Unit) {
    if (!state.isInPip) content()
}

/** Shows [content] only while the activity is in picture-in-picture, for example a "Playing in PiP" label. */
@Composable
public fun ShowInPip(state: PipState, content: @Composable () -> Unit) {
    if (state.isInPip) content()
}
