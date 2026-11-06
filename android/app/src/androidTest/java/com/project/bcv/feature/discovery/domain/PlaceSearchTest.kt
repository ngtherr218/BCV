package com.project.bcv.feature.discovery.domain

import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import org.junit.Assert.*
import org.junit.Test

class PlaceSearchTest {
    private val places = listOf(
        ExploreMapPlace(1, "Bảo tàng Hà Nội", Coordinates(21.0,105.0), false, "Bảo tàng"),
        ExploreMapPlace(2, "Đảo Cô Tô", Coordinates(20.0,106.0), true, "Biển và đảo")
    )
    @Test fun filtersTypeWithoutAccents() { assertEquals(listOf(1L), PlaceSearch.filter(places, "bao tang").map { it.id }) }
    @Test fun matchesVietnameseDAndCase() { assertEquals(listOf(2L), PlaceSearch.filter(places, "DAO").map { it.id }) }
    @Test fun matchesName() { assertEquals(listOf(2L), PlaceSearch.filter(places, "co to").map { it.id }) }
    @Test fun clearRestoresAllPlaces() { assertEquals(places, PlaceSearch.filter(places, "  ")) }
    @Test fun noMatchReturnsEmpty() { assertTrue(PlaceSearch.filter(places, "khong ton tai").isEmpty()) }
}
