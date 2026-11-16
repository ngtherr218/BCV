package com.project.bcv.core.ui

import java.io.File
import java.security.MessageDigest

/** Original public bytes are shared by thumbnail/detail decodes. Private images never enter here. */
internal class PublicImageDiskCache(private val directory: File) {
    private val lifetime = 24 * 60 * 60 * 1000L
    private val budget = 48 * 1024 * 1024L
    private fun file(url: String) = File(directory, MessageDigest.getInstance("SHA-256")
        .digest(url.toByteArray()).joinToString("") { "%02x".format(it) })

    fun read(url: String): ByteArray? = runCatching {
        val entry = file(url)
        if (!entry.isFile || System.currentTimeMillis() - entry.lastModified() > lifetime || entry.length() > 16 * 1024 * 1024) {
            entry.delete(); null
        } else entry.readBytes().takeIf { it.isNotEmpty() }
    }.getOrNull()

    @Synchronized fun write(url: String, bytes: ByteArray) {
        runCatching {
            if(bytes.isEmpty() || bytes.size > 16 * 1024 * 1024) return
            directory.mkdirs()
            val destination = file(url)
            val temporary = File.createTempFile("image-", ".tmp", directory)
            try {
                temporary.writeBytes(bytes)
                if(!temporary.renameTo(destination)) temporary.delete()
            } finally { temporary.delete() }
            val entries = directory.listFiles()?.filter { !it.name.endsWith(".tmp") }?.sortedBy { it.lastModified() }.orEmpty()
            var total = entries.sumOf { it.length() }
            for(entry in entries) { if(total<=budget) break; val size=entry.length(); if(entry.delete())total-=size }
        }
    }
}
