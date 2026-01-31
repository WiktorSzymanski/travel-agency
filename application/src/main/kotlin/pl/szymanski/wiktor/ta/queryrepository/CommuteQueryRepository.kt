package pl.szymanski.wiktor.ta.queryrepository

import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.dto.CommuteStatisticDto
import pl.szymanski.wiktor.ta.offermaker.LocalDateTimeRange
import java.time.LocalDateTime
import java.util.UUID

interface CommuteQueryRepository {
    suspend fun save(entity: Commute): Commute?

    suspend fun update(projectionUpdate: ProjectionUpdate)

    suspend fun findById(commuteId: UUID): Commute

    suspend fun findAllByStatus(status: CommuteStatusEnum): List<Commute>

    suspend fun findByLocationAndArrivalDate(location: LocationEnum, dateRange: LocalDateTimeRange): List<Commute>

    suspend fun findStatistics(
        page: Int = 1,
        size: Int = 20,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<CommuteStatisticDto>
}
