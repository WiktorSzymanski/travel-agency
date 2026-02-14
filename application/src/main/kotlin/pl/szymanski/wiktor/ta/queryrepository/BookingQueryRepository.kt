package pl.szymanski.wiktor.ta.queryrepository

import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId


interface BookingQueryRepository {

    suspend fun findById(id: BookingId): Booking

    suspend fun save(entity: Booking)
}
