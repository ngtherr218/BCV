package com.project.bcv.feature.discovery.domain

object MediaUploadPolicy {
    val acceptedMimeTypes = arrayOf("image/jpeg", "image/png", "image/webp")
    const val IMAGE_MAX_BYTES = 10 * 1024 * 1024
    fun maximumBytes(mime: String): Int {
        require(mime in acceptedMimeTypes) { "Chỉ hỗ trợ ảnh JPEG/PNG/WebP." }
        return IMAGE_MAX_BYTES
    }
}
