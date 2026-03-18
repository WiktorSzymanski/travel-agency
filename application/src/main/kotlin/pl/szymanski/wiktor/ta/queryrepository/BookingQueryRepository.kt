package pl.szymanski.wiktor.ta.queryrepository

import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.Page
import pl.szymanski.wiktor.ta.Pageable
import java.util.UUID


interface BookingQueryRepository {

    suspend fun findById(id: BookingId): Pair<Booking, Long>

    suspend fun save(entity: Booking)

    suspend fun findAllByStatus(status: BookingState, pageable: Pageable): Page<Booking>

    suspend fun findAllByUserId(userId: UUID, pageable: Pageable): Page<Booking>
}
