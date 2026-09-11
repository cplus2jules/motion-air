package com.motionair.controller

import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
import java.util.UUID

class ProtocolTest {
    private fun invitation() = JSONObject().put("v", 1).put("id", UUID.randomUUID().toString()).put("name", "Gaming PC")
        .put("hosts", JSONArray(listOf("192.168.1.10", "gaming-pc.local"))).put("port", 3443)
        .put("fingerprint", "ab".repeat(32)).put("code", "012345").put("expiresAt", 5000)
    @Test fun acceptsTheSameQrAndJsonAsIphone() {
        val json = invitation()
        val qr = "joypadair://pair?data=" + Base64.getUrlEncoder().withoutPadding().encodeToString(json.toString().toByteArray())
        assertEquals(Invitation.parse(json.toString(), 1000).id, Invitation.parse(qr, 1000).id)
        assertEquals("012345", Invitation.parse(qr, 1000).code)
    }
    @Test fun rejectsUntrustedEndpointsAndExpiredInvitations() {
        for (host in listOf("8.8.8.8", "example.com", "127.1", "192.168.001.1", "pc.local/steal", "pc.local@evil.com", "-pc.local")) {
            assertFalse(host, Protocol.localHost(host))
            assertThrows(Exception::class.java) { Invitation.parse(invitation().put("hosts", JSONArray(listOf(host))).toString(), 1000) }
        }
        assertThrows(Exception::class.java) { Invitation.parse(invitation().toString(), 5000) }
        assertThrows(Exception::class.java) { Invitation.parse(invitation().put("fingerprint", "wrong").toString(), 1000) }
        assertThrows(Exception::class.java) { Invitation.parse(invitation().put("v", 2).toString(), 1000) }
    }
    @Test fun verifiesEveryMotionAckField() {
        val ack = Protocol.config(true).put("t", "config-ack")
        assertTrue(Protocol.acknowledges(ack, true)); assertFalse(Protocol.acknowledges(ack, false))
        assertFalse(Protocol.acknowledges(ack.put("orientation", "landscape-right"), true))
        assertFalse(Protocol.acknowledges(Protocol.config(true), true))
    }
    @Test fun androidSensorsKeepGravityUnitsAxesAndMonotonicTime() {
        val packet = Protocol.motion(floatArrayOf(0f, 0f, 9.80665f), floatArrayOf(Math.PI.toFloat(), 0f, 0f), 1_234_567_000L, "session", 3)
        assertEquals(-1.0, packet.getDouble("az"), .00001)
        assertEquals(180.0, packet.getDouble("gx"), .0001)
        assertEquals(1_234_567L, packet.getLong("ts")); assertEquals(3L, packet.getLong("seq"))
        val edge = Protocol.motion(floatArrayOf(9.80665f, 0f, 0f), floatArrayOf(0f, 1f, 0f), 1000, "session", 1)
        assertEquals(-1.0, edge.getDouble("ax"), .00001); assertEquals(180 / Math.PI, edge.getDouble("gy"), .0001)
        assertThrows(Exception::class.java) { Protocol.motion(floatArrayOf(Float.NaN, 0f, 0f), floatArrayOf(0f, 0f, 0f), 0, "s", 1) }
        assertThrows(Exception::class.java) { Protocol.motion(FloatArray(3), FloatArray(3), -1, "s", 1) }
    }
    @Test fun savedReplyCannotChangeIdentityFormatOrRedirectReconnect() {
        val saved = invitation().put("token", "abc_def-".repeat(4)).put("clientID", UUID.randomUUID().toString())
        assertEquals("Gaming PC", SavedComputer.from(saved).name)
        assertThrows(Exception::class.java) { SavedComputer.from(saved.put("hosts", JSONArray(listOf("evil.example")))) }
    }
}
