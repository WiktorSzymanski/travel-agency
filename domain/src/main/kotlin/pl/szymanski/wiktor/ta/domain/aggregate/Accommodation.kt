package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
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
    val lastRevision: Int = 0,
    val lastEtag : String? = null
) {
    fun apply(event: AccommodationEvent, revision: Int, etag: String): Accommodation {
        return when (event) {
            is AccommodationCreatedEvent -> this.copy(
                id = event.accommodationId,
                name = event.name,
                location = event.location,
                rent = event.rent,
                status = AccommodationStatusEnum.AVAILABLE,
                lastRevision = revision,
                lastEtag = etag
            )
            is AccommodationBookedEvent -> this.copy(
                status = AccommodationStatusEnum.BOOKED,
                bookingId = event.bookingId,
                lastRevision = revision,
                lastEtag = etag
            )
            is AccommodationBookingCanceledEvent -> this.copy(
                status = AccommodationStatusEnum.AVAILABLE,
                bookingId = null,
                lastRevision = revision,
                lastEtag = etag
            )
            is AccommodationExpiredEvent -> this.copy(
                status = AccommodationStatusEnum.EXPIRED,
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
                    accommodationId = accommodation.id,
                    name = name,
                    location = location,
                    rent = rent,
                )

            return accommodation to event
        }
        
        fun fromEvents(events: List<Triple<AccommodationEvent, Int, String>>): Accommodation? {
            if (events.isEmpty()) return null

            val (createdEvent, _) = events.first()

            require(createdEvent is AccommodationCreatedEvent) { "First event must be AccommodationCreatedEvent" }

            var accommodation = Accommodation(
                id = createdEvent.accommodationId,
                name = createdEvent.name,
                location = createdEvent.location,
                rent = createdEvent.rent,
            )

            for ((event, revision, etag) in events) {
                accommodation = accommodation.apply(event, revision, etag)
            }
            
            return accommodation
        }
    }

    fun expire(): AccommodationEvent {
        require(status == AccommodationStatusEnum.AVAILABLE) {
            "Accommodation $id cannot expire in status $status"
        }

        require(!LocalDateTime.now().isBefore(rent.from)) {
            "Accommodation $id cannot be expired before its rent start"
        }

        this.status = AccommodationStatusEnum.EXPIRED

        return AccommodationExpiredEvent(
            accommodationId = id,
        )
    }

    fun book(bookingId: UUID): AccommodationEvent {
        statusCheck()
        require(this.status == AccommodationStatusEnum.AVAILABLE) {
            "Accommodation $id cannot be booked when in status $status"
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
        require(this.status == AccommodationStatusEnum.BOOKED) {
            "Accommodation $id booking cannot be canceled when in status $status"
        }

        require(this.bookingId == bookingId) {
            "Accommodation $id is not BOOKED by bookingId $bookingId"
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
        require(this.bookingId == bookingId) {
            "Accommodation $id is not BOOKED by bookingId $bookingId"
        }

        this.bookingId = null
        this.status = AccommodationStatusEnum.AVAILABLE

        return AccommodationBookingCanceledEvent(
            accommodationId = id,
            bookingId = bookingId,
        )
    }

    fun compensateCancelBooking(bookingId: UUID): AccommodationEvent {
        require(this.bookingId == null) {
            "Accommodation $id is already booked"
        }

        this.status = AccommodationStatusEnum.BOOKED
        this.bookingId = bookingId

        return AccommodationBookedEvent(
            accommodationId = id,
            bookingId = bookingId,
        )
    }
}