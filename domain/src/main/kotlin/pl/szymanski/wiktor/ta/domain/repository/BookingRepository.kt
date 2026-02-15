package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.event.BookingEvent

interface BookingRepository {
    suspend fun findById(id: BookingId): Pair<Booking, Long>

    suspend fun create(
        entity: Booking,
        event: BookingEvent,
    )

    suspend fun save(
        entity: Booking,
        event: BookingEvent,
    )
}
