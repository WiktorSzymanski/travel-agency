package pl.szymanski.wiktor.ta.queryRepository

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.event.Event
import java.util.UUID

interface AttractionQueryRepository {
    suspend fun save(entity: Attraction): Attraction?

    suspend fun update(projectionUpdate: ProjectionUpdate)

    suspend fun findById(attractionId: UUID): Attraction

    suspend fun findAllByStatus(status: AttractionStatusEnum): List<Attraction>

}