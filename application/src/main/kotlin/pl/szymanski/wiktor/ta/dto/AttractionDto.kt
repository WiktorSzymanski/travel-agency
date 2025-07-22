package pl.szymanski.wiktor.ta.dto

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction

@Serializable
data class AttractionDto(
    val id: String,
    val name: String,
    val location: String,
    val date: String,
    val availableSlots: Int,
) {
    companion object {
        fun fromDomain(attraction: Attraction) =
            AttractionDto(
                id = attraction._id.toString(),
                name = attraction.name,
                location = attraction.location.name,
                date = attraction.date.toString(),
                availableSlots = attraction.capacity - attraction.bookings.size,
            )
    }
}
