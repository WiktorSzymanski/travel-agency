package pl.szymanski.wiktor.ta.query

import pl.szymanski.wiktor.ta.Page
import pl.szymanski.wiktor.ta.Pageable
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.queryrepository.CommuteQueryRepository

class CommuteQuery (
    private val commuteRepository: CommuteQueryRepository,
) {
    suspend fun getScheduledCommutes(pageable: Pageable): Page<Commute> =
        commuteRepository.findAllByStatus(CommuteStatusEnum.SCHEDULED, pageable)
}