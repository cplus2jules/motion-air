package com.motionair.controller

/** Sensor timestamps and now use elapsedRealtimeNanos, never wall-clock time. */
class MotionFrames {
    data class Frame(val acceleration: FloatArray, val gyroscope: FloatArray, val timestamp: Long)
    private var acceleration: FloatArray? = null
    private var accelerationTime = -1L
    private var lastSent = -1L

    private fun fresh(timestamp: Long, now: Long) =
        timestamp >= 0 && now >= timestamp && now - timestamp <= 100_000_000

    fun acceleration(values: FloatArray, timestamp: Long, now: Long) {
        if (values.size < 3 || values.take(3).any { !it.isFinite() } ||
            timestamp <= accelerationTime || !fresh(timestamp, now)) return
        acceleration = values.copyOf(3)
        accelerationTime = timestamp
    }

    fun gyroscope(values: FloatArray, timestamp: Long, now: Long): Frame? {
        val accel = acceleration ?: return null
        if (values.size < 3 || values.take(3).any { !it.isFinite() } ||
            !fresh(timestamp, now) || !fresh(accelerationTime, now) ||
            timestamp <= lastSent || (lastSent >= 0 && timestamp - lastSent < 16_000_000) ||
            kotlin.math.abs(timestamp - accelerationTime) > 100_000_000) return null
        lastSent = timestamp
        return Frame(accel.copyOf(), values.copyOf(3), timestamp)
    }
}
