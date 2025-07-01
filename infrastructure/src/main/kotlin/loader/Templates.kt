package loader

import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.Seat

data class CommuteTemplate(
    val name: String,
    val departureLocation: String,
    val arrivalLocation: String,
    val seats: List<Seat>,
)

data class AttractionTemplate(
    val name: String,
    val location: String,
    val capacity: Int,
)

data class AccommodationTemplate(
    val name: String,
    val location: String,
    val rent: Rent,
)
