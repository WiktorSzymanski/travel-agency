package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import pl.szymanski.wiktor.ta.domain.exception.AttractionBookFailedException
import pl.szymanski.wiktor.ta.domain.exception.AttractionBookingCancelFailedException
import pl.szymanski.wiktor.ta.domain.exception.AttractionExpireFailedException
import pl.szymanski.wiktor.ta.domain.exception.AttractionMissingCreatedEventException
import pl.szymanski.wiktor.ta.domain.exception.AttractionEmptyEventListException
import java.time.LocalDateTime

data class Attraction(
    val id: AttractionId = AttractionId.generate(),
    val name: String,
    val location: LocationEnum,
    val date: LocalDateTime,
    val capacity: Int,
    val bookings: MutableList<BookingId> = mutableListOf(),
    var status: AttractionStatusEnum = AttractionStatusEnum.SCHEDULED,
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
                    attractionId = attraction.id,
                    name = name,
                    location = location,
                    date = date,
                    capacity = capacity,
                )

            return attraction to listOf(event)
        }

        fun fromEvents(events: List<AttractionEvent>): Attraction {
            if (events.isEmpty()) throw AttractionEmptyEventListException()

            val createdEvent = events.first()

            if (createdEvent !is AttractionCreatedEvent)
                throw AttractionMissingCreatedEventException()

            val attraction =
                Attraction(
                    id = createdEvent.attractionId,
                    name = createdEvent.name,
                    location = createdEvent.location,
                    date = createdEvent.date,
                    capacity = createdEvent.capacity,
                )

            for (event in events.drop(1)) {
                attraction.apply(event)
            }

            return attraction
        }
    }

    fun apply(event: AttractionEvent): Unit = when (event) {
        is AttractionCreatedEvent -> Unit

        is AttractionBookedEvent -> {
            this.bookings.add(event.bookingId)
            Unit
        }

        is AttractionFullEvent -> {
            this.status = AttractionStatusEnum.FULL
        }


        is AttractionAvailableEvent -> {
            this.status = AttractionStatusEnum.SCHEDULED
        }


        is AttractionBookingCanceledEvent -> {
            this.bookings.removeIf { it == event.bookingId }
            Unit
        }

        is AttractionExpiredEvent -> {
            this.status = AttractionStatusEnum.EXPIRED
        }


        is AttractionBookedCompensatedEvent -> {
            this.bookings.removeIf { it == event.bookingId }
            Unit
        }

        is AttractionBookingCanceledCompensatedEvent -> {
            this.bookings.add(event.bookingId)
            Unit
        }
    }

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

    fun book(bookingId: BookingId): List<AttractionEvent> {
        statusCheck()

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

    fun cancelBooking(bookingId: BookingId): List<AttractionEvent> {
        statusCheck()

        if (!listOf(AttractionStatusEnum.SCHEDULED, AttractionStatusEnum.FULL).contains(this.status)) {
            throw AttractionBookingCancelFailedException(id, status)
        }

        val removed = bookings.removeIf { it == bookingId }

        if (!removed) {
            throw AttractionBookingCancelFailedException(bookingId, id)
        }

        return listOf(
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

    fun compensateBook(bookingId: BookingId): List<AttractionEvent> {
        val removed = bookings.removeIf { it == bookingId }

        if (!removed) {
            throw AttractionBookingCancelFailedException(bookingId, id)
        }

        return listOf(
            AttractionBookedCompensatedEvent(
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

    fun compensateCancelBooking(bookingId: BookingId): List<AttractionEvent> {
        if (bookings.any { it == bookingId }) {
            throw AttractionBookFailedException(bookingId, id)
        }

        // just in case
        if (bookings.size >= capacity) {
            throw AttractionBookFailedException(id)
        }

        bookings.add(bookingId)

        return listOf(
            AttractionBookingCanceledCompensatedEvent(
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
