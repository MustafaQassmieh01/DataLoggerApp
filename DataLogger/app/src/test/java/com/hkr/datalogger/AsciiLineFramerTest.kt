package com.hkr.datalogger

import java.io.IOException
import org.junit.Assert.*
import org.junit.Test

class AsciiLineFramerTest {
    @Test fun fragmentedAndMultipleFrames() {
        val framer = AsciiLineFramer()
        assertNull(framer.accept('S'.code))
        val frames = "TART\r\nSTOP\npartial".mapNotNull { framer.accept(it.code) }
        assertEquals(listOf("START", "STOP"), frames)
        val reconnected = AsciiLineFramer()
        assertEquals(listOf("HELLO"), "HELLO\n".mapNotNull { reconnected.accept(it.code) })
    }
    @Test(expected = IOException::class) fun boundedPartialFrame() {
        val framer = AsciiLineFramer(2)
        "ABC".forEach { framer.accept(it.code) }
    }
    @Test(expected = IOException::class) fun rejectsNonAscii() { AsciiLineFramer().accept(255) }
}
