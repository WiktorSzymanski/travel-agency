package pl.szymanski.wiktor.ta.commandhandler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.command.BookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CompensateBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CompensateCancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CompensateCancelReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CompensateReleaseTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CompensateReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CreateTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ExpireTravelOfferCommand
import pl.szymanski.wiktor.ta.command.MakeTravelOfferAvailableCommand
import pl.szymanski.wiktor.ta.command.MakeTravelOfferUnavailableCommand
import pl.szymanski.wiktor.ta.command.RebookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ReleaseTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferId
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import java.util.UUID
import kotlin.test.Test

class TravelOfferCommandHandlerTest {
    private val travelOfferRepository = mockk<TravelOfferRepository>()
    private val handler = TravelOfferCommandHandler(travelOfferRepository)
    private val travelOffer = mockk<TravelOffer>(relaxed = true)

    private val commuteId = CommuteId.generate()
    private val accommodationId = AccommodationId.generate()
    private val attractionId = AttractionId.generate()
    private val travelOfferId = TravelOfferId.generate(commuteId, accommodationId, attractionId)

    init {
        mockkObject(TravelOffer.Companion)
        coEvery { travelOfferRepository.findById(any()) } returns travelOffer
    }

    @Test
    fun `handle CreateTravelOfferCommand should delegate to TravelOffer_create`() =
        runTest {
            // Given
            val command =
                CreateTravelOfferCommand(
                    travelOfferId = travelOfferId,
                    correlationId = UUID.randomUUID(),
                    name = "Trip X",
                    commuteId = commuteId,
                    accommodationId = accommodationId,
                    attractionId = attractionId,
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 0) { travelOfferRepository.findById(any()) }
            verify(exactly = 1) { TravelOffer.create(any(), any(), any(), any()) }
        }

