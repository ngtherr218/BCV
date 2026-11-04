package com.project.bcv.feature.questitem.inventory.data

import com.project.bcv.core.network.ApiFailure
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.questitem.inventory.domain.FlightPlace
import com.project.bcv.feature.questitem.inventory.domain.FlightResult
import com.project.bcv.feature.questitem.inventory.domain.FlightTicketStatus
import com.project.bcv.feature.questitem.inventory.domain.ItemUseRepository
import com.project.bcv.feature.questitem.inventory.domain.RegionOption
import com.project.bcv.feature.questitem.inventory.domain.ShoesEffect
import com.project.bcv.feature.questitem.inventory.domain.ShoesJourney
import com.project.bcv.feature.questitem.inventory.domain.SpeedShoesStatus
import org.json.JSONObject

class ItemUseRepositoryImpl(
    private val remote: JsonApiClient,
    private val session: SessionPreferences
) : ItemUseRepository {
    private fun token() = session.token
        ?: throw ApiFailure("UNAUTHORIZED", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")

    override fun shoes(): SpeedShoesStatus = remote.get("api/items/speed-shoes", token()).toShoes()

    override fun useShoes(): SpeedShoesStatus = remote.post("api/items/speed-shoes/use", JSONObject(), token()).toShoes()

    override fun flight(): FlightTicketStatus {
        val json = remote.get("api/items/flight-ticket", token())
        val regions = json.getJSONArray("regions")
        return FlightTicketStatus(
            json.getString("name"), json.getBoolean("active"), json.getInt("quantity"),
            json.getBoolean("hasActiveJourney"), json.optString("currentRegion").takeUnless { json.isNull("currentRegion") },
            (0 until regions.length()).map { index ->
                regions.getJSONObject(index).let { RegionOption(it.getLong("regionId"), it.getString("name"), it.getInt("placeCount")) }
            }
        )
    }

    override fun places(regionId: Long): List<FlightPlace> {
        val places = remote.get("api/items/flight-ticket/regions/$regionId/places", token()).getJSONArray("places")
        return (0 until places.length()).map { index ->
            places.getJSONObject(index).let { FlightPlace(it.getLong("placeId"), it.getString("name"), it.getBoolean("discovered")) }
        }
    }

    override fun useFlight(placeId: Long): FlightResult {
        val json = remote.post("api/items/flight-ticket/use", JSONObject().put("placeId", placeId), token())
        return FlightResult(json.getString("region"), json.getString("place"), json.getInt("quantity"))
    }

    private fun JSONObject.toShoes() = SpeedShoesStatus(
        getString("name"), getBoolean("active"), getInt("quantity"), getDouble("multiplier"), getInt("range"),
        optJSONObject("journey")?.let {
            ShoesJourney(it.getLong("journeyId"), it.getString("destinationName"), it.getInt("recordedSteps"), it.getInt("requiredSteps"))
        },
        optJSONObject("effect")?.let { ShoesEffect(it.getDouble("multiplier"), it.getInt("range"), it.getInt("remainingSteps")) }
    )
}
