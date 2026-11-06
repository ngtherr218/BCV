package com.project.bcv.core.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.util.Base64
import com.project.bcv.core.network.MediaUrlPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Public HTTPS/data images can be cached; authenticated content is always revalidated. */
object RemoteImages {
    private const val MAX_IMAGE_BYTES = 16 * 1024 * 1024
    private val dataHeader = Regex("data:image/(jpeg|png|webp);base64,")
    private val downloads = Semaphore(4)
    private val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    private fun cacheKey(url: String, sample: Int): String {
        val source = if (url.startsWith("data:")) MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray()).joinToString("") { "%02x".format(it) } else url
        return "$sample|$source"
    }

    fun cached(url: String, sample: Int): Bitmap? = cache.get(cacheKey(url, sample))

    fun supports(url: String) = url.startsWith("https://") || dataHeader.find(url)?.range?.first == 0

    /** Decodes [url] at 1/[sample] size; returns null when it cannot be fetched. */
    suspend fun load(url: String, sample: Int): Bitmap? {
        if (!supports(url)) return null
        cached(url, sample)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            try {
                if (url.startsWith("data:")) decodeDataImage(url, sample)
                else downloads.withPermit { decode(download(url), sample) }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { null }
        }
        bitmap?.let { cache.put(cacheKey(url, sample), it) }
        return bitmap
    }

    /** No redirects or shared bitmap cache: revoked/private media must not outlive its authorization. */
    suspend fun loadAuthenticated(baseUrl: String, contentUrl: String, token: String, sample: Int): Bitmap? {
        if (token.isBlank()) return null
        val url = MediaUrlPolicy.authenticatedUrl(baseUrl, contentUrl) ?: return null
        return withContext(Dispatchers.IO) {
            try { downloads.withPermit { decode(download(url, token), sample) } }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { null }
        }
    }

    internal fun decodeDataImage(value: String, sample: Int): Bitmap? {
        val header = dataHeader.find(value)?.takeIf { it.range.first == 0 } ?: return null
        val encoded = value.substring(header.range.last + 1)
        if (encoded.length > MAX_IMAGE_BYTES * 4 / 3 + 8) return null
        return runCatching { decode(Base64.decode(encoded, Base64.DEFAULT), sample) }.getOrNull()
    }

    private fun decode(bytes: ByteArray, sample: Int): Bitmap? {
        if (bytes.isEmpty() || bytes.size > MAX_IMAGE_BYTES) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampling = sample.coerceAtLeast(1)
        val maxPixels = if (sample >= 4) 262_144 else 4_000_000
        while (bounds.outWidth.toLong() * bounds.outHeight / sampling / sampling > maxPixels) sampling *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sampling })
    }

    private fun download(url: String, token: String? = null): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = if (token == null) 8_000 else 30_000
        connection.instanceFollowRedirects = token == null
        connection.setRequestProperty("User-Agent", "BCV Android App (educational demo)")
        if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
        return try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "Không tải được ảnh." }
            require(connection.contentLengthLong <= MAX_IMAGE_BYTES) { "Ảnh quá lớn." }
            connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= MAX_IMAGE_BYTES) { "Ảnh quá lớn." }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } finally { connection.disconnect() }
    }
}
