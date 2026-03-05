package pl.szymanski.wiktor.ta.generator

import pl.szymanski.wiktor.ta.domain.PickedSeat
import pl.szymanski.wiktor.ta.domain.Seat

data class CommuteTemplate(
    val name: String,
    val departureLocation: String,
    val arrivalLocation: String,
    val seats: List<PickedSeat>,
)

data class AttractionTemplate(
    val name: String,
    val location: String,
    val capacity: Int,
)

data class AccommodationTemplate(
    val name: String,
    val location: String,
)
