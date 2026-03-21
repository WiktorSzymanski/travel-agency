package pl.szymanski.wiktor.ta.service

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.queryrepository.CommuteQueryRepository

class CommuteService(
    private val commuteRepository: CommuteQueryRepository
) {
    suspend fun checkAvailability(commuteId: CommuteId, seat: Seat) {
        val (commute, _) = commuteRepository.findById(commuteId)
        commute.checkAvailability(seat)
    }
}
