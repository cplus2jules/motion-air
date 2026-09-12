package com.motionair.controller

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*

/** The same three lessons as the iPhone welcome. Practice never touches a live session. */
class WelcomeView(
    context: Context,
    private val spanish: Boolean,
    initialStep: Int,
    private val replay: Boolean,
    private val stepChanged: (Int) -> Unit,
    private val finished: (Boolean) -> Unit,
) : LinearLayout(context) {
    private val ink = Color.rgb(32, 38, 48)
    private val muted = Color.rgb(90, 102, 118)
    private val blue = Color.rgb(0, 113, 227)
    private var step = initialStep.coerceIn(0, 2)
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun t(en: String, es: String) = if (spanish) es else en
    private fun label(value: String, size: Float = 16f, bold: Boolean = false) = TextView(context).apply {
        text = value; textSize = size; setTextColor(if (bold) ink else muted)
        if (bold) typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
        setLineSpacing(dp(3).toFloat(), 1f)
    }
    private fun button(value: String, primary: Boolean = false, action: () -> Unit) = Button(context).apply {
        stateListAnimator = null; elevation = 0f
        text = value; textSize = 16f; isAllCaps = false; minHeight = dp(52); minimumWidth = 0
        setPadding(dp(16), dp(6), dp(16), dp(6)); setTextColor(if (primary) Color.WHITE else blue)
        background = shape(if (primary) blue else Color.TRANSPARENT, dp(28).toFloat())
        setOnClickListener { performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); action() }
    }
    init { orientation = VERTICAL; render() }
    fun back(): Boolean {
        if (step == 0) return false
        step--; render(); return true
    }
    private fun render() {
        removeAllViews(); stepChanged(step)
        val header = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(24), dp(4), dp(12), dp(4)) }
        header.addView(ImageView(context).apply { setImageResource(R.drawable.motion_mark); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }, LayoutParams(dp(30), dp(30)))
        header.addView(label("Motion Air", 20f, true), LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })
        header.addView(button(if (replay) t("Close", "Cerrar") else t("Skip", "Omitir")) { finished(false) })
        addView(header, LayoutParams(-1, -2))

        val scroll = ScrollView(context).apply { isFillViewport = true }
        val body = LinearLayout(context).apply { orientation = VERTICAL; setPadding(dp(28), dp(20), dp(28), dp(20)) }
        scroll.addView(body, FrameLayout.LayoutParams(-1, -2)); addView(scroll, LayoutParams(-1, 0, 1f))
        val title = when (step) {
            0 -> t("Small screen.\nBig game.", "Pantalla pequeña.\nGran partida.")
            1 -> t("Feels familiar.\nPlays your way.", "Ya lo conoces.\nJuega a tu manera.")
            else -> t("Bring your\nbest moves.", "Saca tus\nmejores pasos.")
        }
        body.addView(label(title, 40f, true).apply {
            if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
        }, LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
        body.addView(label(when (step) {
            0 -> t("Your phone is the controller. Your computer runs the game. Let's get them together.", "Tu teléfono es el mando. Tu equipo ejecuta el juego. Vamos a conectarlos.")
            1 -> t("Move the stick to navigate. A selects. B goes back. Give them a try.", "Mueve el stick para navegar. A selecciona. B vuelve atrás. Pruébalos.")
            else -> t("Enable Motion after connecting. Then use Dance Lock to keep accidental taps out of your game.", "Activa el movimiento al conectar. Después bloquea los controles para evitar pulsaciones accidentales.")
        }), LayoutParams(-1, -2).apply { bottomMargin = dp(24) })
        when (step) {
            0 -> body.addView(WelcomeArtwork(context, t("Motion Air controller and computer", "Mando de Motion Air y equipo"), t("P R E S S  P L A Y", "P U L S A  J U G A R")), LayoutParams(-1, dp(300)))
            1 -> practice(body)
            else -> motionPreview(body)
        }
        body.addView(label(when (step) {
            0 -> t("Open Motion Air on your Mac or Windows PC. Keep both devices on the same Wi-Fi or hotspot.", "Abre Motion Air en tu Mac o PC con Windows. Usa la misma Wi-Fi o punto de acceso en ambos equipos.")
            1 -> t("Just practice. These controls stay right here.", "Solo es práctica. Estos controles no se envían al juego.")
            else -> t("Keep the app open while playing. After reconnecting, turn Motion on again.", "Mantén la app abierta al jugar. Al reconectar, activa el movimiento de nuevo.")
        }, 14f), LayoutParams(-1, -2).apply { topMargin = dp(22) })

        val footer = LinearLayout(context).apply {
            orientation = VERTICAL; setPadding(dp(28), dp(14), dp(28), dp(18)); setBackgroundColor(Color.WHITE)
        }
        val progress = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        repeat(3) { index -> progress.addView(View(context).apply {
            background = shape(if (index == step) muted else Color.LTGRAY, dp(3).toFloat())
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LayoutParams(dp(if (index == step) 28 else 8), dp(6)).apply { marginEnd = dp(8) }) }
        progress.addView(label(t("${step + 1} of 3", "${step + 1} de 3"), 13f).apply { gravity = Gravity.END; setSingleLine(true) }, LayoutParams(0, -2, 1f))
        footer.addView(progress, LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
        val actions = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        if (step > 0) actions.addView(button("←") { back() }.apply { contentDescription = t("Previous step", "Paso anterior") }, LayoutParams(dp(52), -2).apply { marginEnd = dp(10) })
        actions.addView(button(when (step) {
            0 -> t("Let's try it", "Vamos a probar")
            1 -> t("One more thing", "Una cosa más")
            else -> if (replay) t("Back to controller", "Volver al mando") else t("Pair my computer", "Conectar mi equipo")
        }, true) { if (step < 2) { step++; render() } else finished(!replay) }, LayoutParams(0, -2, 1f))
        footer.addView(actions); addView(footer, LayoutParams(-1, -2))
    }
    private fun practice(body: LinearLayout) {
        val response = label(t("Try the stick, A or B.", "Prueba el stick, A o B."), 15f, true).apply {
            gravity = Gravity.CENTER; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val grips = LinearLayout(context).apply { gravity = Gravity.CENTER; isBaselineAligned = false }
        fun grip(color: Int) = LinearLayout(context).apply {
            orientation = VERTICAL; gravity = Gravity.CENTER; background = shape(color, dp(40).toFloat())
        }
        val left = grip(Color.rgb(255, 83, 100)); val right = grip(Color.rgb(32, 165, 235))
        grips.addView(left, LayoutParams(0, dp(200), 1f).apply { marginEnd = dp(14) })
        grips.addView(right, LayoutParams(0, dp(200), 1f))
        left.addView(MenuStick(context) { x, y -> response.text = if (x == 0.0 && y == 0.0) t("Right back to center.", "De vuelta al centro.") else t("That's the way.", "Así se hace.") }, LayoutParams(-1, dp(144)))
        // Accessible alternatives to the free-drag stick, also available in the controller.
        val directions = LinearLayout(context).apply { gravity = Gravity.CENTER }
        listOf("←", "→").forEachIndexed { index, arrow -> directions.addView(button(arrow) {
            response.text = if (index == 0) t("Moving left.", "Hacia la izquierda.") else t("Moving right.", "Hacia la derecha.")
        }.apply { contentDescription = if (index == 0) t("Practice move left", "Practicar izquierda") else t("Practice move right", "Practicar derecha"); setTextColor(ink) }, LayoutParams(0, dp(48), 1f)) }
        left.addView(directions)
        listOf("A", "B").forEach { key -> right.addView(PadButton(context, key) { down ->
            if (down) response.text = if (key == "A") t("Selected. Nice one.", "Seleccionado. Muy bien.") else t("And you're back.", "Ya estás de vuelta.")
        }.apply { textSize = 26f; contentDescription = t("Practice $key", "Practicar $key") }, LayoutParams(dp(64), dp(64)).apply { topMargin = dp(10); bottomMargin = dp(10); marginStart = if (key == "A") dp(22) else 0; marginEnd = if (key == "B") dp(22) else 0 }) }
        body.addView(grips, LayoutParams(-1, -2))
        body.addView(response, LayoutParams(-1, -2).apply { topMargin = dp(20) })
    }
    private fun motionPreview(body: LinearLayout) {
        var locked = false
        val preview = LinearLayout(context).apply {
            orientation = VERTICAL; gravity = Gravity.CENTER; minimumHeight = dp(240); setPadding(dp(14), dp(24), dp(14), dp(14))
            background = shape(Color.WHITE, dp(30).toFloat()).apply { setStroke(dp(7), ink) }
            rotation = -8f; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        val mark = ImageView(context).apply { setImageResource(R.drawable.motion_mark); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
        preview.addView(mark, LayoutParams(dp(70), dp(70)))
        val waveform = LinearLayout(context).apply { gravity = Gravity.CENTER; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
        listOf(10, 18, 28, 14, 22, 34, 22, 14, 28, 18, 10).forEach { height -> waveform.addView(View(context).apply { background = shape(Color.rgb(32, 165, 235), dp(3).toFloat()) }, LayoutParams(dp(5), dp(height)).apply { marginEnd = dp(4) }) }
        preview.addView(waveform, LayoutParams(-1, dp(46)))
        val message = label(t("Your moves,\nin the game.", "Tus movimientos,\nen el juego."), 14f, true).apply { gravity = Gravity.CENTER }
        preview.addView(message); preview.contentDescription = message.text
        body.addView(preview, LayoutParams(dp(170), -2).apply { gravity = Gravity.CENTER_HORIZONTAL })
        val toggle = button(t("Try Dance Lock", "Probar bloqueo")) {}
        toggle.setOnClickListener {
            locked = !locked; preview.rotation = if (locked) 0f else -8f
            message.text = if (locked) t("Taps locked.\nMotion keeps going.", "Controles bloqueados.\nEl movimiento sigue.") else t("Your moves,\nin the game.", "Tus movimientos,\nen el juego.")
            preview.contentDescription = message.text
            toggle.text = if (locked) t("Unlock preview", "Desbloquear vista previa") else t("Try Dance Lock", "Probar bloqueo")
            toggle.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        }
        body.addView(toggle, LayoutParams(-1, -2).apply { topMargin = dp(14) })
    }
}

/** Native artwork matching the computer and controller illustration in the iPhone welcome. */
private class WelcomeArtwork(context: Context, description: String, private val playLabel: String) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mark = context.getDrawable(R.drawable.motion_mark)!!
    init { contentDescription = description }
    override fun onDraw(canvas: Canvas) {
        val scale = minOf(width / 320f, height / 300f)
        canvas.save(); canvas.translate((width - 320 * scale) / 2, (height - 300 * scale) / 2); canvas.scale(scale, scale)
        canvas.save(); canvas.rotate(-6f, 148f, 106f)
        paint.color = Color.LTGRAY; canvas.drawRoundRect(RectF(110f, 164f, 186f, 186f), 8f, 8f, paint)
        paint.color = Color.rgb(32, 38, 48); canvas.drawRoundRect(RectF(28f, 22f, 270f, 180f), 20f, 20f, paint)
        paint.color = Color.WHITE
        canvas.drawPath(Path().apply { moveTo(137f, 68f); lineTo(137f, 100f); lineTo(166f, 84f); close() }, paint)
        paint.typeface = Typeface.MONOSPACE; paint.textSize = 11f; paint.textAlign = Paint.Align.CENTER
        canvas.drawText(playLabel, 149f, 130f, paint); canvas.restore()
        canvas.save(); canvas.rotate(9f, 196f, 212f); mark.setBounds(88, 100, 310, 322); mark.draw(canvas); canvas.restore(); canvas.restore()
    }
}
