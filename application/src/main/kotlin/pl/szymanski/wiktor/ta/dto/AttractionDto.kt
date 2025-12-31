package pl.szymanski.wiktor.ta.dto

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction

@Serializable
sealed interface AttractionDto {
    data object Empty : AttractionDto
    data class Present(
        val id: String,
        val name: String,
        val location: String,
        val date: String,
        val availableSlots: Int,
    ) : AttractionDto

    companion object {
        fun fromDomain(attraction: Attraction?) =
            attraction?.let {
                AttractionDto.Present(
                    id = attraction.id.toString(),
                    name = attraction.name,
                    location = attraction.location.name,
                    date = attraction.date.toString(),
                    availableSlots = attraction.capacity - attraction.bookings.size,
                )
            } ?: AttractionDto.Empty
    }
}
