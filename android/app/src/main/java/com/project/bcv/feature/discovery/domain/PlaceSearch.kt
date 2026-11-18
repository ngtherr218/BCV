package com.project.bcv.feature.discovery.domain

import java.text.Normalizer
import java.util.Locale

object PlaceSearch {
    private val marks = Regex("\\p{M}+")
    fun normalize(value: String) = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(marks, "").replace('đ', 'd')
    fun filter(places: List<ExploreMapPlace>, input: String): List<ExploreMapPlace> {
        val query = normalize(input.trim())
        if (query.isEmpty()) return places
        return places.filter { normalize(it.placeType).contains(query) || normalize(it.name).contains(query) }
    }

    /** Great-circle distance in km. */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}
