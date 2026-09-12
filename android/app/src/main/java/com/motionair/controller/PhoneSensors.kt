package com.motionair.controller

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock

class PhoneSensors(context: Context) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    val available get() = accelerometer != null && gyroscope != null
    private var frames = MotionFrames()
    private var output: ((FloatArray, FloatArray, Long) -> Unit)? = null
    fun start(output: (FloatArray, FloatArray, Long) -> Unit) {
        stop(); check(available)
        this.output = output
        val accelReady = manager.registerListener(this, accelerometer, 16_666)
        val gyroReady = manager.registerListener(this, gyroscope, 16_666)
        if (!accelReady || !gyroReady) { stop(); error("sensors_missing") }
    }
    fun stop() { manager.unregisterListener(this); output = null; frames = MotionFrames() }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onSensorChanged(event: SensorEvent) {
        if (output == null) return
        val now = SystemClock.elapsedRealtimeNanos()
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            frames.acceleration(event.values, event.timestamp, now)
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val frame = frames.gyroscope(event.values, event.timestamp, now) ?: return
            output?.invoke(frame.acceleration, frame.gyroscope, frame.timestamp)
        }
    }
}
