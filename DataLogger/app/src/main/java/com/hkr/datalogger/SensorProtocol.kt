package com.hkr.datalogger

/** Version 1 printable ASCII frames, delimited by LF. */
object SensorProtocol {
    fun encode(sample: SensorSample): String = listOf("SAMPLE", sample.sequence, sample.channel,
        sample.units, sample.timestampNs, sample.receivedAtMs,
        sample.values.joinToString(",")).joinToString(" ")

    fun decode(line: String): SensorSample? {
        val fields = line.split(" ")
        if (fields.size != 7 || fields[0] != "SAMPLE") return null
        val dimensions = when (fields[2] to fields[3]) {
            "accelerometer" to "m/s2", "gyroscope" to "rad/s" -> 3
            "light" to "lx" -> 1
            else -> return null
        }
        val sequence = fields[1].toLongOrNull()?.takeIf { it > 0 } ?: return null
        val timestamp = fields[4].toLongOrNull()?.takeIf { it >= 0 } ?: return null
        val wall = fields[5].toLongOrNull()?.takeIf { it >= 0 } ?: return null
        val values = fields[6].split(",").map { it.toFloatOrNull() ?: return null }
        if (values.size != dimensions || values.any { !it.isFinite() }) return null
        return SensorSample(sequence, fields[2], fields[3], timestamp, wall, values)
    }
}
