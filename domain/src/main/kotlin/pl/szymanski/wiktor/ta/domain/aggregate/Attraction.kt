package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import java.time.LocalDateTime
import java.util.UUID

data class Attraction(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val location: LocationEnum,
    val date: LocalDateTime,
    val capacity: Int,
    val bookings: MutableList<UUID> = mutableListOf(),
    var status: AttractionStatusEnum = AttractionStatusEnum.SCHEDULED,
    val lastRevision: Int = -1,
) {
    fun expire(): List<AttractionEvent> {
        if (LocalDateTime.now().isBefore(date)) {
            throw AttractionExpireFailedException(id)
        }

        this.status = AttractionStatusEnum.EXPIRED

        return listOf(
            AttractionExpiredEvent(
                attractionId = id,
            ),
        )
    }

    fun book(bookingId: UUID): List<AttractionEvent> {
        statusCheck()

        // If capacity is already reached, report as fully booked
        if (bookings.size >= capacity) {
            throw AttractionBookFailedException(id)
        }

        if (status != AttractionStatusEnum.SCHEDULED) {
            throw AttractionBookFailedException(id, status)
        }

        if (bookings.any { it == bookingId }) {
            throw AttractionBookFailedException(bookingId, id)
        }

        bookings.add(bookingId)

        return listOfNotNull(
            AttractionBookedEvent(
                attractionId = id,
                bookingId = bookingId,
            ),
            takeIf { slotsCheck() }?.let {
                AttractionFullEvent(
                    attractionId = id,
                )
            },
        )
    }

    fun cancelBooking(bookingId: UUID): List<AttractionEvent> {
        statusCheck()

        if (!listOf(AttractionStatusEnum.SCHEDULED, AttractionStatusEnum.FULL).contains(this.status)) {
            throw AttractionBookingCancelFailedException(id, status)
        }

        val removed = bookings.removeIf { it == bookingId }

        if (!removed) {
            throw AttractionBookingCancelFailedException(bookingId, id)
        }

        return listOfNotNull(
            AttractionBookingCanceledEvent(
                attractionId = id,
                bookingId = bookingId,
            ),
            takeIf { slotsCheck() }.let {
                AttractionAvailableEvent(
                    attractionId = id,
                )
            },
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

        if (!removed) {
            throw AttractionBookingCancelFailedException(bookingId, id)
        }

        return listOfNotNull(
            AttractionBookingCanceledEvent(
                attractionId = id,
                bookingId = bookingId,
            ),
            takeIf { slotsCheck() }.let {
                AttractionAvailableEvent(
                    attractionId = id,
                )
            },
        )
    }

    fun compensateCancelBooking(bookingId: UUID): List<AttractionEvent> {
        if (bookings.any { it == bookingId }) {
            throw AttractionBookFailedException(bookingId, id)
        }

        // just in case
        if (bookings.size >= capacity) {
            throw AttractionBookFailedException(id)
        }

        bookings.add(bookingId)

        return listOfNotNull(
            AttractionBookedEvent(
                attractionId = id,
                bookingId = bookingId,
            ),
            takeIf { slotsCheck() }.let {
                AttractionFullEvent(
                    attractionId = id,
                )
            },
        )
    }
}
