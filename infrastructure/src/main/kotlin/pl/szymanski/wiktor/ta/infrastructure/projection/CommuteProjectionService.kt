package pl.szymanski.wiktor.ta.infrastructure.projection

import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.event.CommuteBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.CommuteBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.queryRepository.CommuteCancelUpdate
import pl.szymanski.wiktor.ta.queryRepository.CommuteQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.CommuteUpdate
import pl.szymanski.wiktor.ta.queryRepository.CommuteUpdateRevision
import pl.szymanski.wiktor.ta.queryRepository.CommuteUpdateStatus

class CommuteProjectionService(
    private val commuteQueryRepository: CommuteQueryRepository
) {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    }
    fun startProjection() {
        ProjectionEventRepository().subscribe("commute") {
            event, i -> updateProjection(event as CommuteEvent, i)
        }
    }

    private suspend fun updateProjection(event: CommuteEvent, revision: Int) {
        when (event) {
            is CommuteCreatedEvent -> {
                commuteQueryRepository.save(
                    Commute(
                        _id = event.commuteId,
                        name = event.name,
                        departure = event.departure,
                        arrival = event.arrival,
                        seats = event.seats,
                        status = CommuteStatusEnum.SCHEDULED,
                        lastRevision = revision
                    )
                ) ?: throw IllegalStateException("Unable to save commute ${event.commuteId}")
            }
            is CommuteBookedEvent -> {
                commuteQueryRepository.update(
                    CommuteUpdate(
                        _id = event.commuteId,
                        bookingId = event.bookingId,
                        revision = revision
                    )
                )
            }
            is CommuteBookingCanceledEvent -> {
                commuteQueryRepository.update(
                    CommuteCancelUpdate(
                        _id = event.commuteId,
                        bookingId = event.bookingId,
                        revision = revision
                    )
                )
            }
            is CommuteExpiredEvent -> {
                commuteQueryRepository.update(
                    CommuteUpdateStatus(
                        _id = event.commuteId,
                        status = CommuteStatusEnum.EXPIRED,
                        revision = revision
                    )
                )
            }
            is CommuteFullEvent -> {
                commuteQueryRepository.update(
                    CommuteUpdateStatus(
                        _id = event.commuteId,
                        status = CommuteStatusEnum.FULL,
                        revision = revision
                    )
                )
            }
            is CommuteAvailableEvent -> {
                commuteQueryRepository.update(
                    CommuteUpdateStatus(
                        _id = event.commuteId,
                        status = CommuteStatusEnum.SCHEDULED,
                        revision = revision
                    )
                )
            }
            is CommuteBookedCompensatedEvent -> {
                commuteQueryRepository.update(
                    CommuteCancelUpdate(
                        _id = event.commuteId,
                        bookingId = event.bookingId,
                        revision = revision
                    )
                )
            }
            is CommuteBookingCanceledCompensatedEvent -> {
                commuteQueryRepository.update(
                    CommuteUpdate(
                        _id = event.commuteId,
                        bookingId = event.bookingId,
                        revision = revision
                    )
                )
            }
            else -> {
                commuteQueryRepository.update(
                    CommuteUpdateRevision(
                        _id = event.commuteId,
                        revision = revision
                    )
                )
            }
        }
    }
}