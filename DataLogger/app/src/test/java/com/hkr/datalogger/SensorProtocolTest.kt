package com.hkr.datalogger

import org.junit.Assert.*
import org.junit.Test

class SensorProtocolTest {
    @Test fun sampleRoundTrip() {
        val sample = SensorSample(1, "accelerometer", "m/s2", 100, 200, listOf(1f, -2f, 3f))
        assertEquals(sample, SensorProtocol.decode(SensorProtocol.encode(sample)))
    }
    @Test fun rejectInvalidFrames() {
        listOf("SAMPLE 1 light lx 1 2 NaN", "SAMPLE 1 light lx 1 2 1,2",
            "SAMPLE 0 light lx 1 2 1", "SAMPLE 1 temperature C 1 2 20",
            "SAMPLE 1 light lx -1 2 1", "SAMPLE 1 light lx 1 2", "START").forEach {
            assertNull(it, SensorProtocol.decode(it))
        }
    }
}
