package pl.szymanski.wiktor.ta.query

import pl.szymanski.wiktor.ta.CommuteQueryRepository
import pl.szymanski.wiktor.ta.dto.CommuteStatisticDto
import java.time.LocalDateTime

class CommuteStatisticsQuery(
    private val commuteQueryRepository: CommuteQueryRepository
) {
    suspend fun getCommuteStats(page: Int, size: Int, start: LocalDateTime, end: LocalDateTime): List<CommuteStatisticDto> =
        commuteQueryRepository.findStatistics(page, size, start, end)
}