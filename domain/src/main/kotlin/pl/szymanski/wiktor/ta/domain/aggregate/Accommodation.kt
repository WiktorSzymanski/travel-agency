package pl.szymanski.wiktor.ta.domain.aggregate

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.domain.exception.AccommodationBookingCancelFailedException
import pl.szymanski.wiktor.ta.domain.exception.AccommodationBookingFailedException
import pl.szymanski.wiktor.ta.domain.exception.AccommodationExpireFailedException
import pl.szymanski.wiktor.ta.domain.exception.AccommodationMissingCreatedEventException
import pl.szymanski.wiktor.ta.domain.exception.AccommodationEmptyEventListException
import java.time.LocalDateTime

@Serializable
data class Accommodation(
    val id: AccommodationId = AccommodationId.generate(),
    val name: String,
    val location: LocationEnum,
    val rent: Rent,
    var bookingId: BookingId = BookingId.Empty,
    var status: AccommodationStatusEnum = AccommodationStatusEnum.AVAILABLE,
) {
    companion object {
        fun create(
            id: AccommodationId,
            name: String,
            location: LocationEnum,
            rent: Rent,
        ): Pair<Accommodation, AccommodationCreatedEvent> {
            val accommodation =
                Accommodation(
                    id = id,
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
    
        fun fromEvents(events: List<AccommodationEvent>): Accommodation {
            if (events.isEmpty())
                throw AccommodationEmptyEventListException()

            val createdEvent = events.first()

            if (createdEvent !is AccommodationCreatedEvent)
                throw AccommodationMissingCreatedEventException()

            val accommodation =
                Accommodation(
                    id = createdEvent.accommodationId,
                    name = createdEvent.name,
                    location = createdEvent.location,
                    rent = createdEvent.rent,
                )

            for (event in events.drop(1)) accommodation.apply(event)

            return accommodation
        }
    }

    fun apply(event: AccommodationEvent): Unit = when (event) {
        is AccommodationCreatedEvent -> Unit
        
        is AccommodationBookedEvent -> {
            this.status = AccommodationStatusEnum.BOOKED
            this.bookingId = event.bookingId
        }

        is AccommodationBookingCanceledEvent -> {
            this.status = AccommodationStatusEnum.AVAILABLE
            this.bookingId = BookingId.Empty
        }

        is AccommodationExpiredEvent -> {
            this.status = AccommodationStatusEnum.EXPIRED
        }

        is AccommodationBookedCompensatedEvent -> {
            this.status = AccommodationStatusEnum.AVAILABLE
            this.bookingId = BookingId.Empty
        }

        is AccommodationBookingCanceledCompensatedEvent -> {
            this.status = AccommodationStatusEnum.BOOKED
            this.bookingId = event.bookingId
        }
    }

    fun expire(): List<AccommodationEvent> {
        if (status != AccommodationStatusEnum.AVAILABLE) {
            throw AccommodationExpireFailedException(id, status)
        }

        if (LocalDateTime.now().isBefore(rent.from)) {
            throw AccommodationExpireFailedException(id)
        }

        this.status = AccommodationStatusEnum.EXPIRED

        return listOf(AccommodationExpiredEvent(
            accommodationId = id,
        ))
    }

    fun book(bookingId: BookingId): List<AccommodationEvent> {
        statusCheck()
        if (this.status != AccommodationStatusEnum.AVAILABLE) {
            throw AccommodationBookingFailedException(id, status)
        }

        this.status = AccommodationStatusEnum.BOOKED
        this.bookingId = bookingId

        return listOf(AccommodationBookedEvent(
            accommodationId = id,
            bookingId = bookingId,
        ))
    }

    fun cancelBooking(bookingId: BookingId): List<AccommodationEvent> {
        statusCheck()
        if (this.status != AccommodationStatusEnum.BOOKED) {
            throw AccommodationBookingCancelFailedException(id, status)
        }

        if (this.bookingId != bookingId) {
            throw AccommodationBookingCancelFailedException(id, bookingId)
        }

        this.bookingId = BookingId.Empty
        this.status = AccommodationStatusEnum.AVAILABLE

        return listOf(AccommodationBookingCanceledEvent(
            accommodationId = id,
            bookingId = bookingId,
        ))
    }

    private fun statusCheck() {
        if (!listOf(AccommodationStatusEnum.AVAILABLE, AccommodationStatusEnum.BOOKED).contains(this.status)) return
        if (LocalDateTime.now().isBefore(rent.from)) return

        this.status = AccommodationStatusEnum.EXPIRED
    }

    // Note: Compensation methods currently return domain events which are later mapped to compensation events.
    fun compensateBook(bookingId: BookingId): List<AccommodationEvent> {
        if (this.bookingId != bookingId) {
            throw AccommodationBookingCancelFailedException(id, bookingId)
        }

        this.bookingId = BookingId.Empty
        this.status = AccommodationStatusEnum.AVAILABLE

        return listOf(AccommodationBookedCompensatedEvent(
            accommodationId = id,
            bookingId = bookingId,
        ))
    }

    fun compensateCancelBooking(bookingId: BookingId): List<AccommodationEvent> {
        if (this.bookingId != BookingId.Empty) {
            throw AccommodationBookingFailedException(id)
        }

        this.status = AccommodationStatusEnum.BOOKED
        this.bookingId = bookingId

        return listOf(AccommodationBookingCanceledCompensatedEvent(
            accommodationId = id,
            bookingId = bookingId,
        ))
    }
}
