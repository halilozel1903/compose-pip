package io.github.halilozel1903.pip.core

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * The aspect ratio of a picture-in-picture window, as a reduced `width:height` fraction.
 *
 * Android rejects PiP aspect ratios outside `1:2.39` to `2.39:1` with an `IllegalArgumentException`, so every
 * value made by [of] or [fromVideoSize] is clamped into [Min]..[Max]. Use [width] and [height] to build an
 * `android.util.Rational`.
 */
public class PipAspectRatio private constructor(
    /** Numerator of the reduced fraction. */
    public val width: Int,
    /** Denominator of the reduced fraction. */
    public val height: Int,
) : Comparable<PipAspectRatio> {

    /** The ratio as a number, for example `1.777` for 16:9. */
    public val value: Float get() = width.toFloat() / height

    /** `true` for a window taller than it is wide. */
    public val isPortrait: Boolean get() = height > width

    override fun compareTo(other: PipAspectRatio): Int =
        (width.toLong() * other.height).compareTo(other.width.toLong() * height)

    override fun equals(other: Any?): Boolean =
        other is PipAspectRatio && other.width == width && other.height == height

    override fun hashCode(): Int = 31 * width + height

    override fun toString(): String = "$width:$height"

    public companion object {
        /** The tallest ratio Android allows for a PiP window: 1:2.39. */
        public val Min: PipAspectRatio = PipAspectRatio(100, 239)

        /** The widest ratio Android allows for a PiP window: 2.39:1. */
        public val Max: PipAspectRatio = PipAspectRatio(239, 100)

        /** 16:9, most video. */
        public val Widescreen: PipAspectRatio = PipAspectRatio(16, 9)

        /** 4:3, older TV content. */
        public val Standard: PipAspectRatio = PipAspectRatio(4, 3)

        /** 1:1. */
        public val Square: PipAspectRatio = PipAspectRatio(1, 1)

        /** 9:16, vertical video and video calls. */
        public val Portrait: PipAspectRatio = PipAspectRatio(9, 16)

        /** The ratio of [width] to [height], reduced and clamped to [Min]..[Max]. Both must be positive. */
        public fun of(width: Int, height: Int): PipAspectRatio {
            require(width > 0 && height > 0) { "Aspect ratio sides must be positive, was $width:$height" }
            return reduce(width.toLong(), height.toLong())
        }

        /** Like [of], but `null` when a side is zero or negative, as for a video whose size is not known yet. */
        public fun ofOrNull(width: Int, height: Int): PipAspectRatio? =
            if (width > 0 && height > 0) of(width, height) else null

        /**
         * The display aspect ratio of a video frame of [width] x [height] pixels whose pixels are
         * [pixelWidthHeightRatio] times as wide as they are high (anamorphic video), turned by [rotationDegrees].
         * `null` while the size is unknown (zero).
         */
        public fun fromVideoSize(
            width: Int,
            height: Int,
            pixelWidthHeightRatio: Float = 1f,
            rotationDegrees: Int = 0,
        ): PipAspectRatio? {
            if (width <= 0 || height <= 0) return null
            val pixelRatio = if (pixelWidthHeightRatio > 0f && pixelWidthHeightRatio.isFinite()) pixelWidthHeightRatio else 1f
            var displayWidth = width.toLong()
            var displayHeight = height.toLong()
            if (abs(pixelRatio - 1f) > 0.0001f) {
                // Keep three decimals of the pixel ratio: 1440 x 1080 at 1.333 is about 16:9.
                displayWidth = (width * pixelRatio.toDouble() * 1000).roundToLong()
                displayHeight = height * 1000L
            }
            val turned = ((rotationDegrees % 360) + 360) % 360
            return if (turned == 90 || turned == 270) reduce(displayHeight, displayWidth) else reduce(displayWidth, displayHeight)
        }

        private fun reduce(width: Long, height: Long): PipAspectRatio {
            // Clamp first, comparing cross products so no precision is lost.
            if (width * Min.height < Min.width * height) return Min
            if (width * Max.height > Max.width * height) return Max
            val divisor = gcd(width, height)
            var w = width / divisor
            var h = height / divisor
            // Rational takes ints; scale huge fractions down while keeping the ratio close.
            while (w > Int.MAX_VALUE || h > Int.MAX_VALUE) {
                w /= 2
                h /= 2
            }
            return PipAspectRatio(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1))
        }

        private tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
    }
}
