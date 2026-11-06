package com.project.bcv.feature.discovery.domain

import java.text.Normalizer
import java.util.Locale

object PlaceSearch {
    private val marks = Regex("\\p{M}+")
    private fun normalize(value: String) = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(marks, "").replace('đ', 'd')
    fun filter(places: List<ExploreMapPlace>, input: String): List<ExploreMapPlace> {
        val query = normalize(input.trim())
        if (query.isEmpty()) return places
        return places.filter { normalize(it.placeType).contains(query) || normalize(it.name).contains(query) }
    }
}
