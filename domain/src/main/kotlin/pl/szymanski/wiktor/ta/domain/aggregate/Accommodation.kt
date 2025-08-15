package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookFailedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCancelFailedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpireFailedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import java.time.LocalDateTime
import java.util.UUID

data class Accommodation(
    val _id: UUID = UUID.randomUUID(),
    val name: String,
    val location: LocationEnum,
    val rent: Rent,
    var bookingId: UUID? = null,
    var status: AccommodationStatusEnum = AccommodationStatusEnum.AVAILABLE,
    val lastRevision: Int = -1,
) {
    fun apply(event: AccommodationEvent, revision: Int): Accommodation {
        return when (event) {
            is AccommodationCreatedEvent -> this.copy(
                _id = event.accommodationId,
                name = event.name,
                location = event.location,
                rent = event.rent,
                status = AccommodationStatusEnum.AVAILABLE,
                lastRevision = revision
            )
            is AccommodationBookedEvent -> this.copy(
                status = AccommodationStatusEnum.BOOKED,
                bookingId = event.bookingId,
                lastRevision = revision
            )
            is AccommodationBookingCanceledEvent -> this.copy(
                status = AccommodationStatusEnum.AVAILABLE,
                bookingId = null,
                lastRevision = revision
            )
            is AccommodationExpiredEvent -> this.copy(
                status = AccommodationStatusEnum.EXPIRED,
                lastRevision = revision
            )
            else -> this.copy(lastRevision = revision)
        }
    }
    companion object {
        fun create(
            name: String,
            location: LocationEnum,
            rent: Rent,
        ): Pair<Accommodation, AccommodationCreatedEvent> {
            val accommodation =
                Accommodation(
                    name = name,
                    location = location,
                    rent = rent,
                )

            val event =
                AccommodationCreatedEvent(
                    accommodationId = accommodation._id,
                    name = name,
                    location = location,
                    rent = rent,
                )

            return accommodation to event
        }
        
        fun fromEvents(events: List<Pair<AccommodationEvent, Int>>): Accommodation? {
            if (events.isEmpty()) return null

            val (createdEvent, _) = events.first()

            require(createdEvent is AccommodationCreatedEvent) { "First event must be AccommodationCreatedEvent" }

            var accommodation = Accommodation(
                _id = createdEvent.accommodationId,
                name = createdEvent.name,
                location = createdEvent.location,
                rent = createdEvent.rent,
            )

            for ((event, revision) in events) {
                accommodation = accommodation.apply(event, revision)
            }
            
            return accommodation
        }
    }

    fun expire(): AccommodationEvent {
        if (status != AccommodationStatusEnum.AVAILABLE) {
            return AccommodationExpireFailedEvent(
                accommodationId = _id,
                message = "Accommodation $_id cannot expire in status $status"
            )
        }

        if (LocalDateTime.now().isBefore(rent.from)) {
            return AccommodationExpireFailedEvent(
                accommodationId = _id,
                message = "Accommodation $_id cannot be expired before its rent start"
            )
        }

        this.status = AccommodationStatusEnum.EXPIRED

        return AccommodationExpiredEvent(
            accommodationId = _id,
        )
    }

    fun book(bookingId: UUID): AccommodationEvent {
        statusCheck()
        if (this.status != AccommodationStatusEnum.AVAILABLE) {
            return AccommodationBookFailedEvent(
                accommodationId = _id,
                bookingId = bookingId,
                message = "Accommodation $_id cannot be booked when in status $status"
            )
        }

        this.status = AccommodationStatusEnum.BOOKED
        this.bookingId = bookingId

        return AccommodationBookedEvent(
            accommodationId = _id,
            bookingId = bookingId,
        )
    }

    fun cancelBooking(bookingId: UUID): AccommodationEvent {
        statusCheck()
        if (this.status != AccommodationStatusEnum.BOOKED) {
            return AccommodationBookingCancelFailedEvent(
                accommodationId = _id,
                bookingId = bookingId,
                message = "Accommodation $_id booking cannot be canceled when in status $status"
            )
        }

        if (this.bookingId != bookingId) {
            return AccommodationBookingCancelFailedEvent(
                accommodationId = _id,
                bookingId = bookingId,
                message = "Accommodation $_id is not BOOKED by bookingId $bookingId"
            )
        }

        this.bookingId = null
        this.status = AccommodationStatusEnum.AVAILABLE

        return AccommodationBookingCanceledEvent(
            accommodationId = _id,
            bookingId = bookingId,
        )
    }

    private fun statusCheck() {
        if (!listOf(AccommodationStatusEnum.AVAILABLE, AccommodationStatusEnum.BOOKED).contains(this.status)) return
        if (LocalDateTime.now().isBefore(rent.from)) return

        this.status = AccommodationStatusEnum.EXPIRED
    }

    fun compensateBook(bookingId: UUID): AccommodationEvent {
        if (this.bookingId != bookingId) {
            return AccommodationBookingCancelFailedEvent(
                accommodationId = _id,
                bookingId = bookingId,
                message = "Accommodation $_id is not BOOKED by bookingId $bookingId"
            )
        }

        this.bookingId = null
        this.status = AccommodationStatusEnum.AVAILABLE

        return AccommodationBookingCanceledEvent(
            accommodationId = _id,
            bookingId = bookingId,
        )
    }

    fun compensateCancelBooking(bookingId: UUID): AccommodationEvent {
        if (this.bookingId != null) {
            return AccommodationBookFailedEvent(
                accommodationId = _id,
                bookingId = bookingId,
                message = "Accommodation $_id is already booked"
            )
        }

        this.status = AccommodationStatusEnum.BOOKED
        this.bookingId = bookingId

        return AccommodationBookedEvent(
            accommodationId = _id,
            bookingId = bookingId,
        )
    }
}
