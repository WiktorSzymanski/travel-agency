package pl.szymanski.wiktor.ta.dto

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation

@Serializable
data class AccommodationDto(
    val id: String,
    val name: String,
    val location: String,
    val rent: RentDto,
    val booking: BookingDto? = null,
    val status: String,
) {
    companion object {
        fun fromDomain(accommodation: Accommodation) =
            AccommodationDto(
                id = accommodation._id.toString(),
                name = accommodation.name,
                location = accommodation.location.name,
                rent = RentDto.fromDomain(accommodation.rent),
                booking = accommodation.booking?.let { BookingDto.fromDomain(it) },
                status = accommodation.status.name,
            )
    }
}
