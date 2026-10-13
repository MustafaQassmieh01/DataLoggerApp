package com.hkr.datalogger

/** Each channel has its own rate budget; invalid or out-of-order timestamps are discarded. */
class SampleThrottle(private val intervalNs: Long) {
    init { require(intervalNs > 0) }
    private val previous = mutableMapOf<Int, Long>()

    fun accept(channel: Int, timestampNs: Long): Boolean {
        if (timestampNs < 0) return false
        val last = previous[channel]
        if (last != null && (timestampNs < last || timestampNs - last < intervalNs)) return false
        previous[channel] = timestampNs
        return true
    }

    fun clear() = previous.clear()
}
