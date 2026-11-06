package com.project.bcv.feature.discovery.domain

object MediaUploadPolicy {
    val acceptedMimeTypes = arrayOf("image/jpeg", "image/png", "image/webp", "video/mp4", "video/webm")
    const val IMAGE_MAX_BYTES = 10 * 1024 * 1024
    const val VIDEO_MAX_BYTES = 100 * 1024 * 1024

    fun maximumBytes(mime: String): Int {
        require(mime in acceptedMimeTypes) { "Chọn ảnh JPEG/PNG/WebP hoặc video MP4/WebM." }
        return if (mime.startsWith("image/")) IMAGE_MAX_BYTES else VIDEO_MAX_BYTES
    }

    fun validateVideo(durationMs: Long?, width: Int?, height: Int?, hasVideo: Boolean) {
        require(hasVideo && durationMs != null && width != null && height != null) { "Không đọc được thông tin video. Hãy chọn tệp MP4/WebM hợp lệ." }
        require(durationMs > 0 && durationMs <= 60_000) { "Video phải dài trên 0 và không quá 60 giây." }
        require(width > 0 && height > 0 && minOf(width, height) <= 1080 && maxOf(width, height) <= 1920) { "Video tối đa 1080p (1920×1080 hoặc 1080×1920)." }
    }
}
