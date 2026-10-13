package io.github.halilozel1903.pip.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PipRectTest {
    @Test
    fun sizeAndEmptiness() {
        val rect = PipRect(0, 100, 1080, 708)
        assertEquals(1080, rect.width)
        assertEquals(608, rect.height)
        assertTrue(!rect.isEmpty)
        assertTrue(PipRect(10, 10, 10, 50).isEmpty)
        assertTrue(PipRect(10, 50, 20, 40).isEmpty)
    }

    @Test
    fun fitPillarboxesInWideBox() {
        // A 16:9 video in a 2400 x 1080 landscape screen is drawn 1920 wide, centered.
        val fitted = PipRect(0, 0, 2400, 1080).fit(PipAspectRatio.Widescreen)
        assertEquals(PipRect(240, 0, 2160, 1080), fitted)
    }

    @Test
    fun fitLetterboxesInTallBox() {
        // A 16:9 video in a 1080 x 1080 box is 608 high, centered.
        val fitted = PipRect(0, 200, 1080, 1280).fit(PipAspectRatio.Widescreen)
        assertEquals(PipRect(0, 436, 1080, 1044), fitted)
    }

    @Test
    fun fitKeepsMatchingBox() {
        val box = PipRect(0, 132, 1080, 740)
        assertEquals(box, box.fit(PipAspectRatio.Widescreen))
    }

    @Test
    fun fitOfEmptyIsEmpty() {
        val empty = PipRect(0, 0, 0, 0)
        assertEquals(empty, empty.fit(PipAspectRatio.Widescreen))
    }

    @Test
    fun intersect() {
        val window = PipRect(0, 0, 1080, 2400)
        assertEquals(PipRect(0, 0, 1080, 300), PipRect(-50, -100, 1200, 300).intersect(window))
        assertNull(PipRect(0, 2500, 1080, 2600).intersect(window))
    }

    @Test
    fun fromBoundsRounds() {
        assertEquals(PipRect(0, 133, 1080, 741), PipRect.fromBounds(0.2f, 132.6f, 1079.5f, 740.5f))
    }
}
