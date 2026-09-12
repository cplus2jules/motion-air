package com.motionair.controller

import org.junit.Assert.*
import org.junit.Test

class ButtonPressTimingTest {
    @Test fun twoQuickTapsRemainTwoPressesWithAReleaseBetweenThem() {
        val timing = ButtonPressTiming()
        timing.append(true)
        assertEquals(true, timing.pop(0))
        timing.append(false)
        timing.append(true)
        timing.append(false)
        assertNull(timing.pop(99))
        assertEquals(false, timing.pop(100))
        assertNull(timing.pop(149))
        assertEquals(true, timing.pop(150))
        assertNull(timing.pop(249))
        assertEquals(false, timing.pop(250))
        assertFalse(timing.hasPending)
    }
    @Test fun duplicatesDoNotCreateExtraPressesAndButtonsAreIndependent() {
        val a = ButtonPressTiming(); val b = ButtonPressTiming()
        a.append(true); a.append(true); a.append(false); a.append(false)
        assertEquals(true, a.pop(0))
        b.append(true)
        assertEquals(true, b.pop(1))
        assertEquals(false, a.pop(100))
        assertNull(a.pop(150))
    }
    @Test fun queueHasABoundedBacklog() {
        val timing = ButtonPressTiming()
        repeat(32) { timing.append(it % 2 == 0) }
        assertThrows(IllegalStateException::class.java) { timing.append(true) }
    }
}
