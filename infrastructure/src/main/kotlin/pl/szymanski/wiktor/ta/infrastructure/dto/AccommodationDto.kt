package pl.szymanski.wiktor.ta.infrastructure.dto

import kotlinx.serialization.Serializable
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.util.UUID


@Serializable
@Document(collection = "accommodations")
data class AccommodationDto(
    @Id val mongoId: String? = null,
    @Field("id") val id: String,
    val name: String,
    val location: String,
    val rent: RentDto,
    val booking: String? = null,
    val status: String,
    val version: Long = 0L
) {
    companion object {
        fun fromDomain(accommodation: Accommodation, version: Long = 0L) =
            AccommodationDto(
                id = accommodation.id.value.toString(),
                name = accommodation.name,
                location = accommodation.location.name,
                rent = RentDto.fromDomain(accommodation.rent),
                booking = accommodation.bookingId.value?.toString(),
                status = accommodation.status.name,
                version = version
            )
    }

    fun toDomain(): Accommodation =
        Accommodation(
            id = AccommodationId.from(UUID.fromString(id)),
            name = name,
            location = LocationEnum.valueOf(location),
            rent = rent.toDomain(),
            bookingId = booking?.let { BookingId.from(UUID.fromString(it)) } ?: BookingId.Empty,
            status = AccommodationStatusEnum.valueOf(status)
        )
}
