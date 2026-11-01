package com.project.bcv.feature.discovery.domain

/** A hardware total is since boot, not since the journey. Never credit that initial total. */
data class SensorStepProgress(val total: Long = 0, val lastRaw: Long? = null, val boot: Int = -1) {
    fun counter(raw: Long, currentBoot: Int): SensorStepProgress {
        if (raw < 0) return this
        val delta = if (lastRaw != null && boot == currentBoot && raw >= lastRaw) raw - lastRaw else 0L
        return SensorStepProgress(total + delta, raw, currentBoot)
    }

    fun detected() = copy(total = total + 1)
}
