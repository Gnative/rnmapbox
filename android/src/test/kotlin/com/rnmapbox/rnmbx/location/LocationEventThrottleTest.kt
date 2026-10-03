package com.rnmapbox.rnmbx.location

import com.rnmapbox.rnmbx.modules.LocationEventThrottle
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocationEventThrottleTest {
    @Test
    fun `configured milliseconds match the iOS elapsed-time comparison`() {
        val throttle = LocationEventThrottle(waitBetweenEvents = 500.0, lastSentTimestamp = 1_000_000_000L)
        assertFalse(throttle.shouldSend(1_001_000_000L)) // 1 ms
        assertFalse(throttle.shouldSend(1_499_000_000L)) // 499 ms
        assertFalse(throttle.shouldSend(1_500_000_000L)) // iOS uses strictly greater than
        assertTrue(throttle.shouldSend(1_501_000_000L))
    }

    @Test
    fun `first update and disabled throttling are immediate`() {
        assertTrue(LocationEventThrottle(waitBetweenEvents = 500.0).shouldSend(0L))
        assertTrue(LocationEventThrottle(lastSentTimestamp = 100L).shouldSend(100L))
    }
}
