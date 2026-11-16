package com.project.bcv.feature.account.interests.domain

data class PlaceType(val id: Long, val name: String)
data class InterestSnapshot(val placeTypes: List<PlaceType>, val selectedIds: Set<Long>, val requiresReselection: Boolean = false)

interface InterestRepository {
    fun load(): InterestSnapshot
    fun save(ids: Set<Long>)
}
