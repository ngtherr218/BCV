package com.project.bcv.feature.questitem.quest.domain

/** A quest for the current day or week; progress comes from server-confirmed events only. */
data class Quest(
    val id: Long,
    val name: String,
    val conditionType: String,
    val target: Int,
    val progress: Int,
    val completed: Boolean,
    val rewardReceived: Boolean,
    val rewardName: String?,
    val rewardType: String?,
    val rewardQuantity: Int?,
    val rankingPoints: Int = 0,
    val awardedRankingPoints: Int = 0
)

data class QuestOverview(val updatedAtMillis: Long, val daily: List<Quest>, val weekly: List<Quest>)

interface QuestRepository {
    fun load(): QuestOverview
}
