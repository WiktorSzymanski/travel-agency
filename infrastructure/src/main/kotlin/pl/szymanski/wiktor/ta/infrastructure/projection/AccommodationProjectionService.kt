package pl.szymanski.wiktor.ta.infrastructure.projection

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.event.AccommodationBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.AccommodationBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.queryRepository.AccommodationQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.AccommodationUpdate
import pl.szymanski.wiktor.ta.queryRepository.AccommodationUpdateRevision
import pl.szymanski.wiktor.ta.queryRepository.AccommodationUpdateStatus

class AccommodationProjectionService(
    private val accommodationQueryRepository: AccommodationQueryRepository
) {
    fun startProjection() {
        ProjectionEventRepository().subscribe("accommodation") {
            event, i -> updateProjection(event as AccommodationEvent, i)
        }
    }

    private suspend fun updateProjection(event: AccommodationEvent, revision: Int) {
        when (event) {
            is AccommodationCreatedEvent -> {
                accommodationQueryRepository.save(
                    Accommodation(
                        _id = event.accommodationId,
                        name = event.name,
                        location = event.location,
                        rent = event.rent,
                        status = AccommodationStatusEnum.AVAILABLE,
                        lastRevision = revision
                    )
                ) ?: throw IllegalStateException("Unable to save accommodation ${event.accommodationId}")
            }
            is AccommodationBookedEvent -> {
                accommodationQueryRepository.update(
                    AccommodationUpdate(
                        _id = event.accommodationId,
                        status = AccommodationStatusEnum.BOOKED,
                        bookingId = event.bookingId,
                        revision = revision
                    )
                )
            }
            is AccommodationBookingCanceledEvent -> {
                accommodationQueryRepository.update(
                    AccommodationUpdate(
                        _id = event.accommodationId,
                        status = AccommodationStatusEnum.AVAILABLE,
                        bookingId = null,
                        revision = revision
                    )
                )
            }
            is AccommodationExpiredEvent -> {
                accommodationQueryRepository.update(
                    AccommodationUpdateStatus(
                        _id = event.accommodationId,
                        status = AccommodationStatusEnum.EXPIRED,
                        revision = revision
                    )
                )
            }
            is AccommodationBookedCompensatedEvent -> {
                accommodationQueryRepository.update(
                    AccommodationUpdate(
                        _id = event.accommodationId,
                        status = AccommodationStatusEnum.AVAILABLE,
                        bookingId = null,
                        revision = revision
                    )
                )
            }
            is AccommodationBookingCanceledCompensatedEvent -> {
                accommodationQueryRepository.update(
                    AccommodationUpdate(
                        _id = event.accommodationId,
                        status = AccommodationStatusEnum.BOOKED,
                        bookingId = event.bookingId,
                        revision = revision
                    )
                )
            }
            else -> {
                accommodationQueryRepository.update(
                    AccommodationUpdateRevision(
                        _id = event.accommodationId,
                        revision = revision,
                        event = event
                    )
                )
            }
        }
    }
}