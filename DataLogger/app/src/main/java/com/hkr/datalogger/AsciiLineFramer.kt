package com.hkr.datalogger

import java.io.IOException

/** One instance per connection; incomplete final frames are discarded. */
class AsciiLineFramer(private val limit: Int = 8192) {
    private val pending = StringBuilder()
    fun accept(byte: Int): String? {
        if (byte !in 0..127) throw IOException("Expected ASCII data")
        if (byte == 10) {
            val line = pending.toString().removeSuffix("\r")
            pending.setLength(0)
            if (line.any { it.code !in 32..126 }) throw IOException("Expected printable ASCII frame")
            return line
        }
        if (pending.length >= limit) throw IOException("Incoming line exceeds $limit bytes")
        pending.append(byte.toChar())
        return null
    }
}
