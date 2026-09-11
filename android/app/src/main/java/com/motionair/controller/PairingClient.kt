package com.motionair.controller

import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray

class PairingClient {
    @Volatile private var current: Call? = null
    @Volatile private var cancelled = false
    fun cancel() { cancelled = true; current?.cancel() }
    fun claim(invitation: Invitation, phoneName: String): SavedComputer {
        val client = PinnedClient.create(invitation.fingerprint)
        try {
            for (host in invitation.hosts) {
                if (cancelled) error("cancelled")
                val body = JSONObject().put("code", invitation.code).put("name", phoneName.take(64))
                val request = Request.Builder().url("https://$host:${invitation.port}/api/pairing/claim")
                    .post(body.toString().toRequestBody("application/json".toMediaType())).build()
                val call = client.newCall(request); current = call
                val response = try { call.execute() } catch (_: java.io.IOException) { continue }
                response.use {
                    if (!it.isSuccessful) error(when (it.code) { 403 -> "expired_code"; 429 -> "too_many_attempts"; else -> "pair_failed" })
                    val reply = JSONObject(it.body?.string() ?: error("pair_failed"))
                    require(reply.getString("id") == invitation.id) { "identity_changed" }
                    val saved = SavedComputer.from(JSONObject(reply.toString()).put("fingerprint", invitation.fingerprint)
                        .put("hosts", JSONArray(listOf(host) + invitation.hosts.filter { h -> h != host })).put("port", invitation.port))
                    if (cancelled) error("cancelled")
                    return saved
                }
            }
            error("connect_failed")
        } finally { current = null; client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll() }
    }
}
