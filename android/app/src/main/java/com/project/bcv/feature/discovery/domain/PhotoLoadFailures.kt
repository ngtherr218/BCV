package com.project.bcv.feature.discovery.domain

/** A denied private thumbnail may be retried after its place is unlocked. */
internal class PhotoLoadFailures {
    private data class Attempt(val placeId: Long, val url: String, val discovered: Boolean)
    private val failed = mutableSetOf<Attempt>()
    fun contains(placeId: Long, url: String, discovered: Boolean) = Attempt(placeId, url, discovered) in failed
    fun add(placeId: Long, url: String, discovered: Boolean) { failed.add(Attempt(placeId, url, discovered)) }
    fun clear() = failed.clear()
}
