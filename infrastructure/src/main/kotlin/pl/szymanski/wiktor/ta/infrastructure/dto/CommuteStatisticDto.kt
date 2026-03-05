package pl.szymanski.wiktor.ta.infrastructure.dto

data class CommuteStatisticDto(
    val time: String,
    val totalCommuteCount: Int,
    val totalBookingsCount: Int,
    val arrivalLocations: List<ArrivalLocationDto>,
)

data class ArrivalLocationDto(
    val location: String,
    val commutesNumber: Int,
    val passengersNumber: Int,
)
