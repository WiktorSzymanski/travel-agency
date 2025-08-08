package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookFailedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCancelFailedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpireFailedEvent
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
        if (LocalDateTime.now().isBefore(date)) {
            return listOf(AttractionExpireFailedEvent(
                attractionId = _id,
                message = "Attraction $_id cannot expire before its date"
            ))
        }

        this.status = AttractionStatusEnum.EXPIRED

        return listOf(AttractionExpiredEvent(
            attractionId = _id,
        ))
    }

    fun book(bookingId: UUID): List<AttractionEvent> {
        statusCheck()

        if (status != AttractionStatusEnum.SCHEDULED) {
            return listOf(AttractionBookFailedEvent(
                attractionId = _id,
                bookingId = bookingId,
                message = "Attraction $_id is not open for booking, current status is $status"
            ))
        }

        if (bookings.any { it == bookingId }) {
            return listOf(AttractionBookFailedEvent(
                attractionId = _id,
                bookingId = bookingId,
                message = "Booking $bookingId already signed for Attraction $_id"
            ))
        }

        // just in case
        if (bookings.size >= capacity) {
            return listOf(AttractionBookFailedEvent(
                attractionId = _id,
                bookingId = bookingId,
                message = "Attraction $_id is fully booked"
            ))
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

        if (!listOf(AttractionStatusEnum.SCHEDULED, AttractionStatusEnum.FULL).contains(this.status)) {
            return listOf(AttractionBookingCancelFailedEvent(
                attractionId = _id,
                bookingId = bookingId,
                message = "Cannot cancel booking for Attraction $_id not in SCHEDULED status"
            ))
        }

        val removed = bookings.removeIf { it == bookingId }

        if (!removed) {
            return listOf(AttractionBookingCancelFailedEvent(
                attractionId = _id,
                bookingId = bookingId,
                message = "Booking $bookingId was not signed for Attraction $_id"
            ))
        }

        return listOfNotNull(AttractionBookingCanceledEvent(
            attractionId = _id,
            bookingId = bookingId,
        ),
            takeIf { slotsCheck() }.let {
                AttractionAvailableEvent(
                    attractionId = _id,
                )
            })
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
}
