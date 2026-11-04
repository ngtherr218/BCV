package com.project.bcv.feature.collection.data

import com.project.bcv.core.network.ApiFailure
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.collection.domain.CollectionCard
import com.project.bcv.feature.collection.domain.CollectionDetail
import com.project.bcv.feature.collection.domain.CollectionOverview
import com.project.bcv.feature.collection.domain.CollectionRepository
import com.project.bcv.feature.collection.domain.CollectionSummary

class CollectionRepositoryImpl(
    private val remote: JsonApiClient,
    private val session: SessionPreferences
) : CollectionRepository {
    override fun loadDetail(collectionId: Long): CollectionDetail {
        val json = remote.get("api/collections/$collectionId", token())
        val array = json.optJSONArray("cards")
        val cards = (0 until (array?.length() ?: 0)).map { index ->
            array!!.getJSONObject(index).let { card ->
                CollectionCard(
                    card.getLong("cardId"),
                    card.getLong("placeId"),
                    card.getString("name"),
                    card.optString("imageUrl").takeIf { it.isNotBlank() && it != "null" },
                    card.getBoolean("owned")
                )
            }
        }
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
                    (0 until (images?.length() ?: 0)).map { images!!.getString(it) }
                )
            }
        }
        return CollectionOverview(json.getInt("ownedCards"), json.getInt("totalCards"), collections)
    }
}
