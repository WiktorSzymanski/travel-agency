package pl.szymanski.wiktor.ta.queryrepository

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import java.util.UUID

interface AttractionQueryRepository {
    suspend fun save(entity: Attraction): Attraction?

    suspend fun update(projectionUpdate: ProjectionUpdate)

    suspend fun findById(attractionId: UUID): Attraction

    suspend fun findAllByStatus(status: AttractionStatusEnum): List<Attraction>
}
