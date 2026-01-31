package pl.szymanski.wiktor.ta.queryrepository

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.offermaker.LocalDateTimeRange
import java.util.UUID

interface AttractionQueryRepository {
    suspend fun save(entity: Attraction): Attraction?

    suspend fun update(projectionUpdate: ProjectionUpdate)

    suspend fun findById(attractionId: UUID): Attraction

    suspend fun findAllByStatus(status: AttractionStatusEnum): List<Attraction>

    suspend fun findByLocationAndDate(location: LocationEnum, dateRange: LocalDateTimeRange): List<Attraction>
}
