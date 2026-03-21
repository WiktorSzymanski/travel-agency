package pl.szymanski.wiktor.ta.query

import pl.szymanski.wiktor.ta.Pageable
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.queryrepository.AccommodationQueryRepository

class AccommodationQuery (
    private val accommodationRepository: AccommodationQueryRepository,
) {
    suspend fun getAvailableAccommodations(pageable: Pageable) =
        accommodationRepository.findAllByStatus(AccommodationStatusEnum.AVAILABLE, pageable)
}