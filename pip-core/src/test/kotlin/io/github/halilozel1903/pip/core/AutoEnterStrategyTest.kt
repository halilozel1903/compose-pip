package io.github.halilozel1903.pip.core

import kotlin.test.Test
import kotlin.test.assertEquals

class AutoEnterStrategyTest {
    @Test
    fun android12AndNewerUseParams() {
        assertEquals(AutoEnterStrategy.Params, PipApiLevels.autoEnterStrategy(sdkInt = 31, autoEnter = true))
        assertEquals(AutoEnterStrategy.Params, PipApiLevels.autoEnterStrategy(sdkInt = 37, autoEnter = true))
    }

    @Test
    fun olderVersionsUseUserLeaveHint() {
        assertEquals(AutoEnterStrategy.UserLeaveHint, PipApiLevels.autoEnterStrategy(sdkInt = 26, autoEnter = true))
        assertEquals(AutoEnterStrategy.UserLeaveHint, PipApiLevels.autoEnterStrategy(sdkInt = 30, autoEnter = true))
    }

    @Test
    fun offWhenDisabledUnsupportedOrTooOld() {
        assertEquals(AutoEnterStrategy.Off, PipApiLevels.autoEnterStrategy(sdkInt = 35, autoEnter = false))
        assertEquals(AutoEnterStrategy.Off, PipApiLevels.autoEnterStrategy(sdkInt = 35, autoEnter = true, supported = false))
        assertEquals(AutoEnterStrategy.Off, PipApiLevels.autoEnterStrategy(sdkInt = 25, autoEnter = true))
    }
}
