package pl.szymanski.wiktor.ta.queryrepository

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.LocalDateTimeRange
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.Page
import pl.szymanski.wiktor.ta.Pageable
import java.util.UUID

interface AttractionQueryRepository {
    suspend fun save(entity: Attraction, metadata: Metadata)

    suspend fun update(projectionUpdate: ProjectionUpdate)

    suspend fun findById(id: AttractionId): Pair<Attraction, Long>

    suspend fun findAllByStatus(status: AttractionStatusEnum,  pageable: Pageable): Page<Attraction>
}
