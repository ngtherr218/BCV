package com.project.bcv.feature.collection.domain

/** One collection: how many of its cards the user owns, with up to four owned card photos. */
data class CollectionSummary(
    val id: Long,
    val name: String,
    val description: String?,
    val ownedCards: Int,
    val totalCards: Int,
    val previewImageUrls: List<String>
)

data class CollectionOverview(
    val ownedCards: Int,
    val totalCards: Int,
    val collections: List<CollectionSummary>
)

/** A card in a collection; [imageUrl] is only known once the card is owned. */
data class CollectionCard(
    val id: Long,
    val placeId: Long,
    val name: String,
    val imageUrl: String?,
    val owned: Boolean
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
    fun load(): CollectionOverview
    fun loadDetail(collectionId: Long): CollectionDetail
}
