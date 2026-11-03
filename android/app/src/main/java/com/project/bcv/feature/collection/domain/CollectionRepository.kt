package com.project.bcv.feature.collection.domain

/** One collection: how many of its cards the user owns, with up to four owned card photos. */
data class CollectionSummary(
    val id: Long,
    val name: String,
    val description: String?,
    val ownedCards: Int,
    val totalCards: Int,
    val previewImageUrls: List<String>,
    val newCards: Int = 0
)

data class CollectionOverview(
    val ownedCards: Int,
    val totalCards: Int,
    val collections: List<CollectionSummary>,
    val newCards: Int = 0,
    val newItems: List<NewCollectionCard> = emptyList()
)

data class NewCollectionCard(val card: CollectionCard, val collectionId: Long, val collectionName: String)

/** A card in a collection; [imageUrl] is only known once the card is owned. */
data class CollectionCard(
    val id: Long,
    val placeId: Long,
    val name: String,
    val imageUrl: String?,
    val owned: Boolean,
    val isNew: Boolean = false
)

data class CollectionDetail(
    val id: Long,
    val name: String,
    val description: String?,
    val ownedCards: Int,
    val totalCards: Int,
    val cards: List<CollectionCard>
)

interface CollectionRepository {
    fun invalidate()
    fun loadBadgeCount(): Int
    fun markSeen(cardId: Long)
    fun load(): CollectionOverview
    fun loadDetail(collectionId: Long): CollectionDetail
}
