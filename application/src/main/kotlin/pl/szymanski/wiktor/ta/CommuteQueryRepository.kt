package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.dto.CommuteStatisticDto
import java.time.LocalDateTime

interface CommuteQueryRepository {
    suspend fun findStatistics(
        page: Int = 1,
        size: Int = 20,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<CommuteStatisticDto>
}