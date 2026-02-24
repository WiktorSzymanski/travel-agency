package pl.szymanski.wiktor.ta.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId

interface BookingRepository : Repository<Booking> {
    suspend fun findById(id: BookingId): Pair<Booking, Long>
}
