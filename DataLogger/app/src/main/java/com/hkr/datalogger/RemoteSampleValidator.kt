package com.hkr.datalogger

/** Connection-scoped validation. Reset when a new stream or connection starts. */
class RemoteSampleValidator {
    private var lastSequence = 0L

    fun reset() { lastSequence = 0L }

    fun accept(line: String): SensorSample? {
        val sample = SensorProtocol.decode(line) ?: return null
        if (sample.sequence <= lastSequence) return null
        lastSequence = sample.sequence
        return sample
    }
}
