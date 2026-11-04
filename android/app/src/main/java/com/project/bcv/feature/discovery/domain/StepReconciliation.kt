package com.project.bcv.feature.discovery.domain

enum class StepProvider { HEALTH_CONNECT, SENSOR }

/** Persisted, monotonic journey total. Shadow sensor steps never get added to a healthy primary. */
data class StepReconciliation(
    val provider: StepProvider,
    val total: Long = 0,
    val primaryRaw: Long? = null,
    val sensorAnchor: Long? = null,
    val lastPrimaryAdvanceAt: Long = 0
) {
    fun observe(primary: Long?, sensor: Long?, now: Long, failed: Boolean = false): StepReconciliation {
        val raw = primary?.takeIf { it >= 0 }
        val shadow = sensor?.takeIf { it >= 0 }
        if (provider == StepProvider.SENSOR) {
            val delta = if (shadow != null && sensorAnchor != null) (shadow - sensorAnchor).coerceAtLeast(0) else 0
            return copy(total = total + delta, sensorAnchor = shadow ?: sensorAnchor)
        }
        if (failed && primaryRaw == null && shadow != null) {
            val extra = sensorAnchor?.let { (shadow - it).coerceAtLeast(0) } ?: 0
            return copy(provider = StepProvider.SENSOR, total = total + extra, sensorAnchor = shadow,
                lastPrimaryAdvanceAt = now)
        }
        // First reading, or a reboot resetting elapsedRealtime, establishes a fresh observation window.
        if (primaryRaw == null || now < lastPrimaryAdvanceAt) {
            val baseline = copy(total = maxOf(total, raw ?: 0), primaryRaw = raw,
                sensorAnchor = shadow ?: sensorAnchor.takeIf { raw == 0L }, lastPrimaryAdvanceAt = now)
            return if (failed && shadow != null) baseline.copy(provider = StepProvider.SENSOR) else baseline
        }
        val extra = if (shadow != null && sensorAnchor != null) (shadow - sensorAnchor).coerceAtLeast(0) else 0
        val observedLongEnough = now - lastPrimaryAdvanceAt >= STALL_MILLIS
        if (!failed && raw != null && raw > primaryRaw) {
            // After time in background a writer may have advanced briefly, then stopped. Do not
            // discard the sensor window just because that partial update arrived before this read.
            if (shadow != null && observedLongEnough && extra - (raw - primaryRaw) >= STALL_STEPS) {
                return copy(provider = StepProvider.SENSOR, total = maxOf(raw, total + extra), sensorAnchor = shadow)
            }
            return copy(total = maxOf(total, raw), primaryRaw = raw, sensorAnchor = shadow,
                lastPrimaryAdvanceAt = now)
        }
        val stalled = observedLongEnough && extra >= STALL_STEPS
        if (shadow != null && (failed || stalled)) {
            // Pin the source for the rest of the journey. Late primary records cannot be counted again.
            return copy(provider = StepProvider.SENSOR, total = total + extra, sensorAnchor = shadow)
        }
        return if (sensorAnchor == null && shadow != null) copy(sensorAnchor = shadow) else this
    }

    companion object {
        /** Import old checkpoints without reading or subscribing to the removed Recording API. */
        fun restore(providerName: String, total: Long, primaryRaw: Long?, sensorAnchor: Long?,
                    lastPrimaryAdvanceAt: Long, sensorUsable: Boolean): StepReconciliation? {
            if (providerName == "RECORDING") {
                // The sensor anchor was saved alongside the last credited total. Use only its delta.
                // Without a usable sensor, Health Connect establishes its own cumulative baseline.
                return StepReconciliation(
                    if (sensorUsable) StepProvider.SENSOR else StepProvider.HEALTH_CONNECT,
                    total = total, sensorAnchor = if (sensorUsable) sensorAnchor else null)
            }
            val provider = runCatching { StepProvider.valueOf(providerName) }.getOrNull() ?: return null
            return StepReconciliation(provider, total, primaryRaw, sensorAnchor, lastPrimaryAdvanceAt)
        }

        const val STALL_MILLIS = 120_000L
        const val STALL_STEPS = 20L
    }
}
