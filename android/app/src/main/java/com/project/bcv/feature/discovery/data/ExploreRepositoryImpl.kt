package com.project.bcv.feature.discovery.data

import com.project.bcv.core.network.ApiFailure
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.discovery.domain.ExploreJourney
import com.project.bcv.feature.discovery.domain.JourneyCardReward
import com.project.bcv.feature.discovery.domain.ExploreLocation
import com.project.bcv.feature.discovery.domain.ExploreMapPlace
import com.project.bcv.feature.discovery.domain.ExplorePlaceDetail
import com.project.bcv.feature.discovery.domain.ExploreRepository
import com.project.bcv.feature.discovery.domain.ExploreSnapshot
import com.project.bcv.feature.discovery.domain.RecommendedPlace
import com.project.bcv.feature.discovery.domain.Recommendations
import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import org.json.JSONObject

class ExploreRepositoryImpl(
    private val remote: JsonApiClient,
    private val session: SessionPreferences
) : ExploreRepository {
    override fun load() = remote.get("api/explore", token()).toSnapshot()

    override fun loadPlace(placeId: Long): ExplorePlaceDetail =
        remote.get("api/explore/places/$placeId", token()).let {
            ExplorePlaceDetail(it.getLong("placeId"), it.getString("name"), it.getString("placeType"),
                it.optString("imageUrl").takeIf(String::isNotBlank), it.getBoolean("isDiscovered"),
                it.getDouble("distanceKm"), it.getInt("requiredSteps"),
                it.optString("introduction").takeIf(String::isNotBlank),
                it.optString("highlights").takeIf(String::isNotBlank),
                it.optString("regionName").takeIf(String::isNotBlank),
                it.optJSONArray("imageUrls")?.let { array ->
                    (0 until array.length()).map(array::getString)
                }.orEmpty())
        }

    override fun loadRecommendations(): Recommendations =
        remote.get("api/explore/recommendations", token()).let { result ->
            val places = result.getJSONArray("places")
            Recommendations(result.getString("regionName"),
                Coordinates(result.getDouble("latitude"), result.getDouble("longitude")),
                (0 until places.length()).map { index ->
                places.getJSONObject(index).let {
                    RecommendedPlace(it.getInt("rank"), it.getLong("placeId"), it.getString("name"),
                        it.getString("regionName"), it.optString("imageUrl").takeIf(String::isNotBlank),
                        it.getBoolean("isDiscovered"), it.getDouble("distanceKm"), it.getInt("requiredSteps"),
                        it.optJSONArray("matchedPreferences")?.let { array ->
                            (0 until array.length()).map(array::getString)
                        }.orEmpty(),
                        it.optJSONArray("reasons")?.let { array ->
                            (0 until array.length()).map(array::getString)
                        }.orEmpty())
                }
            })
        }

    override fun createJourney(placeId: Long, replaceActive: Boolean): ExploreSnapshot =
        remote.post("api/explore/journeys", JSONObject().put("placeId", placeId)
            .put("replaceActive", replaceActive), token()).toSnapshot()

    override fun syncSteps(journeyId: Long, cumulativeSteps: Long): ExploreSnapshot =
        remote.post("api/explore/sync-steps", JSONObject()
            .put("journeyId", journeyId)
            .put("cumulativeSteps", cumulativeSteps), token()).toSnapshot()

    private fun JSONObject.toSnapshot(): ExploreSnapshot {
        val location = optJSONObject("location")?.let {
            ExploreLocation(it.getString("regionName"), it.optString("placeName").takeIf(String::isNotBlank),
                Coordinates(it.getDouble("latitude"), it.getDouble("longitude")))
        }
        val journey = optJSONObject("journey")?.let {
            ExploreJourney(it.getLong("journeyId"), it.getString("destinationName"),
                Coordinates(it.getDouble("destinationLatitude"), it.getDouble("destinationLongitude")),
                it.getInt("requiredSteps"), it.getInt("recordedSteps"),
                it.getString("status"), it.getLong("startedAtEpochSeconds"),
                if (it.has("startLatitude") && !it.isNull("startLatitude"))
                    Coordinates(it.getDouble("startLatitude"), it.getDouble("startLongitude")) else null,
                it.optLong("destinationPlaceId").takeIf { id -> id > 0 },
                it.optJSONObject("awardedCard")?.let { card ->
                    JourneyCardReward(card.getLong("cardId"), card.getString("name"))
                })
        }
        val places = optJSONArray("places")?.let { array ->
            (0 until array.length()).map { index ->
                array.getJSONObject(index).let { place ->
                    ExploreMapPlace(place.getLong("placeId"), place.getString("name"),
                        Coordinates(place.getDouble("latitude"), place.getDouble("longitude")),
                        place.getBoolean("isDiscovered"))
                }
            }
        }.orEmpty()
        return ExploreSnapshot(location, journey, places)
    }

    private fun token(): String = session.token
        ?: throw ApiFailure("UNAUTHORIZED", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
}
