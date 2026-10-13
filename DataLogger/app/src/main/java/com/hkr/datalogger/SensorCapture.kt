package com.hkr.datalogger

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/** Foreground-only capture; callback and listener operations run on the activity thread. */
class SensorCapture(context: Context, private val onSample: (SensorSample) -> Unit) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val channels = listOf(
        SensorChannel(Sensor.TYPE_ACCELEROMETER, "accelerometer", "m/s2", 3),
        SensorChannel(Sensor.TYPE_GYROSCOPE, "gyroscope", "rad/s", 3),
        SensorChannel(Sensor.TYPE_LIGHT, "light", "lx", 1)
    )
    private val sensors = channels.associateWith { manager.getDefaultSensor(it.type) }
    private val throttle = SampleThrottle(100_000_000L)
    private var sequence = 0L
    var running = false
        private set

    fun availability(): List<String> = sensors.map { (channel, sensor) ->
        "${channel.name}: ${if (sensor == null) "unavailable" else "available (${channel.units})"}"
    }

    /** Returns channels whose listeners actually registered, which can differ from availability. */
    fun start(): List<String> {
        stop()
        sequence = 0
        throttle.clear()
        val registered = sensors.mapNotNull { (channel, sensor) ->
            if (sensor != null && manager.registerListener(this, sensor, 100_000)) channel.name else null
        }
        running = registered.isNotEmpty()
        return registered
    }

    fun stop() {
        running = false
        manager.unregisterListener(this)
        throttle.clear()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!running) return
        val channel = channels.firstOrNull { it.type == event.sensor.type } ?: return
        val values = event.values.take(channel.dimensions)
        if (values.size != channel.dimensions || values.any { !it.isFinite() }) return
        if (!throttle.accept(channel.type, event.timestamp)) return
        onSample(SensorSample(++sequence, channel.name, channel.units, event.timestamp,
            System.currentTimeMillis(), values))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

data class SensorChannel(val type: Int, val name: String, val units: String, val dimensions: Int)

/** timestampNs is Android's monotonic sensor clock; receivedAtMs is local wall-clock receipt time. */
data class SensorSample(val sequence: Long, val channel: String, val units: String,
                        val timestampNs: Long, val receivedAtMs: Long, val values: List<Float>)
