package pl.szymanski.wiktor.ta.infrastructure.document

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.util.*

@Document(collection = "accommodations")
data class AccommodationDocument(
    @Id
    @Field("_id", targetType = FieldType.IMPLICIT)
    val id: UUID,
    val name: String,
    val location: String,
    val rent: RentDocument,
    val booking: String? = null,
    val status: String,
    @Version
    val version: Long = 0L
) {
    companion object {
        fun fromDomain(accommodation: Accommodation, version: Long = 0L) =
            AccommodationDocument(
                id = accommodation.id.value,
                name = accommodation.name,
                location = accommodation.location.name,
                rent = RentDocument.fromDomain(accommodation.rent),
                booking = accommodation.bookingId.value?.toString(),
                status = accommodation.status.name,
                version = version
            )
    }

    fun toDomain(): Accommodation =
        Accommodation(
            id = AccommodationId.from(id),
            name = name,
            location = LocationEnum.valueOf(location),
            rent = rent.toDomain(),
            bookingId = booking?.let { BookingId.from(UUID.fromString(it)) } ?: BookingId.Empty,
            status = AccommodationStatusEnum.valueOf(status)
        )
}

