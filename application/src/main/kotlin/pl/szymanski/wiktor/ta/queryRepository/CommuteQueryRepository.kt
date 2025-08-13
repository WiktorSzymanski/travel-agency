package pl.szymanski.wiktor.ta.queryRepository

import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.dto.CommuteStatisticDto
import java.time.LocalDateTime
import java.util.UUID

interface CommuteQueryRepository {
    suspend fun save(entity: Commute): Commute?

    suspend fun findById(commuteId: UUID): Commute

    suspend fun findAllByStatus(status: CommuteStatusEnum): List<Commute>

    suspend fun update(entity: CommuteUpdate)

    suspend fun update(entity: CommuteUpdateStatus)

    suspend fun update(entity: CommuteCancelUpdate)

    suspend fun findStatistics(
        page: Int = 1,
        size: Int = 20,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<CommuteStatisticDto>
}

data class CommuteUpdate(
    val _id: UUID,
    val bookingId: UUID? = null,
    val seat: Seat? = null,
)

data class CommuteCancelUpdate(
    val _id: UUID,
    val status: CommuteStatusEnum? = null,
    val bookingId: UUID? = null
)

data class CommuteUpdateStatus(
    val _id: UUID,
    val status: CommuteStatusEnum? = null,
)
