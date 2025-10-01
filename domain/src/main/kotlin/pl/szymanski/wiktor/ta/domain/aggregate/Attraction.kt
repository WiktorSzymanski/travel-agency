package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
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
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val location: LocationEnum,
    val date: LocalDateTime,
    val capacity: Int,
    val bookings: MutableList<UUID> = mutableListOf(),
    var status: AttractionStatusEnum = AttractionStatusEnum.SCHEDULED,
    val lastRevision: Int = 0,
    val lastEtag : String? = null
) {
    fun apply(event: AttractionEvent, revision: Int, etag: String): Attraction {
        return when (event) {
            is AttractionCreatedEvent -> this.copy(
                id = event.attractionId,
                name = event.name,
                location = event.location,
                date = event.date,
                capacity = event.capacity,
                status = AttractionStatusEnum.SCHEDULED,
                bookings = mutableListOf(),
                lastRevision = revision,
                lastEtag = etag
            )
            is AttractionBookedEvent -> {
                val newBookings = this.bookings.toMutableList()
                newBookings.add(event.bookingId)
                this.copy(
                    bookings = newBookings,
                    lastRevision = revision,
                    lastEtag = etag
                )
            }
            is AttractionFullEvent -> this.copy(
                status = AttractionStatusEnum.FULL,
                lastRevision = revision,
                lastEtag = etag
            )
            is AttractionAvailableEvent -> this.copy(
                status = AttractionStatusEnum.SCHEDULED,
                lastRevision = revision,
                lastEtag = etag
            )
            is AttractionBookingCanceledEvent -> {
                val newBookings = this.bookings.toMutableList()
                newBookings.removeIf { it == event.bookingId }
                this.copy(
                    bookings = newBookings,
                    lastRevision = revision,
                    lastEtag = etag
                )
            }
            is AttractionExpiredEvent -> this.copy(
                status = AttractionStatusEnum.EXPIRED,
                lastRevision = revision,
                lastEtag = etag
            )
            else -> this.copy(
                lastRevision = revision,
                lastEtag = etag)
        }
    }
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
        
        fun fromEvents(events: List<Triple<AttractionEvent, Int, String>>): Attraction? {
            if (events.isEmpty()) return null

            val (createdEvent, _) = events.first()

            require(createdEvent is AttractionCreatedEvent) { "First event must be AttractionCreatedEvent" }

            var attraction = Attraction(
                id = createdEvent.attractionId,
                name = createdEvent.name,
                location = createdEvent.location,
                date = createdEvent.date,
                capacity = createdEvent.capacity,
            )

            for ((event, revision, etag) in events) {
                attraction = attraction.apply(event, revision, etag)
            }
            
            return attraction
        }
    }

    // SHOULD ADD COMPENSATING METHODS THAT IGNORE EXPIRED STATUS

    fun expire(): List<AttractionEvent> {
        require(!LocalDateTime.now().isBefore(date)) {
            "Attraction $id cannot expire before its date"
        }

        this.status = AttractionStatusEnum.EXPIRED

        return listOf(
            AttractionExpiredEvent(
                attractionId = id,
            )
        )
    }

    fun book(bookingId: UUID): List<AttractionEvent> {
        statusCheck()

        require(status == AttractionStatusEnum.SCHEDULED) {
            "Attraction $id is not open for booking, current status is $status"
        }

        require(bookings.none { it == bookingId }) {
            "Booking $bookingId already signed for Attraction $id"
        }

        // just in case
        require(bookings.size < capacity) {
            "Attraction $id is fully booked"
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
            }
        )
    }

    fun cancelBooking(bookingId: UUID): List<AttractionEvent> {
        statusCheck()

        require(listOf(AttractionStatusEnum.SCHEDULED, AttractionStatusEnum.FULL).contains(this.status)) {
            "Cannot cancel booking for Attraction $id not in SCHEDULED or FULL status"
        }

        val removed = bookings.removeIf { it == bookingId }

        require(removed) {
            "Booking $bookingId was not signed for Attraction $id"
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
            "Booking $bookingId was not signed for Attraction $id"
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
            }
        )
    }

    fun compensateCancelBooking(bookingId: UUID): List<AttractionEvent> {
        require(bookings.none { it == bookingId }) {
            "Booking $bookingId already signed for Attraction $id"
        }

        // just in case
        require(bookings.size < capacity) {
            "Attraction $id is fully booked"
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
            }
        )
    }
}