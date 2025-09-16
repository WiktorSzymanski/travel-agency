package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.event.Event
import java.util.UUID

interface AccommodationRepository : Repository<Accommodation> {
    suspend fun findById(accommodationId: UUID): Accommodation

//    override suspend fun save(event: Event)
}
