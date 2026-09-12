package com.motionair.controller

import org.junit.Assert.*
import org.junit.Test

class MotionFramesTest {
    private val gravity = floatArrayOf(0f, 0f, 9.80665f)
    private val gyro = floatArrayOf(1f, 2f, 3f)

    @Test fun oldAccelerationCannotHideBehindAFreshGyroscope() {
        val frames = MotionFrames()
        frames.acceleration(gravity, 950_000_000, 950_000_000)
        assertNull(frames.gyroscope(gyro, 1_040_000_000, 1_090_000_000))
        frames.acceleration(gravity, 1_085_000_000, 1_090_000_000)
        assertNotNull(frames.gyroscope(gyro, 1_090_000_000, 1_090_000_000))
    }

    @Test fun rejectDelayedFutureDuplicateAndOutOfOrderFrames() {
        val frames = MotionFrames()
        frames.acceleration(gravity, 1_000_000_000, 1_000_000_000)
        assertNull(frames.gyroscope(gyro, 1_100_000_000, 1_000_000_000))
        assertNull(frames.gyroscope(gyro, 1_000_000_000, 1_100_000_001))
        assertNotNull(frames.gyroscope(gyro, 1_000_000_000, 1_000_000_000))
        assertNull(frames.gyroscope(gyro, 1_000_000_000, 1_000_000_000))
        assertNull(frames.gyroscope(gyro, 999_000_000, 1_000_000_000))
    }

    @Test fun sensorArraysAreCopiedAndBadReadingsDoNotPoisonRecovery() {
        val frames = MotionFrames()
        val source = gravity.copyOf()
        frames.acceleration(source, 1_000_000_000, 1_000_000_000)
        source[2] = 0f
        frames.acceleration(floatArrayOf(Float.NaN, 0f, 0f), 1_010_000_000, 1_010_000_000)
        frames.acceleration(floatArrayOf(99f, 0f, 0f), 999_000_000, 1_010_000_000)
        assertNull(frames.gyroscope(floatArrayOf(0f), 1_010_000_000, 1_010_000_000))
        val frame = frames.gyroscope(gyro, 1_010_000_000, 1_010_000_000)!!
        assertArrayEquals(gravity, frame.acceleration, 0f)
        assertArrayEquals(gyro, frame.gyroscope, 0f)
        assertEquals(1_010_000_000, frame.timestamp)
        assertNull(MotionFrames().gyroscope(gyro, 2_000_000_000, 2_000_000_000))
    }
}
