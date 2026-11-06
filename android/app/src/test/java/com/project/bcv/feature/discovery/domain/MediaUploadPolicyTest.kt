package com.project.bcv.feature.discovery.domain

import org.junit.Assert.*
import org.junit.Test

class MediaUploadPolicyTest {
    @Test fun newlyUploadedVideosUseTheSameLimitAsOfficialVideos() {
        assertEquals(100 * 1024 * 1024, MediaUploadPolicy.maximumBytes("video/mp4"))
        assertEquals(10 * 1024 * 1024, MediaUploadPolicy.maximumBytes("image/jpeg"))
        assertEquals(100 * 1024 * 1024, MediaUploadPolicy.maximumBytes("video/webm"))
        assertTrue(runCatching { MediaUploadPolicy.maximumBytes("image/gif") }.isFailure)
    }

    @Test fun portraitAndLandscape1080pAreAcceptedAtTheDurationBoundary() {
        MediaUploadPolicy.validateVideo(60_000, 1920, 1080, true)
        MediaUploadPolicy.validateVideo(1, 1080, 1920, true)
    }

    @Test fun longHighResolutionAndUnreadableVideosAreRejectedBeforeUpload() {
        assertTrue(runCatching { MediaUploadPolicy.validateVideo(60_001, 1920, 1080, true) }.isFailure)
        assertTrue(runCatching { MediaUploadPolicy.validateVideo(1000, 1921, 1080, true) }.isFailure)
        assertTrue(runCatching { MediaUploadPolicy.validateVideo(1000, 1081, 1081, true) }.isFailure)
        assertTrue(runCatching { MediaUploadPolicy.validateVideo(0, 1280, 720, true) }.isFailure)
        assertTrue(runCatching { MediaUploadPolicy.validateVideo(null, 1280, 720, true) }.isFailure)
        assertTrue(runCatching { MediaUploadPolicy.validateVideo(1000, null, 720, true) }.isFailure)
        assertTrue(runCatching { MediaUploadPolicy.validateVideo(1000, 1280, 720, false) }.isFailure)
    }
}
