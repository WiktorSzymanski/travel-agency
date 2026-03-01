package pl.szymanski.wiktor.ta.infrastructure.document

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.time.LocalDateTime
import java.util.*

@Document(collection = "attractions")
data class AttractionDocument (
    @Id
    @Field("_id", targetType = FieldType.IMPLICIT)
    val id: UUID,
    val name: String,
    val location: String,
    val date: String,
    val capacity: Int,
    val bookings: List<String>,
    val status: String,
    @Version
    val version: Long = 0L
) {
    companion object {
        fun fromDomain(attraction: Attraction, version: Long = 0L) =
            when (attraction.id) {
                is AttractionId.Present -> AttractionDocument(
                    id = attraction.id.value!!,
                    name = attraction.name,
                    location = attraction.location.name,
                    date = attraction.date.toString(),
                    capacity = attraction.capacity,
                    bookings = attraction.bookings.map { it.value.toString() },
                    status = attraction.status.name,
                    version = version
                )
                is AttractionId.Empty -> throw IllegalArgumentException("Attraction ID cannot be empty when converting to document")
            }
    }

    fun toDomain(): Attraction =
        Attraction(
            id = AttractionId.Present(id),
            name = name,
            location = LocationEnum.valueOf(location),
            date = LocalDateTime.parse(date),
            capacity = capacity,
            bookings = bookings.map { BookingId.from(UUID.fromString(it)) }.toMutableList(),
            status = AttractionStatusEnum.valueOf(status)
        )
}

