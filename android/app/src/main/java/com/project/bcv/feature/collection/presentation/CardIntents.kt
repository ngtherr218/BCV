package com.project.bcv.feature.collection.presentation

import android.content.Context
import android.content.Intent
import com.project.bcv.feature.collection.domain.NewCollectionCard

object CardIntents {
    fun detail(context: Context, reward: NewCollectionCard) = Intent(context, CardDetailActivity::class.java)
        .putExtra(CardDetailActivity.EXTRA_CARD_ID, reward.card.id)
        .putExtra(CardDetailActivity.EXTRA_IS_NEW, reward.card.isNew)
        .putExtra(CardDetailActivity.EXTRA_CARD_NAME, reward.card.name)
        .putExtra(CardDetailActivity.EXTRA_PLACE_ID, reward.card.placeId)
        .putExtra(CardDetailActivity.EXTRA_IMAGE_URL, reward.card.imageUrl)
        .putExtra(CardDetailActivity.EXTRA_OWNED, reward.card.owned)
        .putExtra(CardDetailActivity.EXTRA_COLLECTION_NAME, reward.collectionName)
}
