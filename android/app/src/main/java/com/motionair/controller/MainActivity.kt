package com.motionair.controller

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.*
import com.google.zxing.integration.android.IntentIntegrator
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val ink = Color.rgb(32, 38, 48)
    private val muted = Color.rgb(90, 102, 118)
    private val blue = Color.rgb(32, 165, 235)
    private val red = Color.rgb(255, 83, 100)
    private val canvas = Color.rgb(245, 246, 248)
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var store: PairingStore
    private lateinit var session: ControllerSession
    private lateinit var column: LinearLayout
    private var spanish = false
    private var screen = "home"
    private var generation = 0
    private var pairing: PairingClient? = null
    private var pending = false
    private var status: TextView? = null
    private var detail: TextView? = null
    private var motionButton: Button? = null
    private var lockButton: Button? = null
    private val controls = mutableListOf<View>()
    private var locked = false
    private var homeError = ""

    private fun t(en: String, es: String) = if (spanish) es else en
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val language = getPreferences(MODE_PRIVATE).getString("language", Locale.getDefault().language)
        spanish = language == "es"
        store = PairingStore(this)
        session = ControllerSession(this) { refresh() }
        showHome()
    }
    private fun base(dark: Boolean = false) {
        status = null; detail = null; motionButton = null; lockButton = null; controls.clear()
        val scroll = ScrollView(this).apply { isFillViewport = true; setBackgroundColor(if (dark) ink else canvas) }
        column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(20), dp(24), dp(28)) }
        scroll.addView(column, FrameLayout.LayoutParams(-1, -2))
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        setContentView(scroll)
        window.decorView.systemUiVisibility = if (dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }
    private fun text(value: String, size: Float = 16f, color: Int = ink, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); setLineSpacing(dp(3).toFloat(), 1f)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
    }
    private fun addText(value: String, size: Float = 16f, color: Int = ink, bold: Boolean = false): TextView = text(value, size, color, bold).also { column.addView(it) }
    private fun action(value: String, primary: Boolean = true, run: () -> Unit): Button = Button(this).apply {
        text = value; textSize = 16f; isAllCaps = false; minHeight = dp(54); setPadding(dp(16), dp(8), dp(16), dp(8))
        setTextColor(if (primary) Color.WHITE else ink); background = shape(if (primary) ink else Color.WHITE, dp(20).toFloat())
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
        setOnClickListener { run() }; column.addView(this)
    }
    private fun showHome() {
        screen = "home"; locked = false; base()
        addText("MOTION AIR", 14f, muted, true)
        addText(t("One more round.", "Otra partida más."), 34f, ink, true)
        val logo = ImageView(this).apply { setImageResource(com.motionair.controller.R.drawable.ic_motion_air); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        column.addView(logo, LinearLayout.LayoutParams(dp(154), dp(154)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(16) })
        addText(t("Your phone. Your next move.", "Tu teléfono. Tu próxima jugada."), 21f, ink, true)
        addText(t("Open Motion Air on your Mac or Windows PC. Keep both devices on the same Wi-Fi, then scan its pairing QR.",
            "Abre Motion Air en tu Mac o PC con Windows. Conecta ambos equipos a la misma Wi-Fi y escanea el QR de conexión."), color = muted)
        status = addText(if (pending) t("Pairing securely…", "Conectando de forma segura…") else errorText(homeError), color = if (pending) muted else Color.rgb(174, 45, 48))
        action(t("Scan computer QR", "Escanear QR del equipo")) {
            IntentIntegrator(this).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                .setPrompt(t("Scan the QR in the Motion Air launcher", "Escanea el QR del lanzador de Motion Air"))
                .setBeepEnabled(false).setOrientationLocked(true).initiateScan()
        }.isEnabled = !pending
        action(t("Paste pairing code", "Pegar código de conexión"), false) { pasteCode() }.isEnabled = !pending
        val computers = try { store.all() } catch (_: Exception) {
            addText(t("Saved connections could not be unlocked. Reset them to pair again.", "No se pudieron abrir las conexiones guardadas. Restablécelas para emparejar de nuevo."), color = muted)
            action(t("Reset saved connections", "Restablecer conexiones"), false) {
                AlertDialog.Builder(this).setMessage(t("Forget all saved computers?", "¿Olvidar todos los equipos?"))
                    .setNegativeButton(t("Cancel", "Cancelar"), null).setPositiveButton(t("Reset", "Restablecer")) { _, _ -> store.clear(); showHome() }.show()
            }; emptyList()
        }
        if (computers.isNotEmpty()) addText(t("YOUR COMPUTERS", "TUS EQUIPOS"), 13f, muted, true)
        computers.forEach { pc ->
            action(t("Connect to ", "Conectar a ") + pc.name, false) { connect(pc) }.isEnabled = !pending
            val forget = text(t("Forget ", "Olvidar ") + pc.name, 14f, muted).apply {
                minHeight = dp(48); gravity = Gravity.CENTER_VERTICAL; isClickable = true; isFocusable = true
                setOnClickListener { AlertDialog.Builder(this@MainActivity).setTitle(t("Forget this computer?", "¿Olvidar este equipo?"))
                    .setMessage(pc.name).setNegativeButton(t("Keep", "Conservar"), null)
                    .setPositiveButton(t("Forget", "Olvidar")) { _, _ -> store.remove(pc.id); showHome() }.show() }
            }; column.addView(forget)
        }
        action(if (spanish) "English" else "Español", false) {
            spanish = !spanish; getPreferences(MODE_PRIVATE).edit().putString("language", if (spanish) "es" else "en").apply(); showHome()
        }
        addText(t("Windows: open Motion Air.cmd. Mac: open Motion Air.command.", "Windows: abre Motion Air.cmd. Mac: abre Motion Air.command."), 13f, muted)
        addText(t("Built on Joypad Air by David García.", "Basado en Joypad Air de David García."), 12f, muted)
    }
    private fun pasteCode() {
        val input = EditText(this).apply { hint = "joypadair://pair?data=…"; minLines = 3; maxLines = 6; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE }
        AlertDialog.Builder(this).setTitle(t("Paste pairing code", "Pegar código de conexión")).setView(input)
            .setNegativeButton(t("Cancel", "Cancelar"), null).setPositiveButton(t("Continue", "Continuar")) { _, _ -> confirmCode(input.text.toString()) }.show()
    }
    @Deprecated("Legacy scanner integration")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) { result.contents?.let { confirmCode(it) }; return }
        super.onActivityResult(requestCode, resultCode, data)
    }
    private fun confirmCode(raw: String) {
        val invitation = try { Invitation.parse(raw) } catch (e: Exception) { homeError = if (e.message == "expired_code") "expired_code" else "invalid_code"; showHome(); return }
        AlertDialog.Builder(this).setTitle(t("Pair with this computer?", "¿Emparejar con este equipo?"))
            .setMessage(invitation.name + "\n\n" + t("Check that this name matches the computer showing the QR code.", "Comprueba que este nombre coincide con el equipo que muestra el QR."))
            .setNegativeButton(t("Cancel", "Cancelar"), null).setPositiveButton(t("Pair", "Emparejar")) { _, _ -> pair(invitation) }.show()
    }
    private fun pair(invitation: Invitation) {
        pending = true; homeError = ""; val token = ++generation
        val request = PairingClient(); pairing = request; showHome()
        worker.execute {
            val result = runCatching { request.claim(invitation, "${Build.MANUFACTURER} ${Build.MODEL}") }
            main.post {
                if (token != generation || isFinishing) return@post
                pending = false; pairing = null
                result.onSuccess { computer ->
                    try { store.save(computer); connect(computer) } catch (_: Exception) { homeError = "save_failed"; showHome() }
                }.onFailure { homeError = it.message ?: "pair_failed"; showHome() }
            }
        }
    }
    private fun connect(computer: SavedComputer) {
        screen = "controller"; showController(); session.connect(computer)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    private fun showController() {
        screen = "controller"; locked = false; base()
        addText("MOTION AIR", 14f, muted, true)
        status = addText(t("Connecting…", "Conectando…"), 26f, ink, true)
        detail = addText("", 14f, muted)
        val grips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; isBaselineAligned = false }
        fun grip(color: Int) = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(10), dp(14), dp(10), dp(14))
            background = shape(color, dp(34).toFloat())
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        val left = grip(red); val right = grip(blue)
        (left.layoutParams as LinearLayout.LayoutParams).marginEnd = dp(10)
        grips.addView(left); grips.addView(right); column.addView(grips, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
        fun pad(parent: LinearLayout, id: String, label: String, fullLabel: String = label) = PadButton(this, label) { session.button(id, it) }.also {
            it.contentDescription = fullLabel; it.layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(6) }
            parent.addView(it); controls.add(it)
        }
        pad(left, "minus", "−", t("Minus", "Menos"))
        val stick = MenuStick(this) { x, y -> session.stick(x, y) }; left.addView(stick, LinearLayout.LayoutParams(-1, dp(158))); controls.add(stick)
        left.addView(text(t("MOVE", "MOVER"), 12f, ink, true).apply { gravity = Gravity.CENTER })
        pad(right, "plus", "+", t("Plus / pause", "Más / pausa"))
        pad(right, "x", "X")
        val middle = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL; isBaselineAligned = false }
        right.addView(middle)
        val yButton = pad(middle, "y", "Y")
        (yButton.layoutParams as LinearLayout.LayoutParams).marginEnd = dp(8)
        pad(middle, "a", "A", t("A / select", "A / seleccionar"))
        pad(right, "b", "B", t("B / back", "B / volver"))
        right.addView(text(t("PLAY", "JUGAR"), 12f, ink, true).apply { gravity = Gravity.CENTER })
        fun row(vararg buttons: Pair<String, String>) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
            buttons.forEach { (id, title) ->
                val view = PadButton(this, title) { session.button(id, it) }
                view.contentDescription = when (id) {
                    "dpad_left" -> t("D-pad left", "Cruceta izquierda")
                    "dpad_up" -> t("D-pad up", "Cruceta arriba")
                    "dpad_down" -> t("D-pad down", "Cruceta abajo")
                    "dpad_right" -> t("D-pad right", "Cruceta derecha")
                    else -> title
                }
                row.addView(view, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(5) }); controls.add(view)
            }
            column.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        }
        row("dpad_left" to "←", "dpad_up" to "↑", "dpad_down" to "↓", "dpad_right" to "→")
        row("l" to "L", "zl" to "ZL", "zr" to "ZR", "r" to "R")
        row("sl" to "SL", "sr" to "SR")
        motionButton = action(t("Enable Motion", "Activar movimiento")) { session.setMotion(!session.motion) }
        lockButton = action(t("Dance Lock", "Bloqueo para bailar"), false) { session.releaseControls(); showLock() }
        action(t("Reconnect", "Reconectar"), false) { session.computer?.let { connect(it) } }
        action(t("Disconnect", "Desconectar"), false) { session.disconnect(); window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); showHome() }
        refresh()
    }
    private fun showLock() {
        locked = true; base(true)
        addText("MOTION AIR", 14f, Color.LTGRAY, true)
        addText(t("All moves.\nNo mis-taps.", "Todos los pasos.\nSin pulsaciones."), 36f, Color.WHITE, true)
        addText(t("Controls locked. Hold your phone securely and keep Motion Air open while you dance.", "Controles bloqueados. Sujeta bien el teléfono y mantén Motion Air abierto mientras bailas."), 18f, Color.LTGRAY)
        status = addText("", 22f, Color.WHITE, true); detail = addText("", 15f, Color.LTGRAY)
        val unlock = action(t("Hold to unlock", "Mantén para desbloquear"), false) { showController() }
        val release = Runnable { if (locked) showController() }
        unlock.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { unlock.text = t("Keep holding…", "Sigue pulsando…"); main.postDelayed(release, 1500) }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { main.removeCallbacks(release); unlock.text = t("Hold to unlock", "Mantén para desbloquear") }
            }; true
        }
        // TalkBack activation invokes the click listener without requiring a long press.
        unlock.contentDescription = t("Unlock controls", "Desbloquear controles")
        addText(t("Hold for 1.5 seconds. Motion stays on.", "Mantén 1,5 segundos. El movimiento sigue activo."), 14f, Color.LTGRAY)
        refresh()
    }
    private fun refresh() {
        if (!::session.isInitialized || screen != "controller" || status == null) return
        val healthy = session.ready
        status?.text = when {
            session.connecting -> t("Connecting…", "Conectando…")
            !healthy -> t("Disconnected", "Desconectado")
            locked && session.motion && session.receivers > 0 && (session.receivedMotionAge ?: 9999.0) < 500 -> t("Sending motion", "Enviando movimiento")
            locked -> t("Waiting for the game", "Esperando al juego")
            else -> session.computer?.name ?: t("Connected", "Conectado")
        }
        detail?.text = when {
            session.error.isNotEmpty() -> errorText(session.error)
            !healthy -> t("Keep the desktop launcher open.", "Mantén abierto el lanzador del equipo.")
            !session.native -> t("The keyboard bridge is unavailable. Check the desktop launcher.", "El puente del teclado no está disponible. Revisa el lanzador.")
            !session.focused && !locked -> t("Click the Ryujinx window on your computer to use buttons.", "Haz clic en la ventana de Ryujinx para usar los botones.")
            !session.sensorsAvailable -> t("This phone has no gyroscope. Buttons are available.", "Este teléfono no tiene giroscopio. Puedes usar los botones.")
            session.motion && session.receivers == 0 -> t("Motion is on. Waiting for a compatible Ryujinx motion receiver.", "Movimiento activo. Esperando un receptor compatible de Ryujinx.")
            else -> "${session.latency} ms · ${session.sent} " + t("motion frames sent", "muestras enviadas")
        }
        controls.forEach { it.isEnabled = session.controlsEnabled; it.alpha = if (it.isEnabled) 1f else .5f }
        motionButton?.apply { isEnabled = session.canToggleMotion; text = if (session.motion) t("Disable Motion", "Desactivar movimiento") else t("Enable Motion", "Activar movimiento") }
        lockButton?.isEnabled = healthy && session.motion
    }
    private fun errorText(code: String): String = when (code) {
        "" -> ""
        "invalid_code" -> t("That is not a Motion Air pairing code. Scan the QR from your computer.", "Ese código no es de Motion Air. Escanea el QR del equipo.")
        "expired_code" -> t("The code expired or was already used. Generate a new QR on your computer.", "El código venció o ya se usó. Genera un nuevo QR en el equipo.")
        "too_many_attempts" -> t("Too many attempts. Wait five minutes and scan a new code.", "Demasiados intentos. Espera cinco minutos y escanea un nuevo código.")
        "save_failed" -> t("Could not save the connection securely. Unlock your phone and pair again.", "No se pudo guardar la conexión de forma segura. Desbloquea el teléfono y empareja de nuevo.")
        "pair_again", "identity_changed" -> t("Pair this computer again using its current QR code.", "Empareja de nuevo este equipo con su QR actual.")
        "update_bridge", "protocol_error" -> t("The bridge could not confirm the controller profile. Restart or update Motion Air on your computer.", "El puente no confirmó el perfil del mando. Reinicia o actualiza Motion Air en el equipo.")
        "sensors_missing" -> t("The motion sensors are unavailable on this phone.", "Los sensores de movimiento no están disponibles en este teléfono.")
        "connection_lost" -> t("Connection lost. Keep both devices on the same Wi-Fi and tap Reconnect. Motion starts off.", "Se perdió la conexión. Conecta ambos equipos a la misma Wi-Fi y pulsa Reconectar. El movimiento empieza desactivado.")
        else -> t("Could not connect. Keep the launcher open and both devices on the same Wi-Fi. If the computer’s address changed, scan a new QR.", "No se pudo conectar. Mantén el lanzador abierto y ambos equipos en la misma Wi-Fi. Si cambió la dirección del equipo, escanea un nuevo QR.")
    }
    override fun onPause() {
        super.onPause(); generation++; pairing?.cancel(); pairing = null; pending = false
        // A permission sheet or a partially covering activity can pause us
        // while we remain visible. Release touches without stopping tracking.
        if (::session.isInitialized) session.releaseControls()
    }
    override fun onStop() {
        super.onStop()
        if (::session.isInitialized) session.disconnect()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (locked) showController()
    }
    @Deprecated("Legacy back navigation")
    override fun onBackPressed() {
        if (locked) return
        if (screen == "controller") { session.disconnect(); window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); showHome() } else super.onBackPressed()
    }
    override fun onDestroy() { pairing?.cancel(); if (::session.isInitialized) session.disconnect(); worker.shutdownNow(); main.removeCallbacksAndMessages(null); super.onDestroy() }
}
