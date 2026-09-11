package com.motionair.controller

import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

class PinnedClientTest {
    @Test fun tlsAcceptsOnlyTheQrCertificateAndNeverFollowsRedirects() {
        val identity = HeldCertificate.Builder().commonName("Motion Air pinned computer").build()
        val certs = HandshakeCertificates.Builder().heldCertificate(identity).build()
        val pin = MessageDigest.getInstance("SHA-256").digest(identity.certificate.encoded).joinToString("") { "%02x".format(it) }
        val server = MockWebServer(); server.useHttps(certs.sslSocketFactory(), false); server.start()
        val client = PinnedClient.create(pin)
        val wrong = PinnedClient.create("00".repeat(32))
        try {
            server.enqueue(MockResponse().setBody("paired"))
            client.newCall(Request.Builder().url(server.url("/claim")).build()).execute().use { assertEquals("paired", it.body!!.string()) }
            assertThrows(java.io.IOException::class.java) { wrong.newCall(Request.Builder().url(server.url("/claim")).build()).execute() }
            server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", "https://example.com/steal"))
            client.newCall(Request.Builder().url(server.url("/claim")).header("Authorization", "Bearer never-forward").build()).execute().use { assertEquals(302, it.code) }
            assertEquals(2, server.requestCount)
        } finally {
            client.connectionPool.evictAll(); wrong.connectionPool.evictAll(); server.shutdown()
            client.dispatcher.executorService.shutdown(); wrong.dispatcher.executorService.shutdown()
        }
    }
}
