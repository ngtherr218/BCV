package com.project.bcv.core.ui

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicLong

/** Successful writes make the other tabs stale without discarding their visible content. */
object AppDataChanges {
    private val counter = AtomicLong()
    val revision: Long get() = counter.get()
    fun changed() { counter.incrementAndGet() }
}

/** In-memory freshness for a tab; writes refresh immediately, external changes within a minute. */
class TabRefresh(private val clock: () -> Long = { SystemClock.elapsedRealtime() }) {
    private var loadedAt: Long? = null
    private var loadedRevision = -1L
    private var requestedRevision: Long? = null
    private var invalidationRevision = 0L
    private var loadedInvalidation = 0L
    private var requestedInvalidation: Long? = null

    /** Call after the ViewModel's single-flight guard and before starting its request. */
    fun shouldRefresh(hasContent: Boolean, force: Boolean = false): Boolean {
        val revision = AppDataChanges.revision
        val previous = loadedAt
        val now = clock()
        val due = force || !hasContent || invalidationRevision != loadedInvalidation || previous == null ||
            loadedRevision != revision || now < previous || now - previous >= STALE_AFTER_MILLIS
        if (due) {
            requestedRevision = revision
            requestedInvalidation = invalidationRevision
        }
        return due
    }

    fun loaded() {
        loadedAt = clock()
        // Do not swallow a write that finished while this older GET was in flight.
        loadedRevision = requestedRevision ?: AppDataChanges.revision
        loadedInvalidation = requestedInvalidation ?: invalidationRevision
        requestedRevision = null
        requestedInvalidation = null
    }

    fun invalidate() { invalidationRevision++ }

    private companion object { const val STALE_AFTER_MILLIS = 60_000L }
}
