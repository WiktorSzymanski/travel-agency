package pl.szymanski.wiktor.ta.commandhandler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.command.BookingRequestCancelCommand
import pl.szymanski.wiktor.ta.command.CancelBookingCommand
import pl.szymanski.wiktor.ta.command.CompleteBookingCommand
import pl.szymanski.wiktor.ta.command.CreateBookingCommand
import pl.szymanski.wiktor.ta.command.FailBookingCommand
import pl.szymanski.wiktor.ta.command.FailCancelBookingCommand
import pl.szymanski.wiktor.ta.command.ProcessBookingCommand
import pl.szymanski.wiktor.ta.command.ProcessCancelBookingCommand
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.repository.BookingRepository
import java.util.UUID
import kotlin.test.Test

class BookingCommandHandlerTest {
    private val bookingRepository = mockk<BookingRepository>()
    private val handler = BookingCommandHandler(bookingRepository)
    private val booking = mockk<Booking>(relaxed = true)

    init {
        mockkObject(Booking.Companion)
        coEvery { bookingRepository.findById(any()) } returns booking
    }

    @Test
    fun `handle CreateBookingCommand should delegate to Booking_create`() =
        runTest {
            // Given
            val command =
                CreateBookingCommand(
                    bookingId = BookingId.generate(),
                    correlationId = UUID.randomUUID(),
                    travelOffer = TravelOffer(
                        CommuteId.generate(),
                        AccommodationId.generate(),
                    ),
                    userId = UUID.randomUUID(),
                    seat = Seat.Picked("1", "A"),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 0) { bookingRepository.findById(any()) }
            verify(exactly = 1) { Booking.create(any(), any(), any()) }
        }

    @Test
    fun `handle BookingRequestCancelCommand should call booking_requestCancel`() =
        runTest {
            // Given
            val command =
                BookingRequestCancelCommand(
                    bookingId = BookingId.generate(),
                    correlationId = UUID.randomUUID(),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { bookingRepository.findById(any()) }
            verify(exactly = 1) { booking.requestCancel() }
        }

    @Test
    fun `handle ProcessBookingCommand should call booking_process`() =
        runTest {
            // Given
            val command =
                ProcessBookingCommand(
                    bookingId = BookingId.generate(),
                    correlationId = UUID.randomUUID(),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { bookingRepository.findById(any()) }
            verify(exactly = 1) { booking.process() }
        }

    @Test
    fun `handle CompleteBookingCommand should call booking_complete`() =
        runTest {
            // Given
            val command =
                CompleteBookingCommand(
                    bookingId = BookingId.generate(),
                    correlationId = UUID.randomUUID(),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { bookingRepository.findById(any()) }
            verify(exactly = 1) { booking.complete() }
        }

    @Test
    fun `handle CancelBookingCommand should call booking_cancel`() =
        runTest {
            // Given
            val command =
                CancelBookingCommand(
                    bookingId = BookingId.generate(),
                    correlationId = UUID.randomUUID(),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { bookingRepository.findById(any()) }
            verify(exactly = 1) { booking.cancel() }
        }

    @Test
    fun `handle FailBookingCommand should call booking_fail`() =
        runTest {
            // Given
            val command =
                FailBookingCommand(
                    bookingId = BookingId.generate(),
                    correlationId = UUID.randomUUID(),
                    message = "error message",
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { bookingRepository.findById(any()) }
            verify(exactly = 1) { booking.fail(any()) }
        }

    @Test
    fun `handle FailCancelBookingCommand should call booking_failCancellation`() =
        runTest {
            // Given
            val command =
                FailCancelBookingCommand(
                    bookingId = BookingId.generate(),
                    correlationId = UUID.randomUUID(),
                    message = "cancellation failed",
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { bookingRepository.findById(any()) }
            verify(exactly = 1) { booking.failCancellation(any()) }
        }

    @Test
    fun `handle ProcessCancelBookingCommand should call booking_processCancellation`() =
        runTest {
            // Given
            val command =
                ProcessCancelBookingCommand(
                    bookingId = BookingId.generate(),
                    correlationId = UUID.randomUUID(),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { bookingRepository.findById(any()) }
            verify(exactly = 1) { booking.processCancellation() }
        }
}
