package pl.szymanski.wiktor.ta.query

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.queryrepository.AttractionQueryRepository

class AttractionQuery (
    private val attractionRepository: AttractionQueryRepository,
) {
    suspend fun getScheduledAttractions() {
        attractionRepository.findAllByStatus(AttractionStatusEnum.SCHEDULED)
    }
}