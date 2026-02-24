package pl.szymanski.wiktor.ta.queryrepository

import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.LocalDateTimeRange

interface CommuteQueryRepository {
    suspend fun save(entity: Commute)

    suspend fun update(projectionUpdate: ProjectionUpdate)

    suspend fun findById(id: CommuteId): Pair<Commute, Long>

    suspend fun findAllByStatus(status: CommuteStatusEnum): List<Commute>

    suspend fun findByLocationAndArrivalDate(location: LocationEnum, dateRange: LocalDateTimeRange): List<Commute>
}
