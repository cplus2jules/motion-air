package com.motionair.controller

import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object PinnedClient {
    fun matches(certificate: X509Certificate, expected: String): Boolean {
        if (!expected.matches(Regex("[a-f0-9]{64}"))) return false
        val bytes = expected.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return MessageDigest.isEqual(MessageDigest.getInstance("SHA-256").digest(certificate.encoded), bytes)
    }
    fun create(fingerprint: String): OkHttpClient {
        require(fingerprint.matches(Regex("[a-f0-9]{64}")))
        val trust = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) { throw CertificateException() }
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                val leaf = chain?.firstOrNull() ?: throw CertificateException("Missing certificate")
                leaf.checkValidity()
                if (!matches(leaf, fingerprint)) throw CertificateException("Computer identity changed")
            }
        }
        val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), null) }
        return OkHttpClient.Builder().sslSocketFactory(ssl.socketFactory, trust)
            // The QR pins a certificate identity, independent of DHCP address/CN.
            // This verifier still checks that exact identity; it never trusts any certificate.
            .hostnameVerifier { _, session -> (session.peerCertificates.firstOrNull() as? X509Certificate)?.let { matches(it, fingerprint) } == true }
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    require(Protocol.localHost(hostname))
                    return Dns.SYSTEM.lookup(hostname).also { addresses ->
                        require(addresses.isNotEmpty() && addresses.all { it.isSiteLocalAddress || it.isLoopbackAddress || it.isLinkLocalAddress })
                    }
                }
            })
            .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
            .connectTimeout(3, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS).callTimeout(6, TimeUnit.SECONDS).build()
    }
}
