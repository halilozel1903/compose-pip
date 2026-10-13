package io.github.halilozel1903.pip.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PipAspectRatioTest {
    @Test
    fun reducesFractions() {
        assertEquals(PipAspectRatio.Widescreen, PipAspectRatio.of(1920, 1080))
        assertEquals("16:9", PipAspectRatio.of(1280, 720).toString())
        assertEquals(PipAspectRatio.Standard, PipAspectRatio.of(640, 480))
        assertEquals(PipAspectRatio.Portrait, PipAspectRatio.of(1080, 1920))
        assertEquals(PipAspectRatio.Square, PipAspectRatio.of(500, 500))
    }

    @Test
    fun clampsToAndroidLimits() {
        assertEquals(PipAspectRatio.Max, PipAspectRatio.of(3, 1))
        assertEquals(PipAspectRatio.Min, PipAspectRatio.of(1, 3))
        assertEquals(PipAspectRatio.Max, PipAspectRatio.of(2390, 1000))
        assertEquals(PipAspectRatio.of(237, 100), PipAspectRatio.of(2370, 1000))
        assertTrue(PipAspectRatio.Max.value in 2.389f..2.391f)
        assertTrue(PipAspectRatio.Min.value in 0.418f..0.419f)
    }

    @Test
    fun cinemascopeStaysInside() {
        val scope = PipAspectRatio.of(1920, 816)
        assertEquals("40:17", scope.toString())
        assertTrue(scope < PipAspectRatio.Max)
        assertTrue(scope > PipAspectRatio.Widescreen)
    }

    @Test
    fun rejectsNonPositiveSides() {
        assertFailsWith<IllegalArgumentException> { PipAspectRatio.of(0, 9) }
        assertFailsWith<IllegalArgumentException> { PipAspectRatio.of(16, -9) }
        assertNull(PipAspectRatio.ofOrNull(0, 0))
    }

    @Test
    fun videoSizeUnknownIsNull() {
        assertNull(PipAspectRatio.fromVideoSize(0, 0))
        assertNull(PipAspectRatio.fromVideoSize(1280, 0))
    }

    @Test
    fun videoSizeWithSquarePixels() {
        assertEquals(PipAspectRatio.Widescreen, PipAspectRatio.fromVideoSize(1280, 720))
        assertEquals(PipAspectRatio.Widescreen, PipAspectRatio.fromVideoSize(1280, 720, pixelWidthHeightRatio = 1f))
    }

    @Test
    fun anamorphicVideoUsesPixelRatio() {
        // 1440 x 1080 with 4:3 pixels displays as 16:9.
        val ratio = PipAspectRatio.fromVideoSize(1440, 1080, pixelWidthHeightRatio = 4f / 3f)!!
        assertTrue(ratio.value in 1.776f..1.779f, "was $ratio")
        // 720 x 576 PAL with 64:45 pixels is about 16:9 too.
        val pal = PipAspectRatio.fromVideoSize(720, 576, pixelWidthHeightRatio = 64f / 45f)!!
        assertTrue(pal.value in 1.77f..1.79f, "was $pal")
    }

    @Test
    fun invalidPixelRatioIsIgnored() {
        assertEquals(PipAspectRatio.Widescreen, PipAspectRatio.fromVideoSize(1920, 1080, pixelWidthHeightRatio = 0f))
        assertEquals(PipAspectRatio.Widescreen, PipAspectRatio.fromVideoSize(1920, 1080, pixelWidthHeightRatio = Float.NaN))
    }

    @Test
    fun rotationSwapsSides() {
        assertEquals(PipAspectRatio.Portrait, PipAspectRatio.fromVideoSize(1920, 1080, rotationDegrees = 90))
        assertEquals(PipAspectRatio.Portrait, PipAspectRatio.fromVideoSize(1920, 1080, rotationDegrees = -90))
        assertEquals(PipAspectRatio.Widescreen, PipAspectRatio.fromVideoSize(1920, 1080, rotationDegrees = 180))
    }

    @Test
    fun portraitFlag() {
        assertTrue(PipAspectRatio.Portrait.isPortrait)
        assertTrue(!PipAspectRatio.Widescreen.isPortrait)
    }
}
