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
    private val actionBlue = Color.rgb(0, 113, 227)
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
    private lateinit var root: LinearLayout
    private lateinit var footer: LinearLayout
    private var headline: TextView? = null
    private var subtitle: TextView? = null
    private var footerState: String? = null
    private var welcome: WelcomeView? = null
    private var welcomeStep = 0
    private var welcomeReplay = false

    private fun t(en: String, es: String) = if (spanish) es else en
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val language = getPreferences(MODE_PRIVATE).getString("language", Locale.getDefault().language)
        spanish = language == "es"
        store = PairingStore(this)
        session = ControllerSession(this) { refresh() }
        welcomeStep = savedInstanceState?.getInt("welcomeStep") ?: 0
        welcomeReplay = savedInstanceState?.getBoolean("welcomeReplay") ?: false
        if (savedInstanceState?.getString("screen") == "welcome" || !getPreferences(MODE_PRIVATE).getBoolean("welcomeComplete", false)) {
            showWelcome(welcomeReplay)
        } else showController()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("screen", screen); outState.putInt("welcomeStep", welcomeStep); outState.putBoolean("welcomeReplay", welcomeReplay)
        super.onSaveInstanceState(outState)
    }
    private fun showWelcome(replay: Boolean = false) {
        session.releaseControls(); screen = "welcome"; welcomeReplay = replay; base()
        welcome = WelcomeView(this, spanish, welcomeStep, replay, { welcomeStep = it }) { pair ->
            getPreferences(MODE_PRIVATE).edit().putBoolean("welcomeComplete", true).apply()
            welcomeStep = 0; welcome = null
            if (pair) showHome() else showController()
        }
        root.removeAllViews(); root.addView(welcome, LinearLayout.LayoutParams(-1, -1))
    }
    private fun base(dark: Boolean = false) {
        status = null; detail = null; motionButton = null; lockButton = null; headline = null; subtitle = null; footerState = null; controls.clear()
        val background = if (dark) ink else canvas
        val frame = FrameLayout(this).apply { setBackgroundColor(background) }
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(20)) }
        scroll.addView(column, FrameLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        footer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(footer, LinearLayout.LayoutParams(-1, -2))
        frame.addView(root, FrameLayout.LayoutParams(minOf(resources.displayMetrics.widthPixels, dp(620)), -1, Gravity.CENTER_HORIZONTAL))
        frame.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        setContentView(frame)
        window.decorView.systemUiVisibility = if (dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }
    private fun header(title: String = "Motion Air", close: Boolean = false) {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(ImageView(this).apply { setImageResource(R.drawable.motion_mark); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }, LinearLayout.LayoutParams(dp(28), dp(28)))
        row.addView(text(title, 19f, ink, true).apply { gravity = Gravity.CENTER_VERTICAL }, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginStart = dp(10) })
        row.addView(Button(this).apply {
            text = if (close) t("Done", "Listo") else "⚙"; textSize = if (close) 15f else 26f
            isAllCaps = false; minimumWidth = 0; minWidth = 0; setPadding(dp(8), 0, dp(8), 0)
            setTextColor(actionBlue); background = shape(Color.TRANSPARENT)
            contentDescription = if (close) t("Done", "Listo") else t("Settings", "Ajustes")
            setOnClickListener { if (close) showController() else showSettings() }
        }, LinearLayout.LayoutParams(if (close) dp(76) else dp(48), dp(48)))
        column.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })
    }
    private fun showSettings() {
        session.releaseControls()
        AlertDialog.Builder(this).setTitle(t("Settings", "Ajustes"))
            .setItems(arrayOf(t("Show welcome guide", "Ver guía de bienvenida"), if (spanish) "English" else "Español", t("Your computers", "Tus equipos"))) { _, which ->
                when (which) {
                    0 -> { welcomeStep = 0; showWelcome(true) }
                    1 -> { spanish = !spanish; getPreferences(MODE_PRIVATE).edit().putString("language", if (spanish) "es" else "en").apply(); showController() }
                    else -> showHome()
                }
            }.setNegativeButton(t("Done", "Listo"), null).show()
    }
    private fun text(value: String, size: Float = 16f, color: Int = ink, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); setLineSpacing(dp(3).toFloat(), 1f)
        if (bold) typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
    }
    private fun addText(value: String, size: Float = 16f, color: Int = ink, bold: Boolean = false): TextView = text(value, size, color, bold).also { column.addView(it) }
    private fun action(value: String, primary: Boolean = true, parent: LinearLayout = column, run: () -> Unit): Button = Button(this).apply {
        stateListAnimator = null; elevation = 0f
        text = value; textSize = 16f; isAllCaps = false; minHeight = dp(54); setPadding(dp(16), dp(8), dp(16), dp(8))
        setTextColor(if (primary) Color.WHITE else actionBlue); background = shape(if (primary) actionBlue else Color.WHITE, dp(28).toFloat())
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
        setOnClickListener { run() }; parent.addView(this)
    }
    private fun showHome() {
        session.releaseControls(); screen = "connections"; locked = false; base(); header(t("Your computers", "Tus equipos"), true)
        addText(t("Meet your computer.", "Conoce a tu equipo."), 32f, ink, true)
        addText(t("Open Motion Air on your Mac or Windows PC. Keep both devices on the same Wi-Fi, then scan its pairing QR.",
            "Abre Motion Air en tu Mac o PC con Windows. Conecta ambos equipos a la misma Wi-Fi y escanea el QR de conexión."), color = muted)
        if (session.ready || session.connecting) {
            addText(session.computer?.name ?: "Motion Air", 20f, ink, true)
            action(t("Disconnect", "Desconectar"), false) { session.disconnect(); window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); showHome() }
        }
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
        header()
        headline = addText("", 32f, ink, true)
        subtitle = addText("", 16f, muted)
        status = action(t("Connect your computer", "Conecta tu equipo"), false) { showHome() }.apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL; minHeight = dp(70); textSize = 16f
            setTextColor(ink); background = shape(Color.WHITE, dp(20).toFloat())
        }
        val stacked = resources.configuration.screenWidthDp < 360 || resources.configuration.fontScale > 1.3f
        val grips = LinearLayout(this).apply { orientation = if (stacked) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL; isBaselineAligned = false }
        fun grip(color: Int) = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(10), dp(14), dp(10), dp(14))
            background = shape(color, dp(34).toFloat())
            minimumHeight = dp(266)
            layoutParams = if (stacked) LinearLayout.LayoutParams(-1, -2) else LinearLayout.LayoutParams(0, -2, 1f)
        }
        val left = grip(red); val right = grip(blue)
        (left.layoutParams as LinearLayout.LayoutParams).apply { if (stacked) bottomMargin = dp(12) else marginEnd = dp(10) }
        grips.addView(left); grips.addView(right); column.addView(grips, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
        fun pad(parent: LinearLayout, id: String, label: String, fullLabel: String = label) = PadButton(this, label) { session.button(id, it) }.also {
            it.contentDescription = fullLabel; it.layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(6) }
            parent.addView(it); controls.add(it)
        }
        fun gripHeader(parent: LinearLayout, title: String, id: String, key: String, accessibility: String, keyFirst: Boolean) {
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            val caption = text(title, 11f, ink, true).apply { gravity = if (keyFirst) Gravity.END else Gravity.START }
            if (!keyFirst) row.addView(caption, LinearLayout.LayoutParams(0, -2, 1f))
            pad(row, id, key, accessibility)
            if (keyFirst) row.addView(caption, LinearLayout.LayoutParams(0, -2, 1f))
            parent.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
        gripHeader(left, t("MOVE", "MOVER"), "minus", "−", t("Minus", "Menos"), true)
        val stick = MenuStick(this) { x, y -> session.stick(x, y) }; left.addView(stick, LinearLayout.LayoutParams(-1, dp(160))); controls.add(stick)
        left.addView(text(t("Find your next move", "Busca tu próxima jugada"), 12f, ink, true).apply { gravity = Gravity.CENTER })
        gripHeader(right, t("PLAY", "JUGAR"), "plus", "+", t("Plus / pause", "Más / pausa"), false)
        pad(right, "x", "X")
        val middle = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL; isBaselineAligned = false }
        right.addView(middle)
        val yButton = pad(middle, "y", "Y")
        (yButton.layoutParams as LinearLayout.LayoutParams).marginEnd = dp(28)
        pad(middle, "a", "A", t("A / select", "A / seleccionar"))
        pad(right, "b", "B", t("B / back", "B / volver"))
        right.addView(text(t("A selects · B goes back", "A selecciona · B vuelve"), 12f, ink, true).apply { gravity = Gravity.CENTER })
        fun row(vararg buttons: Pair<String, String>, parent: LinearLayout = column) {
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
            parent.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        }
        row("dpad_left" to "←", "dpad_up" to "↑", "dpad_down" to "↓", "dpad_right" to "→")
        val extra = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val more = action(t("More buttons  ⌄", "Más botones  ⌄"), false) {}
        more.setOnClickListener {
            session.releaseControls()
            extra.visibility = if (extra.visibility == View.GONE) View.VISIBLE else View.GONE
            more.text = if (extra.visibility == View.VISIBLE) t("Fewer buttons  ⌃", "Menos botones  ⌃") else t("More buttons  ⌄", "Más botones  ⌄")
        }
        row("sl" to "SL", "sr" to "SR", parent = extra)
        row("l" to "L", "zl" to "ZL", "zr" to "ZR", "r" to "R", parent = extra)
        column.addView(extra)
        detail = addText("", 14f, muted)
        refresh()
    }
    private fun controllerActions(healthy: Boolean) {
        footerState = "${session.ready}:${session.connecting}:${session.computer?.id}"; footer.removeAllViews(); motionButton = null; lockButton = null
        footer.setPadding(dp(20), dp(12), dp(20), dp(8)); footer.setBackgroundColor(Color.WHITE)
        if (healthy) {
            val row = LinearLayout(this).apply { orientation = if (resources.configuration.fontScale > 1.3f) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL }
            footer.addView(row)
            motionButton = action(t("Enable Motion", "Activar movimiento"), parent = row) { session.setMotion(!session.motion) }
            lockButton = action(t("Dance Lock", "Bloqueo para bailar"), false, row) { session.releaseControls(); showLock() }
            if (row.orientation == LinearLayout.HORIZONTAL) {
                motionButton!!.layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(10); bottomMargin = dp(8) }
                lockButton!!.layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { bottomMargin = dp(8) }
            }
            footer.addView(text(t("Keep the app open while playing.", "Mantén la app abierta al jugar."), 12f, muted).apply { gravity = Gravity.CENTER })
        } else {
            action(t("Pair a computer", "Conectar un equipo"), parent = footer) { showHome() }.isEnabled = !session.connecting
            if (session.computer != null) action(t("Reconnect", "Reconectar"), false, footer) { session.computer?.let { connect(it) } }.isEnabled = !session.connecting
        }
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
        if (!locked) {
            if (footerState != "${session.ready}:${session.connecting}:${session.computer?.id}") controllerActions(healthy)
            headline?.text = if (healthy) t("Let's play.", "Vamos a jugar.") else t("Ready, player one?", "¿Listo, jugador uno?")
            subtitle?.text = if (healthy) t("A selects. B goes back. You know the drill.", "A selecciona. B vuelve atrás. Ya sabes cómo.") else t("Your phone. Your computer. One more game.", "Tu teléfono. Tu equipo. Una partida más.")
        }
        status?.text = when {
            session.connecting -> t("Connecting…", "Conectando…")
            !healthy -> if (session.computer != null) t("Disconnected", "Desconectado") else t("Connect your computer\nPair once, then tap to reconnect", "Conecta tu equipo\nConecta una vez y vuelve con un toque")
            locked && session.motion && session.receivers > 0 && (session.receivedMotionAge ?: 9999.0) < 500 -> t("Sending motion", "Enviando movimiento")
            locked -> t("Waiting for the game", "Esperando al juego")
            else -> (session.computer?.name ?: t("Connected", "Conectado")) + "\n" + t("Connected · Player 1", "Conectado · Jugador 1")
        }
        detail?.text = when {
            session.error.isNotEmpty() -> errorText(session.error)
            !healthy -> if (session.computer == null) t("Connect your computer to start playing.", "Conecta tu equipo para empezar a jugar.") else t("Keep the desktop launcher open. Tap Reconnect to play again.", "Mantén el lanzador abierto. Pulsa Reconectar para volver a jugar.")
            !session.native && !locked -> t("The keyboard bridge is unavailable. Check the desktop launcher.", "El puente del teclado no está disponible. Revisa el lanzador.")
            !session.focused && !locked -> t("Click the Ryujinx window on your computer to use buttons.", "Haz clic en la ventana de Ryujinx para usar los botones.")
            !session.sensorsAvailable -> t("This phone has no gyroscope. Buttons are available.", "Este teléfono no tiene giroscopio. Puedes usar los botones.")
            session.motion && session.receivers == 0 -> t("Motion is on. Waiting for a compatible Ryujinx motion receiver.", "Movimiento activo. Esperando un receptor compatible de Ryujinx.")
            else -> "${session.latency} ms · ${session.sent} " + t("motion frames sent", "muestras enviadas")
        }
        controls.forEach { it.isEnabled = session.controlsEnabled; it.alpha = if (it.isEnabled) 1f else .5f }
        motionButton?.apply { isEnabled = session.canToggleMotion; alpha = if (isEnabled) 1f else .45f; text = if (session.motion) t("Motion on", "Movimiento activo") else t("Enable Motion", "Activar movimiento") }
        lockButton?.apply { isEnabled = healthy && session.motion; alpha = if (isEnabled) 1f else .45f }
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
        when (screen) {
            "welcome" -> if (welcome?.back() != true) { if (welcomeReplay) showController() else super.onBackPressed() }
            "connections" -> showController()
            else -> super.onBackPressed()
        }
    }
    override fun onDestroy() { pairing?.cancel(); if (::session.isInitialized) session.disconnect(); worker.shutdownNow(); main.removeCallbacksAndMessages(null); super.onDestroy() }
}
