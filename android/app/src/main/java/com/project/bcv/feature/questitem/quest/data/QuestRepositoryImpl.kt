package com.project.bcv.feature.questitem.quest.data

import com.project.bcv.core.network.ApiFailure
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.questitem.quest.domain.Quest
import com.project.bcv.feature.questitem.quest.domain.QuestOverview
import com.project.bcv.feature.questitem.quest.domain.QuestRepository
import org.json.JSONArray
import java.time.OffsetDateTime

class QuestRepositoryImpl(
    private val remote: JsonApiClient,
    private val session: SessionPreferences
) : QuestRepository {
    override fun load(): QuestOverview {
        val token = session.token
            ?: throw ApiFailure("UNAUTHORIZED", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
        val json = remote.get("api/quests", token)
        val updatedAt = runCatching { OffsetDateTime.parse(json.getString("updatedAt")).toInstant().toEpochMilli() }
            .getOrDefault(System.currentTimeMillis())
        return QuestOverview(updatedAt, quests(json.optJSONArray("daily")), quests(json.optJSONArray("weekly")))
    }

    private fun quests(array: JSONArray?) = (0 until (array?.length() ?: 0)).map { index ->
        array!!.getJSONObject(index).let { item ->
            fun text(key: String) = item.optString(key).takeIf { it.isNotBlank() && it != "null" }
            Quest(
                item.getLong("questId"), item.getString("name"), item.getString("conditionType"),
                item.getInt("targetValue"), item.getInt("progressValue"), item.getBoolean("completed"),
                item.getBoolean("rewardReceived"), text("rewardName"), text("rewardType"),
                if (item.isNull("rewardQuantity")) null else item.getInt("rewardQuantity"),
                if (item.isNull("rewardPoints")) null else item.optInt("rewardPoints")
            )
        }
    }
}
