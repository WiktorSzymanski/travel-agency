package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import java.time.LocalDateTime
import java.util.UUID

data class Accommodation(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val location: LocationEnum,
    val rent: Rent,
    var bookingId: UUID? = null,
    var status: AccommodationStatusEnum = AccommodationStatusEnum.AVAILABLE,
    val lastRevision: Int = -1,
) {
    fun expire(): AccommodationEvent {
        if (status != AccommodationStatusEnum.AVAILABLE) {
            throw AccommodationExpireFailedException(id, status)
        }

        if (LocalDateTime.now().isBefore(rent.from)) {
            throw AccommodationExpireFailedException(id)
        }

        this.status = AccommodationStatusEnum.EXPIRED

        return AccommodationExpiredEvent(
            accommodationId = id,
        )
    }

    fun book(bookingId: UUID): AccommodationEvent {
        statusCheck()
        if (this.status != AccommodationStatusEnum.AVAILABLE) {
            throw AccommodationBookingFailedException(id, status)
        }

        this.status = AccommodationStatusEnum.BOOKED
        this.bookingId = bookingId

        return AccommodationBookedEvent(
            accommodationId = id,
            bookingId = bookingId,
        )
    }

    fun cancelBooking(bookingId: UUID): AccommodationEvent {
        statusCheck()
        if (this.status != AccommodationStatusEnum.BOOKED) {
            throw AccommodationBookingCancelFailedException(id, status)
        }

        if (this.bookingId != bookingId) {
            throw AccommodationBookingCancelFailedException(id, bookingId)
        }

        this.bookingId = null
        this.status = AccommodationStatusEnum.AVAILABLE

        return AccommodationBookingCanceledEvent(
            accommodationId = id,
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
            throw AccommodationBookingCancelFailedException(id, bookingId)
        }

        this.bookingId = null
        this.status = AccommodationStatusEnum.AVAILABLE

        return AccommodationBookingCanceledEvent(
            accommodationId = id,
            bookingId = bookingId,
        )
    }

    fun compensateCancelBooking(bookingId: UUID): AccommodationEvent {
        if (this.bookingId != null) {
            throw AccommodationBookingFailedException(id)
        }

        this.status = AccommodationStatusEnum.BOOKED
        this.bookingId = bookingId

        return AccommodationBookedEvent(
            accommodationId = id,
            bookingId = bookingId,
        )
    }
}
