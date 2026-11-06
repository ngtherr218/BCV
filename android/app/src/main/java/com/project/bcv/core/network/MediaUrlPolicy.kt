package com.project.bcv.core.network

import java.net.URI

/** Only these application endpoints may receive a session token from an image/video player. */
object MediaUrlPolicy {
    private val contentPath = Regex("/api/(community-media/[1-9][0-9]*/content|explore/places/[1-9][0-9]*/videos/[1-9][0-9]*|places/[1-9][0-9]*/thumbnail)")

    fun authenticatedUrl(baseUrl: String, value: String): String? = runCatching {
        val base = URI(baseUrl)
        if (base.scheme !in listOf("https", "http") || base.host.isNullOrBlank() || base.rawUserInfo != null) return null
        val target = base.resolve(URI(value))
        if (!base.scheme.equals(target.scheme, true) || !base.host.equals(target.host, true) || port(base) != port(target)) return null
        if (target.rawUserInfo != null || target.rawQuery != null || target.rawFragment != null) return null
        // Match raw paths: encoded slashes/dot segments and open redirects are never accepted.
        if (!contentPath.matches(target.rawPath.orEmpty()) || value.contains('\\')) return null
        if (URI(value).rawPath != target.rawPath && !URI(value).isAbsolute) return null
        target.toASCIIString()
    }.getOrNull()

    private fun port(uri: URI) = if (uri.port >= 0) uri.port else if (uri.scheme.equals("https", true)) 443 else 80
}