    @Test
    fun `handle ReserveTravelOfferCommand should call travelOffer_reserve`() =
        runTest {
            // Given
            val command =
                ReserveTravelOfferCommand(
                    travelOfferId = travelOfferId,
                    correlationId = UUID.randomUUID(),
                    bookingId = BookingId.generate(),
                    seat = Seat.Picked("1", "A"),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { travelOfferRepository.findById(any()) }
            verify(exactly = 1) { travelOffer.reserve(any(), any()) }
        }

    @Test
    fun `handle CancelReserveTravelOfferCommand should call travelOffer_cancelReservation`() =
        runTest {
            // Given
            val command =
                CancelReserveTravelOfferCommand(
                    travelOfferId = travelOfferId,
                    correlationId = UUID.randomUUID(),
                    bookingId = BookingId.generate(),
                    seat = Seat.Picked("1", "B"),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { travelOfferRepository.findById(any()) }
            verify(exactly = 1) { travelOffer.cancelReservation(any(), any()) }
        }

    @Test
    fun `handle BookTravelOfferCommand should call travelOffer_book`() =
        runTest {
            // Given
            val command =
                BookTravelOfferCommand(
                    travelOfferId = travelOfferId,
                    correlationId = UUID.randomUUID(),
                    bookingId = BookingId.generate(),
                    seat = Seat.Picked("2", "A"),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { travelOfferRepository.findById(any()) }
            verify(exactly = 1) { travelOffer.book(any(), any()) }
        }

    @Test
    fun `handle CancelBookTravelOfferCommand should call travelOffer_cancelBooking`() =
        runTest {
            // Given
            val command =
                CancelBookTravelOfferCommand(
                    travelOfferId = travelOfferId,
                    correlationId = UUID.randomUUID(),
                    bookingId = BookingId.generate(),
                    seat = Seat.Picked("2", "B"),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { travelOfferRepository.findById(any()) }
            verify(exactly = 1) { travelOffer.cancelBooking(any(), any()) }
        }

    @Test
    fun `handle ReleaseTravelOfferCommand should call travelOffer_releaseBooking`() =
        runTest {
            // Given
            val command =
                ReleaseTravelOfferCommand(
                    travelOfferId = travelOfferId,
                    correlationId = UUID.randomUUID(),
                    bookingId = BookingId.generate(),
                    seat = Seat.Picked("3", "A"),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { travelOfferRepository.findById(any()) }
            verify(exactly = 1) { travelOffer.releaseBooking(any(), any()) }
        }

    @Test
    fun `handle RebookTravelOfferCommand should call travelOffer_rebook`() =
        runTest {
            // Given
            val command =
                RebookTravelOfferCommand(
                    travelOfferId = travelOfferId,
                    correlationId = UUID.randomUUID(),
                    bookingId = BookingId.generate(),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { travelOfferRepository.findById(any()) }
            verify(exactly = 1) { travelOffer.rebook(any()) }
        }

    @Test
    fun `handle ExpireTravelOfferCommand should call travelOffer_expire`() =
        runTest {
            // Given
            val command =
                ExpireTravelOfferCommand(
                    travelOfferId = travelOfferId,
                    correlationId = UUID.randomUUID(),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { travelOfferRepository.findById(any()) }
            verify(exactly = 1) { travelOffer.expire() }
        }

    @Test
    fun `handle MakeTravelOfferAvailableCommand should call travelOffer_makeAvailable`() =
        runTest {
            // Given
            val command =
                MakeTravelOfferAvailableCommand(
                    travelOfferId = travelOfferId,
                    correlationId = UUID.randomUUID(),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { travelOfferRepository.findById(any()) }
            verify(exactly = 1) { travelOffer.makeAvailable() }
        }

    @Test
    fun `handle MakeTravelOfferUnavailableCommand should call travelOffer_makeUnavailable`() =
        runTest {
            // Given
            val command =
                MakeTravelOfferUnavailableCommand(
                    travelOfferId = travelOfferId,
                    correlationId = UUID.randomUUID(),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { travelOfferRepository.findById(any()) }
            verify(exactly = 1) { travelOffer.makeUnavailable() }
        }

    @Test
    fun `compensate TravelOfferReleaseEvent should call travelOffer_rebook`() =
        runTest {
            // Given
            val command =
                CompensateReleaseTravelOfferCommand(
                    travelOfferId = travelOfferId,
                    correlationId = UUID.randomUUID(),
                    eventId = UUID.randomUUID(),
                    bookingId = BookingId.generate(),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { travelOfferRepository.findById(any()) }
            verify(exactly = 1) { travelOffer.rebook(any()) }
        }

    @Test
    fun `compensate TravelOfferBookedEvent should call travelOffer_cancelBooking`() =
        runTest {
            // Given
            val command =
                CompensateBookTravelOfferCommand(
                    travelOfferId = travelOfferId,
                    correlationId = UUID.randomUUID(),
                    eventId = UUID.randomUUID(),
                    bookingId = BookingId.generate(),
                    seat = Seat.Picked("3", "D"),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { travelOfferRepository.findById(any()) }
            verify(exactly = 1) { travelOffer.cancelBooking(any(), any()) }
        }

    @Test
    fun `compensate TravelOfferBookingCanceledEvent should call travelOffer_book`() =
        runTest {
            // Given
            val command =
                CompensateCancelBookTravelOfferCommand(
                    travelOfferId = travelOfferId,
                    correlationId = UUID.randomUUID(),
                    eventId = UUID.randomUUID(),
                    bookingId = BookingId.generate(),
                    seat = Seat.Picked("3", "D"),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { travelOfferRepository.findById(any()) }
            verify(exactly = 1) { travelOffer.book(any(), any()) }
        }

    @Test
    fun `compensate TravelOfferReservedEvent should call travelOffer_cancelReservation`() =
        runTest {
            // Given
            val command =
                CompensateReserveTravelOfferCommand(
                    travelOfferId = travelOfferId,
                    correlationId = UUID.randomUUID(),
                    eventId = UUID.randomUUID(),
                    bookingId = BookingId.generate(),
                    seat = Seat.Picked("3", "D"),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { travelOfferRepository.findById(any()) }
            verify(exactly = 1) { travelOffer.cancelReservation(any(), any()) }
        }

    @Test
    fun `compensate TravelOfferReservationCanceledEvent should call travelOffer_reserve`() =
        runTest {
            // Given
            val command =
                CompensateCancelReserveTravelOfferCommand(
                    travelOfferId = travelOfferId,
                    correlationId = UUID.randomUUID(),
                    eventId = UUID.randomUUID(),
                    bookingId = BookingId.generate(),
                    seat = Seat.Picked("3", "D"),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { travelOfferRepository.findById(any()) }
            verify(exactly = 1) { travelOffer.reserve(any(), any()) }
        }
}
