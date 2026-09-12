package com.motionair.controller

/** Keep distinct quick taps visible to a polling game, independently per button. */
class ButtonPressTiming {
    private var requestedDown = false
    private var emittedDown = false
    private var changedAt: Long? = null
    private val pending = ArrayDeque<Boolean>()
    val hasPending get() = pending.isNotEmpty()

    fun append(down: Boolean) {
        if (down == requestedDown) return
        check(pending.size < 32) { "button_queue_full" }
        requestedDown = down
        pending.addLast(down)
    }
    fun delay(now: Long): Long {
        val previous = changedAt ?: return 0
        if (!hasPending) return 0
        return (previous + (if (emittedDown) 100 else 50) - now).coerceAtLeast(0)
    }
    fun pop(now: Long): Boolean? {
        if (!hasPending || delay(now) > 0) return null
        emittedDown = pending.removeFirst()
        changedAt = now
        return emittedDown
    }
}
