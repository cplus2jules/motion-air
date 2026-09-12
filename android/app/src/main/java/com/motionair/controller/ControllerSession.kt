package com.motionair.controller

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.UUID

class ControllerSession(context: Context, private val changed: () -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val sensors = PhoneSensors(context)
    private var generation = 0
    private var socket: WebSocket? = null
    private var client: OkHttpClient? = null
    private var pendingMotion: Boolean? = null
    private var configGeneration = 0
    private var sessionId = UUID.randomUUID().toString()
    private var sequence = 0L
    private var lastPong = 0L
    private var lastUI = 0L
    private val buttonTiming = mutableMapOf<String, ButtonPressTiming>()
    private val buttonTasks = mutableMapOf<String, Runnable>()
    var computer: SavedComputer? = null; private set
    var ready = false; private set
    var connecting = false; private set
    var motion = false; private set
    var native = false; private set
    var focused = false; private set
    var receivers = 0; private set
    var sent = 0L; private set
    var receivedMotionAge: Double? = null; private set
    var latency = 0L; private set
    var error = ""; private set
    val sensorsAvailable get() = sensors.available
    val controlsEnabled get() = ready && native && focused
    val canToggleMotion get() = ready && pendingMotion == null && sensors.available

    fun connect(target: SavedComputer) {
        disconnect(); computer = target; connecting = true; error = ""; changed()
        client = PinnedClient.create(target.fingerprint)
        val token = generation
        var index = 0
        fun attempt() {
            if (token != generation) return
            if (index >= target.hosts.size) { fail("connect_failed"); return }
            val host = target.hosts[index++]
            val request = Request.Builder().url("https://$host:${target.port}/controller")
                .header("Authorization", "Bearer ${target.token}").build()
            var opened = false
            socket = client!!.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) = dispatch(token) {
                    opened = true; lastPong = SystemClock.elapsedRealtime()
                }
                override fun onMessage(webSocket: WebSocket, text: String) = dispatch(token) {
                    if (text.length > 64 * 1024) { fail("protocol_error"); return@dispatch }
                    runCatching { receive(JSONObject(text)) }.onFailure { fail("protocol_error") }
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = dispatch(token) {
                    if (opened) fail("connection_lost") else if (response?.code == 401) fail("pair_again") else attempt()
                }
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) = dispatch(token) {
                    webSocket.close(code, null); fail(if (code == 4001) "pair_again" else "connection_lost")
                }
            })
        }
        attempt()
        main.postDelayed({ if (token == generation && !ready) fail("connect_failed") }, 30_000)
        main.postDelayed(object : Runnable {
            override fun run() {
                if (token != generation) return
                if (lastPong != 0L && SystemClock.elapsedRealtime() - lastPong > 6000) { fail("connection_lost"); return }
                if (ready) send(JSONObject().put("t", "ping").put("ts", SystemClock.elapsedRealtime()))
                main.postDelayed(this, 1000)
            }
        }, 1000)
    }
    private fun dispatch(token: Int, block: () -> Unit) { main.post { if (token == generation) block() } }
    private fun receive(message: JSONObject) {
        when (message.optString("t")) {
            "hello" -> {
                val profiles = message.optJSONArray("motionProfiles")
                if (profiles == null || (0 until profiles.length()).none { profiles.optString(it) == "just-dance" }) { fail("update_bridge"); return }
                native = message.optBoolean("native") && message.opt("accessibility") != false
                focused = message.optJSONObject("focus")?.optBoolean("ok") == true
                configure(false)
            }
            "config-ack" -> {
                val expected = pendingMotion ?: return
                if (!Protocol.acknowledges(message, expected)) { fail("protocol_error"); return }
                pendingMotion = null; ready = true; connecting = false; motion = expected
                if (motion) {
                    try { sensors.start { a, g, timestamp ->
                        // Keep motion disposable. A congested socket cannot queue seconds of stale frames.
                        if (!motion || socket == null || socket!!.queueSize() > 1024) return@start
                        val packet = Protocol.motion(a, g, timestamp, sessionId, ++sequence)
                        if (send(packet)) sent++
                        if (SystemClock.elapsedRealtime() - lastUI > 250) { lastUI = SystemClock.elapsedRealtime(); changed() }
                    } } catch (_: Exception) { fail("sensors_missing"); return }
                } else sensors.stop()
            }
            "pong" -> {
                val now = SystemClock.elapsedRealtime(); val echoed = message.optLong("ts", -1)
                if (echoed in (now - 6000)..now) { lastPong = now; latency = now - echoed }
            }
            "focus" -> {
                focused = message.opt("ok") == true
                if (!focused) releaseControls()
            }
            "accessibility" -> {
                native = message.opt("ok") == true
                if (!native) releaseControls()
            }
            "motion-status" -> { receivers = message.optInt("receivers"); receivedMotionAge = if (message.isNull("motionAgeMs")) null else message.optDouble("motionAgeMs") }
        }
        changed()
    }
    private fun configure(enabled: Boolean) {
        sensors.stop(); motion = false; pendingMotion = enabled
        val configToken = ++configGeneration
        if (!send(Protocol.config(enabled))) return
        val token = generation
        main.postDelayed({ if (token == generation && configToken == configGeneration && pendingMotion != null) fail("protocol_error") }, 3000)
    }
    fun setMotion(enabled: Boolean) { if (canToggleMotion) { configure(enabled); changed() } }
    fun button(id: String, down: Boolean) {
        if (down && !controlsEnabled) return
        if (!down && id !in buttonTiming) return
        val timing = buttonTiming.getOrPut(id) { ButtonPressTiming() }
        try { timing.append(down) } catch (_: IllegalStateException) { fail("connection_lost"); return }
        drainButton(id)
    }
    private fun drainButton(id: String) {
        buttonTasks.remove(id)?.let(main::removeCallbacks)
        val timing = buttonTiming[id] ?: return
        val down = timing.pop(SystemClock.elapsedRealtime())
        if (down != null && !send(JSONObject().put("t", "btn").put("k", id).put("d", down))) return
        if (!timing.hasPending) return
        val token = generation
        val task = Runnable { if (token == generation) drainButton(id) }
        buttonTasks[id] = task
        main.postDelayed(task, timing.delay(SystemClock.elapsedRealtime()))
    }
    fun stick(x: Double, y: Double) {
        if (controlsEnabled || (x == 0.0 && y == 0.0 && ready)) send(JSONObject().put("t", "stick").put("s", "R").put("x", x).put("y", y))
    }
    fun releaseControls() {
        val buttons = buttonTiming.keys.toList()
        buttonTasks.values.forEach(main::removeCallbacks); buttonTasks.clear(); buttonTiming.clear()
        for (id in buttons) {
            if (!send(JSONObject().put("t", "btn").put("k", id).put("d", false))) break
        }
        if (ready) stick(0.0, 0.0)
    }
    private fun send(message: JSONObject): Boolean {
        val connection = socket ?: return false
        if (connection.queueSize() > 16 * 1024 || !connection.send(message.toString())) { fail("connection_lost"); return false }
        return true
    }
    private fun fail(code: String) { disconnect(); error = code; changed() }
    fun disconnect() {
        generation++; sensors.stop()
        buttonTasks.values.forEach(main::removeCallbacks); buttonTasks.clear(); buttonTiming.clear()
        socket?.close(1000, "Controller stopped"); socket?.cancel(); socket = null
        client?.dispatcher?.cancelAll(); client?.connectionPool?.evictAll(); client?.dispatcher?.executorService?.shutdown(); client = null
        ready = false; connecting = false; motion = false; pendingMotion = null; focused = false; native = false
        receivers = 0; receivedMotionAge = null; sent = 0; sequence = 0; lastPong = 0
        sessionId = UUID.randomUUID().toString()
        changed()
    }
}
