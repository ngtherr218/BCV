package com.project.bcv.core.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.util.Arrays
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Loopback only: no backend, saved session or real uploaded files. */
@RunWith(AndroidJUnit4::class)
class MediaUploadStreamTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun fixedLengthUploadStreamsBeforeReadingTheWholeLargeVideo() {
        UploadSink().use { sink ->
            val size = 17L * 1024 * 1024 // Above the old 15 MB cap; generated without a large byte array.
            val source = GeneratedInput(size, sink.firstBodyBytes)
            val result = JsonApiClient(context, sink.baseUrl).postStream("api/places/12/media", "video/mp4",
                FAKE_TOKEN, size, 100L * 1024 * 1024) { source }
            val request = sink.received()
            assertTrue(result.getBoolean("ok"))
            assertEquals("POST /api/places/12/media HTTP/1.1", request.requestLine)
            assertEquals(size.toString(), request.headers["content-length"])
            assertEquals("Bearer $FAKE_TOKEN", request.headers["authorization"])
            assertEquals("video/mp4", request.headers["content-type"])
            assertEquals(size, request.bytes)
            assertTrue(request.correctBody)
            assertTrue(source.closed)
            assertTrue("Transport must consume bounded chunks", source.largestRead <= 81920)
        }
    }

    @Test fun unknownLengthDocumentUsesChunkedTransferWithTheSameAuthAndBody() {
        UploadSink().use { sink ->
            val source = GeneratedInput(256 * 1024L)
            JsonApiClient(context, sink.baseUrl).postStream("api/places/12/media", "video/webm",
                FAKE_TOKEN, -1, 100L * 1024 * 1024) { source }
            val request = sink.received()
            assertEquals("chunked", request.headers["transfer-encoding"])
            assertNull(request.headers["content-length"])
            assertEquals("Bearer $FAKE_TOKEN", request.headers["authorization"])
            assertEquals("video/webm", request.headers["content-type"])
            assertEquals(256 * 1024L, request.bytes)
            assertTrue(request.correctBody)
            assertTrue(source.closed)
        }
    }

    @Test fun byteCapStopsUnknownLengthUploadsAndClosesTheDocument() {
        UploadSink().use { sink ->
            val source = GeneratedInput(256 * 1024L)
            val cap = 100 * 1024L
            val result = runCatching {
                JsonApiClient(context, sink.baseUrl).postStream("api/places/12/media", "video/mp4",
                    FAKE_TOKEN, -1, cap) { source }
            }
            assertTrue("Oversized source must fail", result.isFailure)
            assertTrue("Oversized document must be closed", source.closed)
            assertTrue("No bytes beyond cap may reach the server", sink.received().bytes <= cap)
        }
    }

    private class GeneratedInput(private val size: Long, private val serverRead: CountDownLatch? = null) : InputStream() {
        private var offset = 0L
        var largestRead = 0; private set
        var closed = false; private set
        override fun read(): Int = if (offset++ < size) BODY_BYTE else -1
        override fun read(buffer: ByteArray, start: Int, length: Int): Int {
            if (offset >= size) return -1
            // If a future implementation buffers the entire source before sending, this fails.
            if (offset >= size / 2 && serverRead != null) check(serverRead.await(5, TimeUnit.SECONDS)) {
                "The server must receive data while the source is still being read."
            }
            largestRead = maxOf(largestRead, length)
            val count = minOf(length.toLong(), size - offset).toInt()
            Arrays.fill(buffer, start, start + count, BODY_BYTE.toByte())
            offset += count
            return count
        }
        override fun close() { closed = true }
    }

    private data class Received(val requestLine: String, val headers: Map<String, String>, val bytes: Long, val correctBody: Boolean)

    private class UploadSink : Closeable {
        private val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).apply { soTimeout = 10_000 }
        private val executor = Executors.newSingleThreadExecutor()
        val firstBodyBytes = CountDownLatch(1)
        val baseUrl = "http://127.0.0.1:${server.localPort}/"
        private val result = executor.submit<Received> {
            server.accept().use { socket ->
                socket.soTimeout = 10_000
                val input = BufferedInputStream(socket.getInputStream())
                val requestLine = line(input)
                val headers = linkedMapOf<String, String>()
                while (true) {
                    val value = line(input)
                    if (value.isEmpty()) break
                    headers[value.substringBefore(':').lowercase(Locale.ROOT)] = value.substringAfter(':').trim()
                }
                var bytes = 0L
                var correctBody = true
                val buffer = ByteArray(8192)
                fun consume(length: Long) {
                    var remaining = length
                    while (remaining > 0) {
                        val count = input.read(buffer, 0, minOf(remaining, buffer.size.toLong()).toInt())
                        if (count < 0) return
                        firstBodyBytes.countDown()
                        for (i in 0 until count) if (buffer[i].toInt() != BODY_BYTE) correctBody = false
                        bytes += count; remaining -= count
                    }
                }
                if (headers["transfer-encoding"].equals("chunked", true)) {
                    while (true) {
                        val sizeLine = line(input)
                        if (sizeLine.isEmpty()) break // Client disconnected after a size-limit error.
                        val count = sizeLine.substringBefore(';').trim().toLong(16)
                        if (count == 0L) { while (line(input).isNotEmpty()) Unit; break }
                        consume(count)
                        line(input) // CRLF after this chunk.
                    }
                } else consume(headers["content-length"]?.toLong() ?: 0L)
                runCatching {
                    socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 11\r\nConnection: close\r\n\r\n{\"ok\":true}".toByteArray(Charsets.US_ASCII))
                    socket.getOutputStream().flush()
                }
                Received(requestLine, headers, bytes, correctBody)
            }
        }
        fun received(): Received = result.get(15, TimeUnit.SECONDS)
        override fun close() { server.close(); executor.shutdownNow() }

        private fun line(input: InputStream): String {
            val text = StringBuilder()
            while (true) {
                val next = input.read()
                if (next == -1 || next == 10) return text.toString().removeSuffix("\r")
                check(text.length < 8192) { "Unexpected long HTTP header" }
                text.append(next.toChar())
            }
        }
    }

    companion object {
        private const val FAKE_TOKEN = "local-stream-test-token"
        private const val BODY_BYTE = 42
    }
}
