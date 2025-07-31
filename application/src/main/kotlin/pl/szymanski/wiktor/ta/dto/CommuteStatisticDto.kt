package pl.szymanski.wiktor.ta.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommuteStatisticDto(
    val time: String,
    val totalCommuteCount: Int,
    val totalBookingsCount: Int,
    val arrivalLocations: List<ArrivalLocationDto>
)

@Serializable
data class ArrivalLocationDto(
    val location: String,
    val commutesNumber: Int,
    val passengersNumber: Int,
)