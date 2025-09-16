package pl.szymanski.wiktor.ta.infrastructure.projection

import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeUnavailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferRebookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import pl.szymanski.wiktor.ta.event.TravelOfferBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.TravelOfferBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.queryRepository.TravelOfferQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.TravelOfferUpdate
import pl.szymanski.wiktor.ta.queryRepository.TravelOfferUpdateRevision
import pl.szymanski.wiktor.ta.queryRepository.TravelOfferUpdateStatus

class TravelOfferProjectionService(
    private val travelOfferQueryRepository: TravelOfferQueryRepository
) {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    }

//    fun startProjection() {
//        ProjectionEventRepository().subscribe("travelOffer") {
//            event, i -> updateProjection(event as TravelOfferEvent, i)
//        }
//    }

    suspend fun updateProjection(event: TravelOfferEvent, revision: Int) {
        when (event) {
            is TravelOfferCreatedEvent -> {
                travelOfferQueryRepository.save(
                    TravelOffer(
                        id = event.travelOfferId,
                        name = event.name,
                        commuteId = event.commuteId,
                        accommodationId = event.accommodationId,
                        attractionId = event.attractionId,
                        status = TravelOfferStatusEnum.AVAILABLE,
                        lastRevision = revision
                    )
                )
                ?: log.error("Unable to save travel offer ${event.travelOfferId}")
            }
            is TravelOfferReservedEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdate(
                        id = event.travelOfferId,
                        status = TravelOfferStatusEnum.RESERVED,
                        bookingId = event.bookingId,
                        lastRevision = revision
                    ), event
                )
            }
            is TravelOfferReservationCanceledEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdate(
                        id = event.travelOfferId,
                        status = TravelOfferStatusEnum.AVAILABLE,
                        bookingId = null,
                        lastRevision = revision
                    ), event
                )
            }
            is TravelOfferBookedEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdate(
                        id = event.travelOfferId,
                        status = TravelOfferStatusEnum.BOOKED,
                        bookingId = event.bookingId,
                        lastRevision = revision
                    ), event
                )
            }
            is TravelOfferReleaseEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdateStatus(
                        id = event.travelOfferId,
                        status = TravelOfferStatusEnum.RELEASING,
                        lastRevision = revision
                    ), event
                )
            }
            is TravelOfferBookingCanceledEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdate(
                        id = event.travelOfferId,
                        status = TravelOfferStatusEnum.AVAILABLE,
                        bookingId = null,
                        lastRevision = revision
                    ), event
                )
            }
            is TravelOfferRebookedEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdate(
                        id = event.travelOfferId,
                        status = TravelOfferStatusEnum.BOOKED,
                        bookingId = event.bookingId,
                        lastRevision = revision
                    ), event
                )
            }
            is TravelOfferExpiredEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdateStatus(
                        id = event.travelOfferId,
                        status = TravelOfferStatusEnum.EXPIRED,
                        lastRevision = revision
                    ), event
                )
            }
            is TravelOfferMadeUnavailableEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdateStatus(
                        id = event.travelOfferId,
                        status = TravelOfferStatusEnum.UNAVAILABLE,
                        lastRevision = revision
                    ), event
                )
            }
            is TravelOfferMadeAvailableEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdateStatus(
                        id = event.travelOfferId,
                        status = TravelOfferStatusEnum.AVAILABLE,
                        lastRevision = revision
                    ), event
                )
            }
            is TravelOfferBookedCompensatedEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdate(
                        id = event.travelOfferId,
                        status = TravelOfferStatusEnum.AVAILABLE,
                        bookingId = null,
                        lastRevision = revision
                    ), event
                )
            }
            is TravelOfferBookingCanceledCompensatedEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdate(
                        id = event.travelOfferId,
                        status = TravelOfferStatusEnum.BOOKED,
                        bookingId = event.bookingId,
                        lastRevision = revision
                    ), event
                )
            }
            else -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdateRevision(
                        id = event.travelOfferId,
                        lastRevision = revision,
                        event = event
                    ), event)
            }
        }
    }
}