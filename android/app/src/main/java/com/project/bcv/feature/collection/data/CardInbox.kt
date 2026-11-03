package com.project.bcv.feature.collection.data

import android.content.Context
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import java.security.MessageDigest

/** Retains confirmed journey rewards even when the connected API predates card badges. */
class CardInbox(context: Context, session: SessionPreferences = SessionPreferences(context), namespace: String = "card_inbox") {
    private val preferences = context.applicationContext.getSharedPreferences(namespace, Context.MODE_PRIVATE)
    private val account = MessageDigest.getInstance("SHA-256")
        .digest(session.token.orEmpty().toByteArray()).joinToString("") { "%02x".format(it) }

    fun recordJourney(journeyId: Long, placeId: Long): Boolean = synchronized(lock) {
        if (preferences.getBoolean("$account:journey:$journeyId", false)) return false
        val places = pendingPlaces().toMutableSet().apply { add(placeId.toString()) }
        preferences.edit().putBoolean("$account:journey:$journeyId", true)
            .putStringSet("$account:places", places).apply()
        true
    }

    fun pendingPlaces(): Set<String> = preferences.getStringSet("$account:places", emptySet())!!.toSet()
    fun isUnseen(cardId: Long, placeId: Long) = placeId.toString() in pendingPlaces() && !isSeen(cardId)
    fun isSeen(cardId: Long) = preferences.getBoolean("$account:seen:$cardId", false)
    fun rememberCards(cards: Map<Long, Long>) {
        val changed = cards.filter { (cardId, placeId) -> preferences.getLong("$account:card:$cardId", 0L) != placeId }
        if (changed.isEmpty()) return
        preferences.edit().apply {
            changed.forEach { (cardId, placeId) -> putLong("$account:card:$cardId", placeId) }
        }.apply()
    }
    fun markSeen(cardId: Long) = synchronized(lock) {
        val placeId = preferences.getLong("$account:card:$cardId", 0L)
        val places = pendingPlaces().toMutableSet().apply { remove(placeId.toString()) }
        preferences.edit().putBoolean("$account:seen:$cardId", true)
            .putStringSet("$account:places", places).apply()
    }
    fun dialogShown(journeyId: Long) = preferences.getBoolean("$account:dialog:$journeyId", false)
    fun markDialogShown(journeyId: Long) { preferences.edit().putBoolean("$account:dialog:$journeyId", true).apply() }

    private companion object { val lock = Any() }
}
