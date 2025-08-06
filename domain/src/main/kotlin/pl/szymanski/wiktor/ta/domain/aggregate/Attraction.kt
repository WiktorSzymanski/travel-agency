package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import java.time.LocalDateTime
import java.util.UUID

data class Attraction(
    val _id: UUID = UUID.randomUUID(),
    val name: String,
    val location: LocationEnum,
    val date: LocalDateTime,
    val capacity: Int,
    val bookings: MutableList<UUID> = mutableListOf(),
    var status: AttractionStatusEnum = AttractionStatusEnum.SCHEDULED,
    val version: Int = 1,
) {
    companion object {
        fun create(
            name: String,
            location: LocationEnum,
            date: LocalDateTime,
            capacity: Int,
        ): Pair<Attraction, AttractionCreatedEvent> {
            val attraction =
                Attraction(
                    name = name,
                    location = location,
                    date = date,
                    capacity = capacity,
                )

            val event =
                AttractionCreatedEvent(
                    attractionId = attraction._id,
                    name = name,
                    location = location,
                    date = date,
                    capacity = capacity,
                )

            return attraction to event
        }
    }

    fun expire(): AttractionEvent {
        require(LocalDateTime.now().isAfter(date)) {
            "Attraction $_id cannot expire before its date"
        }

        this.status = AttractionStatusEnum.EXPIRED

        return AttractionExpiredEvent(
            attractionId = _id,
        )
    }

    fun book(bookingId: UUID): AttractionEvent {
        statusCheck()

        require(status == AttractionStatusEnum.SCHEDULED) {
            "Attraction $_id is not open for booking"
        }

        require(bookings.none { it == bookingId }) {
            "Booking $bookingId already signed for Attraction $_id"
        }

        require(bookings.size < capacity) {
            "Attraction $_id is fully booked"
        }

        bookings.add(bookingId)

        return AttractionBookedEvent(
            attractionId = _id,
            bookingId = bookingId,
        )
    }

    fun cancelBooking(bookingId: UUID): AttractionEvent {
        statusCheck()

        require(status == AttractionStatusEnum.SCHEDULED) {
            "Cannot cancel booking for Attraction $_id not in SCHEDULED status"
        }

        val removed = bookings.removeIf { it == bookingId }

        require(removed) {
            "Booking $bookingId was not signed for Attraction $_id"
        }

        return AttractionBookingCanceledEvent(
            attractionId = _id,
            bookingId = bookingId,
        )
    }

    private fun statusCheck() {
        if (!listOf(AttractionStatusEnum.SCHEDULED, AttractionStatusEnum.FULL).contains(this.status)) return
        if (LocalDateTime.now().isBefore(date)) return

        this.status = AttractionStatusEnum.EXPIRED
    }
}
