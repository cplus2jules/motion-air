package com.motionair.controller

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import kotlin.math.hypot

fun shape(color: Int, radius: Float = 24f) = GradientDrawable().apply { setColor(color); cornerRadius = radius }

class PadButton(context: Context, label: String, private val changed: (Boolean) -> Unit) : Button(context) {
    private var pressed = false
    private var touchClick = false
    init {
        text = label; isAllCaps = false; textSize = 17f; setTextColor(Color.WHITE)
        background = shape(Color.rgb(32, 38, 48), 100f); contentDescription = label
        minimumWidth = 0; minWidth = 0; setPadding(0, 0, 0, 0)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { parent.requestDisallowInterceptTouchEvent(true); pressed = true; alpha = .72f; changed(true); performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (pressed) { pressed = false; changed(false) }; alpha = 1f
                parent.requestDisallowInterceptTouchEvent(false)
                if (event.actionMasked == MotionEvent.ACTION_UP) { touchClick = true; performClick(); touchClick = false }
            }
        }
        return true
    }
    override fun performClick(): Boolean {
        super.performClick()
        if (!touchClick && isEnabled) { changed(true); postDelayed({ changed(false) }, 100) }
        return true
    }
    override fun onDetachedFromWindow() { if (pressed) changed(false); pressed = false; super.onDetachedFromWindow() }
}

class MenuStick(context: Context, private val move: (Double, Double) -> Unit) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var xAxis = 0.0; private var yAxis = 0.0
    init { contentDescription = "Menu stick"; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    override fun onDraw(canvas: Canvas) {
        val r = minOf(width, height) * .44f; val cx = width / 2f; val cy = height / 2f
        paint.color = 0x40202630; canvas.drawCircle(cx, cy, r, paint)
        paint.color = Color.rgb(32, 38, 48); canvas.drawCircle(cx + (xAxis * r * .55).toFloat(), cy + (yAxis * r * .55).toFloat(), r * .57f, paint)
        paint.color = 0x507A8795; canvas.drawCircle(cx + (xAxis * r * .55).toFloat(), cy + (yAxis * r * .55).toFloat(), r * .40f, paint)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent.requestDisallowInterceptTouchEvent(true)
                val r = minOf(width, height) * .44
                val x = (event.x - width / 2) / r; val y = (event.y - height / 2) / r
                val magnitude = hypot(x, y).coerceAtLeast(1.0)
                xAxis = x / magnitude; yAxis = y / magnitude
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { xAxis = 0.0; yAxis = 0.0; parent.requestDisallowInterceptTouchEvent(false) }
        }
        move(xAxis, yAxis); invalidate(); return true
    }
    override fun onDetachedFromWindow() { move(0.0, 0.0); super.onDetachedFromWindow() }
}
