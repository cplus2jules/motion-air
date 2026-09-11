package com.motionair.controller

import org.json.JSONObject
import org.json.JSONArray
import java.net.URI
import java.util.Base64
import java.util.UUID
import kotlin.math.PI

object Protocol {
    fun localHost(host: String): Boolean {
        if (host == "localhost") return true
        if (host.endsWith(".local")) return host.length <= 253 && host.split('.').all {
            it.matches(Regex("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?"))
        }
        val parts = host.split('.')
        if (parts.size != 4 || parts.any { !it.matches(Regex("0|[1-9][0-9]{0,2}")) }) return false
        val n = parts.map { it.toInt() }; if (n.any { it > 255 }) return false
        return n[0] == 10 || n[0] == 127 || (n[0] == 172 && n[1] in 16..31) ||
            (n[0] == 192 && n[1] == 168) || (n[0] == 169 && n[1] == 254)
    }
    fun uuid(value: String): Boolean = runCatching { UUID.fromString(value).toString().equals(value, true) }.getOrDefault(false)
    fun config(motion: Boolean) = JSONObject().put("t", "config").put("name", "Motion Air Android")
        .put("orientation", "portrait").put("motionProfile", "just-dance").put("motion", motion)
    fun acknowledges(message: JSONObject, motion: Boolean) = message.optString("t") == "config-ack" &&
        message.optString("orientation") == "portrait" && message.optString("motionProfile") == "just-dance" &&
        message.opt("motion") == motion
    fun motion(accel: FloatArray, gyro: FloatArray, timestampNs: Long, session: String, sequence: Long): JSONObject {
        require(accel.size == 3 && gyro.size == 3 && (accel + gyro).all { it.isFinite() })
        require(timestampNs >= 0 && timestampNs / 1000 <= 9_007_199_254_740_991 && sequence > 0 && sequence <= 9_007_199_254_740_991)
        // Android acceleration is specific force in m/s²; Core Motion's wire
        // convention is its negative in g. Gyroscope axes already agree.
        return JSONObject().put("t", "motion").put("sessionId", session).put("seq", sequence).put("ts", timestampNs / 1000)
            .put("ax", -accel[0] / 9.80665).put("ay", -accel[1] / 9.80665).put("az", -accel[2] / 9.80665)
            .put("gx", gyro[0] * 180 / PI).put("gy", gyro[1] * 180 / PI).put("gz", gyro[2] * 180 / PI)
    }
}

data class Invitation(val json: JSONObject) {
    val id: String = json.getString("id")
    val name: String = json.getString("name")
    val hosts = json.getJSONArray("hosts").let { a -> (0 until a.length()).map { a.getString(it) } }
    val port = json.getInt("port")
    val fingerprint: String = json.getString("fingerprint").lowercase()
    val code: String = json.getString("code")
    companion object {
        fun parse(raw: String, now: Long = System.currentTimeMillis()): Invitation {
            require(raw.toByteArray().size <= 8192) { "invalid_code" }
            val input = raw.trim()
            val content = if (input.startsWith("{")) input else {
                val uri = URI(input)
                require(uri.scheme == "joypadair" && uri.host == "pair" && uri.userInfo == null) { "invalid_code" }
                val payload = uri.rawQuery?.split('&')?.singleOrNull { it.startsWith("data=") }?.substring(5)
                require(payload != null) { "invalid_code" }
                String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)
            }
            val value = Invitation(JSONObject(content))
            require(value.json.getInt("v") == 1 && Protocol.uuid(value.id) && value.name.isNotBlank() && value.name.length <= 64 &&
                value.hosts.size in 1..8 && value.hosts.all(Protocol::localHost) && value.port in 1..65535 &&
                value.fingerprint.matches(Regex("[a-f0-9]{64}")) && value.code.matches(Regex("[0-9]{6}"))) { "invalid_code" }
            require(value.json.getDouble("expiresAt").isFinite() && value.json.getDouble("expiresAt") > now) { "expired_code" }
            return value
        }
    }
}

data class SavedComputer(val id: String, val name: String, val hosts: List<String>, val port: Int,
                         val fingerprint: String, val token: String, val clientID: String) {
    fun json() = JSONObject().put("id", id).put("name", name).put("hosts", JSONArray(hosts)).put("port", port)
        .put("fingerprint", fingerprint).put("token", token).put("clientID", clientID)
    companion object {
        fun from(json: JSONObject): SavedComputer {
            val hosts = json.getJSONArray("hosts").let { a -> (0 until a.length()).map { a.getString(it) } }
            val saved = SavedComputer(json.getString("id"), json.getString("name"), hosts, json.getInt("port"),
                json.getString("fingerprint"), json.getString("token"), json.getString("clientID"))
            require(Protocol.uuid(saved.id) && Protocol.uuid(saved.clientID) && saved.name.isNotBlank() && saved.name.length <= 64 &&
                hosts.size in 1..8 && hosts.all(Protocol::localHost) && saved.port in 1..65535 &&
                saved.fingerprint.matches(Regex("[a-f0-9]{64}")) && saved.token.matches(Regex("[A-Za-z0-9_-]{20,128}")))
            return saved
        }
    }
}
