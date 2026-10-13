package io.github.halilozel1903.pip

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import io.github.halilozel1903.pip.core.PipRect

/**
 * Uses this element's bounds in the window as the source rect hint of picture-in-picture: the window grows out of
 * this rectangle when entering PiP and shrinks back into it when leaving, instead of fading the whole activity.
 * Put it on the video surface.
 *
 * With [fitToAspectRatio], the bounds are narrowed to the PiP aspect ratio, centered, so a letterboxed video
 * starts the transition from the picture rather than from its black bars. Bounds stop updating while in PiP, and
 * the hint is cleared when the element leaves the composition.
 */
public fun Modifier.pipSourceRect(state: PipState, fitToAspectRatio: Boolean = true): Modifier =
    this then PipSourceRectElement(state, fitToAspectRatio)

private data class PipSourceRectElement(
    val state: PipState,
    val fitToAspectRatio: Boolean,
) : ModifierNodeElement<PipSourceRectNode>() {
    override fun create(): PipSourceRectNode = PipSourceRectNode(state, fitToAspectRatio)

    override fun update(node: PipSourceRectNode) {
        if (node.state !== state) node.state.reportSourceBounds(null, node.fitToAspectRatio)
        node.state = state
        node.fitToAspectRatio = fitToAspectRatio
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "pipSourceRect"
        properties["fitToAspectRatio"] = fitToAspectRatio
    }
}

private class PipSourceRectNode(
    var state: PipState,
    var fitToAspectRatio: Boolean,
) : Modifier.Node(), GlobalPositionAwareModifierNode {

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        // In PiP the window itself is small: those bounds are not where the video sits full size.
        if (state.isInPip || !coordinates.isAttached) return
        val bounds = coordinates.boundsInWindow()
        val rect = PipRect.fromBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
        state.reportSourceBounds(rect.takeUnless { it.isEmpty }, fitToAspectRatio)
    }

    override fun onDetach() {
        state.reportSourceBounds(null, fitToAspectRatio)
    }
}
