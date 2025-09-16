package pl.szymanski.wiktor.ta.infrastructure.projection

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import pl.szymanski.wiktor.ta.event.AttractionBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.AttractionBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.queryRepository.AttractionCancelUpdate
import pl.szymanski.wiktor.ta.queryRepository.AttractionQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.AttractionUpdate
import pl.szymanski.wiktor.ta.queryRepository.AttractionUpdateRevision
import pl.szymanski.wiktor.ta.queryRepository.AttractionUpdateStatus

class AttractionProjectionService(
    private val attractionQueryRepository: AttractionQueryRepository
) {
//    fun startProjection() {
//        ProjectionEventRepository().subscribe("attraction")
//            {
//                    event, i -> updateProjection(event as AttractionEvent, i)
//            }
//    }

    suspend fun updateProjection(event: AttractionEvent, revision: Int) {
        when (event) {
            is AttractionCreatedEvent -> {
                attractionQueryRepository.save(
                    Attraction(
                        id = event.attractionId,
                        name = event.name,
                        location = event.location,
                        date = event.date,
                        capacity = event.capacity,
                        status = AttractionStatusEnum.SCHEDULED,
                        lastRevision = revision
                    )
                ) ?: throw IllegalStateException("Unable to save attraction ${event.attractionId}")
            }
            is AttractionBookedEvent -> {
                attractionQueryRepository.update(
                    AttractionUpdate(
                        id = event.attractionId,
                        bookingId = event.bookingId,
                        revision = revision
                    )
                )
            }
            is AttractionBookingCanceledEvent -> {
                attractionQueryRepository.update(
                    AttractionCancelUpdate(
                        id = event.attractionId,
                        bookingId = event.bookingId,
                        revision = revision
                    )
                )
            }
            is AttractionExpiredEvent -> {
                attractionQueryRepository.update(
                    AttractionUpdateStatus(
                        id = event.attractionId,
                        status = AttractionStatusEnum.EXPIRED,
                        revision = revision
                    )
                )
            }
            is AttractionFullEvent -> {
                attractionQueryRepository.update(
                    AttractionUpdateStatus(
                        id = event.attractionId,
                        status = AttractionStatusEnum.FULL,
                        revision = revision
                    )
                )
            }
            is AttractionAvailableEvent -> {
                attractionQueryRepository.update(
                    AttractionUpdateStatus(
                        id = event.attractionId,
                        status = AttractionStatusEnum.SCHEDULED,
                        revision = revision
                    )
                )
            }
            is AttractionBookedCompensatedEvent -> {
                attractionQueryRepository.update(
                    AttractionCancelUpdate(
                        id = event.attractionId,
                        bookingId = event.bookingId,
                        revision = revision
                    )
                )
            }
            is AttractionBookingCanceledCompensatedEvent -> {
                attractionQueryRepository.update(
                    AttractionUpdate(
                        id = event.attractionId,
                        bookingId = event.bookingId,
                        revision = revision
                    )
                )
            }
            is AttractionDateMetEvent -> {}
            else -> {
                attractionQueryRepository.update(
                    AttractionUpdateRevision(
                        id = event.attractionId,
                        revision = revision,
                        event = event
                    )
                )
            }
        }
    }
}