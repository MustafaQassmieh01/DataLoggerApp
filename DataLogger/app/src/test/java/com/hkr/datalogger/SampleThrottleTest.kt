package com.hkr.datalogger

import org.junit.Assert.*
import org.junit.Test

class SampleThrottleTest {
    @Test fun burstIsLimitedWithoutStarvingOtherChannels() {
        val throttle = SampleThrottle(100)
        assertTrue(throttle.accept(1, 1000))
        assertFalse(throttle.accept(1, 1050))
        assertTrue(throttle.accept(2, 1050))
        assertTrue(throttle.accept(1, 1100))
        assertFalse(throttle.accept(1, 1100))
    }

    @Test fun invalidTimestampsDoNotConsumeBudgetAndRestartClearsIt() {
        val throttle = SampleThrottle(100)
        assertFalse(throttle.accept(1, -1))
        assertTrue(throttle.accept(1, 1000))
        assertFalse(throttle.accept(1, 900))
        assertTrue(throttle.accept(1, 1100))
        throttle.clear()
        assertTrue(throttle.accept(1, 5))
    }
}
