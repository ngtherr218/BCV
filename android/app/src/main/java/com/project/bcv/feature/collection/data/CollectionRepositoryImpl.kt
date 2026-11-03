package com.project.bcv.feature.collection.data

import com.project.bcv.core.network.ApiFailure
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.collection.domain.CollectionCard
import com.project.bcv.feature.collection.domain.CollectionDetail
import com.project.bcv.feature.collection.domain.CollectionOverview
import com.project.bcv.feature.collection.domain.CollectionRepository
import com.project.bcv.feature.collection.domain.CollectionSummary

class CollectionRepositoryImpl(
    private val remote: CollectionApi,
    private val session: SessionPreferences,
    private val inbox: CardInbox
) : CollectionRepository {
    private val details = java.util.concurrent.ConcurrentHashMap<Long, Pair<Long, org.json.JSONObject>>()
    private val generation = java.util.concurrent.atomic.AtomicLong()
    override fun invalidate() { generation.incrementAndGet(); details.clear() }

    override fun loadBadgeCount(): Int {
        val json = remote.get("api/collections", token())
        return if (json.has("newCards")) json.getInt("newCards") else inbox.pendingPlaces().size
    }

    override fun markSeen(cardId: Long) {
        try {
            remote.post("api/cards/$cardId/seen", org.json.JSONObject(), token())
        } catch (error: ApiFailure) {
            if (error.code != "NOT_FOUND") throw error
        }
        inbox.markSeen(cardId)
    }

    override fun loadDetail(collectionId: Long): CollectionDetail {
        val version = generation.get()
        val now = android.os.SystemClock.elapsedRealtime()
        val cached = details[collectionId]?.takeIf { now - it.first < 20_000 }
        val json = cached?.second ?: remote.get("api/collections/$collectionId", token()).also {
            if (generation.get() == version) details[collectionId] = android.os.SystemClock.elapsedRealtime() to it
        }
        val array = json.optJSONArray("cards")
        val cards = (0 until (array?.length() ?: 0)).map { index ->
            array!!.getJSONObject(index).let { card ->
                CollectionCard(
                    card.getLong("cardId"),
                    card.getLong("placeId"),
                    card.getString("name"),
                    card.optString("imageUrl").takeIf { it.isNotBlank() && it != "null" },
                    card.getBoolean("owned"), card.getBoolean("owned") && !inbox.isSeen(card.getLong("cardId")) &&
                        (card.optBoolean("isNew") || inbox.isUnseen(card.getLong("cardId"), card.getLong("placeId")))
                )
            }
        }
        inbox.rememberCards(cards.associate { it.id to it.placeId })
        return CollectionDetail(json.getLong("collectionId"), json.getString("name"),
            json.optString("description").takeIf { it.isNotBlank() && it != "null" },
            json.getInt("ownedCards"), json.getInt("totalCards"), cards)
    }

    private fun token(): String = session.token
        ?: throw ApiFailure("UNAUTHORIZED", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")

    override fun load(): CollectionOverview {
        val json = remote.get("api/collections", token())
        val array = json.optJSONArray("collections")
        val collections = (0 until (array?.length() ?: 0)).map { index ->
            array!!.getJSONObject(index).let { item ->
                val images = item.optJSONArray("previewImageUrls")
                CollectionSummary(
                    item.getLong("collectionId"),
                    item.getString("name"),
                    item.optString("description").takeIf { it.isNotBlank() && it != "null" },
                    item.getInt("ownedCards"),
                    item.getInt("totalCards"),
                    (0 until (images?.length() ?: 0)).map { images!!.getString(it) },
                    item.optInt("newCards")
                )
            }
        }
        val newItems = if (json.optInt("newCards") > 0 || inbox.pendingPlaces().isNotEmpty()) {
            collections.flatMap { collection ->
                loadDetail(collection.id).cards.filter { it.isNew }.map {
                    com.project.bcv.feature.collection.domain.NewCollectionCard(it, collection.id, collection.name)
                }
            }
        } else emptyList()
        val summaries = collections.map { collection ->
            collection.copy(newCards = newItems.count { it.collectionId == collection.id })
        }
        return CollectionOverview(json.getInt("ownedCards"), json.getInt("totalCards"), summaries,
            newItems.distinctBy { it.card.id }.size, newItems.distinctBy { it.card.id })
    }
}
