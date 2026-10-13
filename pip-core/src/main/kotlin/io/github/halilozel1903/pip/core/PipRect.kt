package io.github.halilozel1903.pip.core

import kotlin.math.roundToInt

/**
 * A rectangle in window pixels, used as the `sourceRectHint` of a picture-in-picture transition: the part of the
 * window the PiP window grows out of and shrinks back into.
 */
public data class PipRect(
    public val left: Int,
    public val top: Int,
    public val right: Int,
    public val bottom: Int,
) {
    public val width: Int get() = right - left
    public val height: Int get() = bottom - top

    /** `true` when the rectangle has no area. The system ignores an empty source rect hint. */
    public val isEmpty: Boolean get() = width <= 0 || height <= 0

    /**
     * The largest rectangle of [aspectRatio] centered inside this one: where a video of that aspect ratio is
     * drawn when it is letterboxed or pillarboxed into this box. Using it as the source rect hint makes the
     * transition start from the picture itself, not from the black bars around it.
     */
    public fun fit(aspectRatio: PipAspectRatio): PipRect {
        if (isEmpty) return this
        val boxIsWider = width.toLong() * aspectRatio.height > height.toLong() * aspectRatio.width
        return if (boxIsWider) {
            val fittedWidth = (height.toDouble() * aspectRatio.width / aspectRatio.height).roundToInt()
            val inset = (width - fittedWidth) / 2
            PipRect(left + inset, top, left + inset + fittedWidth, bottom)
        } else {
            val fittedHeight = (width.toDouble() * aspectRatio.height / aspectRatio.width).roundToInt()
            val inset = (height - fittedHeight) / 2
            PipRect(left, top + inset, right, top + inset + fittedHeight)
        }
    }

    /** The overlap of this rectangle and [other], or `null` when they do not overlap. */
    public fun intersect(other: PipRect): PipRect? {
        val result = PipRect(
            maxOf(left, other.left),
            maxOf(top, other.top),
            minOf(right, other.right),
            minOf(bottom, other.bottom),
        )
        return if (result.isEmpty) null else result
    }

    override fun toString(): String = "[$left,$top][$right,$bottom]"

    public companion object {
        /** A rectangle from float bounds, as Compose reports them, rounded to whole pixels. */
        public fun fromBounds(left: Float, top: Float, right: Float, bottom: Float): PipRect =
            PipRect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())
    }
}
