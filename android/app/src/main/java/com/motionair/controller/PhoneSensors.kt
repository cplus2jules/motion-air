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
    private var acceleration: FloatArray? = null
    private var accelerationTime = 0L
    private var lastSent = 0L
    private var output: ((FloatArray, FloatArray, Long) -> Unit)? = null
    fun start(output: (FloatArray, FloatArray, Long) -> Unit) {
        stop(); check(available)
        this.output = output
        val accelReady = manager.registerListener(this, accelerometer, 16_666)
        val gyroReady = manager.registerListener(this, gyroscope, 16_666)
        if (!accelReady || !gyroReady) { stop(); error("sensors_missing") }
    }
    fun stop() { manager.unregisterListener(this); output = null; acceleration = null; accelerationTime = 0; lastSent = 0 }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onSensorChanged(event: SensorEvent) {
        if (event.values.take(3).any { !it.isFinite() }) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            acceleration = event.values.copyOf(3); accelerationTime = event.timestamp
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val accel = acceleration ?: return
            if (event.timestamp <= lastSent || event.timestamp - lastSent < 16_000_000 ||
                kotlin.math.abs(event.timestamp - accelerationTime) > 100_000_000 ||
                SystemClock.elapsedRealtimeNanos() - event.timestamp > 100_000_000) return
            lastSent = event.timestamp
            output?.invoke(accel, event.values.copyOf(3), event.timestamp)
        }
    }
}
