package com.project.bcv.feature.account.interests.data

import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.core.network.ApiFailure
import com.project.bcv.feature.account.interests.domain.InterestRepository
import com.project.bcv.feature.account.interests.domain.InterestSnapshot
import com.project.bcv.feature.account.interests.domain.PlaceType
import org.json.JSONArray
import org.json.JSONObject

class InterestRepositoryImpl(
    private val remote: JsonApiClient,
    private val session: SessionPreferences
) : InterestRepository {
    override fun load(): InterestSnapshot {
        val response = remote.get("api/interests", token())
        val types = response.getJSONArray("placeTypes")
        val selected = response.getJSONArray("selectedPlaceTypeIds")
        return InterestSnapshot(
            (0 until types.length()).map { index ->
                types.getJSONObject(index).let { PlaceType(it.getLong("id"), it.getString("name")) }
            },
            (0 until selected.length()).map { selected.getLong(it) }.toSet(),
            response.optBoolean("requiresReselection", false)
        )
    }

    override fun save(ids: Set<Long>) {
        val body = JSONObject().put("placeTypeIds", JSONArray(ids.sorted()))
        remote.put("api/interests", body, token())
    }

    private fun token(): String = session.token
        ?: throw ApiFailure("UNAUTHORIZED", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
}
