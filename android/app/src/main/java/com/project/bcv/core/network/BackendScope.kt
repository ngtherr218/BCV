package com.project.bcv.core.network

import java.net.URI
import java.security.MessageDigest
import java.util.Locale

/** Keep device sessions and raw journey counters separate when switching API/database. */
object BackendScope {
    fun preferenceFile(name: String, apiBaseUrl: String): String {
        val uri = URI(apiBaseUrl.trim())
        val scheme = requireNotNull(uri.scheme).lowercase(Locale.ROOT)
        val host = requireNotNull(uri.host).lowercase(Locale.ROOT)
        val port = when {
            uri.port == -1 || (scheme == "https" && uri.port == 443) || (scheme == "http" && uri.port == 80) -> ""
            else -> ":${uri.port}"
        }
        val path = uri.path.orEmpty().trimEnd('/')
        // Preserve the current local BCV data. Every deployed backend gets independent storage.
        if (scheme == "http" && host in setOf("127.0.0.1", "localhost", "10.0.2.2") &&
            port == ":5081" && path.isEmpty()) return name
        val endpoint = "$scheme://$host$port$path"
        val digest = MessageDigest.getInstance("SHA-256").digest(endpoint.toByteArray(Charsets.UTF_8))
            .take(8).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return "${name}_$digest"
    }
}
