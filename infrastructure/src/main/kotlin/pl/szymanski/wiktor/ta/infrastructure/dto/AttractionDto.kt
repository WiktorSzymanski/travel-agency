package pl.szymanski.wiktor.ta.infrastructure.dto

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.time.LocalDateTime
import java.util.UUID

@Serializable
sealed interface AttractionDto {
    data object Empty : AttractionDto {
        fun toDomain(): Attraction? = null
    }

    @Serializable
    data class Present(
        val id: String,
        val name: String,
        val location: String,
        val date: String,
        val capacity: Int,
        val bookings: List<String>,
        val status: String,
        val version: Long = 0L
    ) : AttractionDto {
        fun toDomain(): Attraction =
            Attraction(
                id = AttractionId.Present(UUID.fromString(id)),
                name = name,
                location = LocationEnum.valueOf(location),
                date = LocalDateTime.parse(date),
                capacity = capacity,
                bookings = bookings.map { BookingId.from(UUID.fromString(it)) }.toMutableList(),
                status = AttractionStatusEnum.valueOf(status)
            )
    }

    companion object {
        fun fromDomain(attraction: Attraction?, version: Long = 0L) =
            attraction?.let {
                Present(
                    id = attraction.id.value.toString(),
                    name = attraction.name,
                    location = attraction.location.name,
                    date = attraction.date.toString(),
                    capacity = attraction.capacity,
                    bookings = attraction.bookings.map { it.value.toString() },
                    status = attraction.status.name,
                    version = version
                )
            } ?: Empty
    }
}


