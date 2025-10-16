package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import java.util.UUID

interface BookingRepository {
    suspend fun findById(id: UUID): Booking

    suspend fun create(
        entity: Booking,
        event: BookingEvent,
    )

    suspend fun save(
        entity: Booking,
        event: BookingEvent,
    )
}
