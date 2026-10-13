package io.github.halilozel1903.pip.core

import io.github.halilozel1903.pip.core.MediaPipAction.Pause
import io.github.halilozel1903.pip.core.MediaPipAction.Play
import io.github.halilozel1903.pip.core.MediaPipAction.Replay
import io.github.halilozel1903.pip.core.MediaPipAction.SeekBack
import io.github.halilozel1903.pip.core.MediaPipAction.SeekForward
import io.github.halilozel1903.pip.core.MediaPipAction.SkipNext
import io.github.halilozel1903.pip.core.MediaPipAction.SkipPrevious
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaPipActionPlannerTest {
    @Test
    fun playingPlaylistShowsSkipAndPause() {
        val plan = MediaPipActionPlanner.plan(isPlaying = true, canSkipPrevious = true, canSkipNext = true)
        assertEquals(listOf(SkipPrevious, Pause, SkipNext), plan.map { it.action })
        assertTrue(plan.all { it.enabled })
    }

    @Test
    fun pausedShowsPlay() {
        val plan = MediaPipActionPlanner.plan(isPlaying = false, canSkipNext = true)
        assertEquals(Play, plan[1].action)
    }

    @Test
    fun endedShowsReplay() {
        val plan = MediaPipActionPlanner.plan(isPlaying = false, isEnded = true)
        assertEquals(Replay, plan[1].action)
    }

    @Test
    fun unavailableSkipsStayInPlaceDisabled() {
        val plan = MediaPipActionPlanner.plan(isPlaying = true, canSkipPrevious = false, canSkipNext = true)
        assertEquals(PlannedPipAction(SkipPrevious, enabled = false), plan[0])
        assertEquals(PlannedPipAction(SkipNext, enabled = true), plan[2])
    }

    @Test
    fun seekLayout() {
        val plan = MediaPipActionPlanner.plan(isPlaying = true, layout = MediaPipActionLayout.Seek)
        assertEquals(listOf(SeekBack, Pause, SeekForward), plan.map { it.action })
        val ended = MediaPipActionPlanner.plan(isPlaying = false, isEnded = true, layout = MediaPipActionLayout.Seek)
        assertEquals(listOf(false, true, false), ended.map { it.enabled })
        val live = MediaPipActionPlanner.plan(isPlaying = true, canSeek = false, layout = MediaPipActionLayout.Seek)
        assertEquals(listOf(false, true, false), live.map { it.enabled })
    }

    @Test
    fun smallLimitsKeepPlayPause() {
        assertEquals(listOf(Pause), MediaPipActionPlanner.plan(isPlaying = true, maxActions = 2).map { it.action })
        assertEquals(listOf(Play), MediaPipActionPlanner.plan(isPlaying = false, maxActions = 1).map { it.action })
        assertEquals(emptyList(), MediaPipActionPlanner.plan(isPlaying = true, maxActions = 0))
    }

    @Test
    fun playPauseAndNoneLayouts() {
        assertEquals(
            listOf(Pause),
            MediaPipActionPlanner.plan(isPlaying = true, layout = MediaPipActionLayout.PlayPause, maxActions = 5).map { it.action },
        )
        assertEquals(emptyList(), MediaPipActionPlanner.plan(isPlaying = true, layout = MediaPipActionLayout.None))
    }
}
