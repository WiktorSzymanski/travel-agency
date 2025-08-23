package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
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
        ): Pair<Attraction, List<AttractionCreatedEvent>> {
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

            return attraction to listOf(event)
        }
    }

    // SHOULD ADD COMPENSATING METHODS THAT IGNORE EXPIRED STATUS

    fun expire(): List<AttractionEvent> {
        require(!LocalDateTime.now().isBefore(date)) {
            "Attraction $_id cannot expire before its date"
        }

        this.status = AttractionStatusEnum.EXPIRED

        return listOf(
            AttractionExpiredEvent(
                attractionId = _id,
            )
        )
    }

    fun book(bookingId: UUID): List<AttractionEvent> {
        statusCheck()

        require(status == AttractionStatusEnum.SCHEDULED) {
            "Attraction $_id is not open for booking, current status is $status"
        }

        require(bookings.none { it == bookingId }) {
            "Booking $bookingId already signed for Attraction $_id"
        }

        // just in case
        require(bookings.size < capacity) {
            "Attraction $_id is fully booked"
        }

        bookings.add(bookingId)

        return listOfNotNull(
            AttractionBookedEvent(
                attractionId = _id,
                bookingId = bookingId,
            ),
            takeIf { slotsCheck() }.let {
                AttractionFullEvent(
                    attractionId = _id,
                )
            }
        )
    }

    fun cancelBooking(bookingId: UUID): List<AttractionEvent> {
        statusCheck()

        require(listOf(AttractionStatusEnum.SCHEDULED, AttractionStatusEnum.FULL).contains(this.status)) {
            "Cannot cancel booking for Attraction $_id not in SCHEDULED or FULL status"
        }

        val removed = bookings.removeIf { it == bookingId }

        require(removed) {
            "Booking $bookingId was not signed for Attraction $_id"
        }

        return listOfNotNull(
            AttractionBookingCanceledEvent(
                attractionId = _id,
                bookingId = bookingId,
            ),
            takeIf { slotsCheck() }.let {
                AttractionAvailableEvent(
                    attractionId = _id,
                )
            }
        )
    }

    private fun slotsCheck(): Boolean {
        return when (this.capacity == this.bookings.size) {
            true -> {
                this.status = AttractionStatusEnum.FULL
                true
            }
            false -> {
                if (this.status == AttractionStatusEnum.FULL) {
                    this.status = AttractionStatusEnum.SCHEDULED
                    return true
                }
                false
            }
        }
    }

    private fun statusCheck() {
        if (!listOf(AttractionStatusEnum.SCHEDULED, AttractionStatusEnum.FULL).contains(this.status)) return
        if (LocalDateTime.now().isBefore(date)) return

        this.status = AttractionStatusEnum.EXPIRED
    }

    fun compensateBook(bookingId: UUID): List<AttractionEvent> {
        val removed = bookings.removeIf { it == bookingId }

        require(removed) {
            "Booking $bookingId was not signed for Attraction $_id"
        }

        return listOfNotNull(
            AttractionBookingCanceledEvent(
                attractionId = _id,
                bookingId = bookingId,
            ),
            takeIf { slotsCheck() }.let {
                AttractionAvailableEvent(
                    attractionId = _id,
                )
            }
        )
    }

    fun compensateCancelBooking(bookingId: UUID): List<AttractionEvent> {
        require(bookings.none { it == bookingId }) {
            "Booking $bookingId already signed for Attraction $_id"
        }

        // just in case
        require(bookings.size < capacity) {
            "Attraction $_id is fully booked"
        }

        bookings.add(bookingId)

        return listOfNotNull(
            AttractionBookedEvent(
                attractionId = _id,
                bookingId = bookingId,
            ),
            takeIf { slotsCheck() }.let {
                AttractionFullEvent(
                    attractionId = _id,
                )
            }
        )
    }
}
