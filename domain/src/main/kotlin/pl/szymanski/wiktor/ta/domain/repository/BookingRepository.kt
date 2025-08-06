package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import java.util.UUID

interface BookingRepository {
    suspend fun findById(bookingId: UUID): Booking
    
    suspend fun save(booking: Booking): Booking?
    
    suspend fun update(booking: Booking)
    
    suspend fun findByUserId(userId: UUID): List<Booking>
    
    suspend fun findByState(state: BookingState): List<Booking>
}