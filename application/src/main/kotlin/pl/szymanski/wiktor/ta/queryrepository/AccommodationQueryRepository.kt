package pl.szymanski.wiktor.ta.queryrepository

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.LocalDateTimeRange
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.Page
import pl.szymanski.wiktor.ta.Pageable
import java.time.LocalDateTime

interface AccommodationQueryRepository {
    suspend fun save(entity: Accommodation, metadata: Metadata)

    suspend fun update(projectionUpdate: ProjectionUpdate)

    suspend fun findById(id: AccommodationId): Pair<Accommodation, Long>

    suspend fun findAllByStatus(status: AccommodationStatusEnum, pageable: Pageable): Page<Accommodation>
}
