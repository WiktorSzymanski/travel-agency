package pl.szymanski.wiktor.ta.generator

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.Seat

@Serializable
data class CommuteTemplate(
    val name: String,
    val departureLocation: String,
    val arrivalLocation: String,
    val seats: List<Seat.Picked>,
)

@Serializable
data class AttractionTemplate(
    val name: String,
    val location: String,
    val capacity: Int,
)

@Serializable
data class AccommodationTemplate(
    val name: String,
    val location: String,
)
