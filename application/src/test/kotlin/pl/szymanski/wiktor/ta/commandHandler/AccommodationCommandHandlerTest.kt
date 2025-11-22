package pl.szymanski.wiktor.ta.commandHandler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.command.BookAccommodationCommand
import pl.szymanski.wiktor.ta.command.CancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.command.CompensateBookAccommodationCommand
import pl.szymanski.wiktor.ta.command.CompensateCancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.command.CreateAccommodationCommand
import pl.szymanski.wiktor.ta.command.ExpireAccommodationCommand
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class AccommodationCommandHandlerTest {
    private val accommodationRepository = mockk<AccommodationRepository>()
    private val handler = AccommodationCommandHandler(accommodationRepository)
    private val accommodation = mockk<Accommodation>(relaxed = true)

    init {
        mockkObject(Accommodation.Companion)
        coEvery { accommodationRepository.findById(any()) } returns accommodation
    }

    @Test
    fun `handle CreateAccommodationCommand should delegate to Accommodation_create`() = runTest {
        // Given
        val command = CreateAccommodationCommand(
            accommodationId = UUID.randomUUID(),
            correlationId = UUID.randomUUID(),
            name = "AccommodationName",
            location = LocationEnum.PARIS,
            rent = Rent(
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().plusDays(5)
            )
        )

        // When
        handler.handle(command)

        // Then
        coVerify(exactly = 0) { accommodationRepository.findById(any()) }
        verify(exactly = 1) { Accommodation.create(any(), any(), any()) }
    }

    @Test
    fun `handle BookAccommodationCommand should call accommodation_book`() = runTest {
        // Given
        val command = BookAccommodationCommand(
            accommodationId = UUID.randomUUID(),
            correlationId = UUID.randomUUID(),
            bookingId = UUID.randomUUID(),
        )

        // When
        handler.handle(command)

        // Then
        coVerify(exactly = 1) { accommodationRepository.findById(any()) }
        verify(exactly = 1) { accommodation.book(any()) }
    }

    @Test
    fun `handle CancelAccommodationBookingCommand should call accommodation_cancelBooking`() = runTest {
        // Given
        val command = CancelAccommodationBookingCommand(
            accommodationId = UUID.randomUUID(),
            correlationId = UUID.randomUUID(),
            bookingId = UUID.randomUUID(),
        )

        // When
        handler.handle(command)

        // Then
        coVerify(exactly = 1) { accommodationRepository.findById(any()) }
        verify(exactly = 1) { accommodation.cancelBooking(any()) }
    }

    @Test
    fun `handle ExpireAccommodationCommand should call accommodation_expire`() = runTest {
        // Given
        val command = ExpireAccommodationCommand(
            accommodationId = UUID.randomUUID(),
            correlationId = UUID.randomUUID(),
        )

        // When
        handler.handle(command)

        // Then
        coVerify(exactly = 1) { accommodationRepository.findById(any()) }
        verify(exactly = 1) { accommodation.expire() }
    }

    @Test
    fun `compensate AccommodationBookedEvent should call accommodation_compensateBook`() = runTest {
        // Given
        val command = CompensateBookAccommodationCommand(
            accommodationId = UUID.randomUUID(),
            correlationId = UUID.randomUUID(),
            eventId = UUID.randomUUID(),
            bookingId = UUID.randomUUID()
        )

        // When
        handler.handle(command)

        // Then
        coVerify(exactly = 1) { accommodationRepository.findById(any()) }
        verify(exactly = 1) { accommodation.compensateBook(any()) }
    }

    @Test
    fun `compensate AccommodationBookingCanceledEvent should call accommodation_compensateCancelBooking`() = runTest {
        // Given
        val command = CompensateCancelAccommodationBookingCommand(
            accommodationId = UUID.randomUUID(),
            correlationId = UUID.randomUUID(),
            eventId = UUID.randomUUID(),
            bookingId = UUID.randomUUID()
        )

        // When
        handler.handle(command)

        // Then
        coVerify(exactly = 1) { accommodationRepository.findById(any()) }
        verify(exactly = 1) { accommodation.compensateCancelBooking(any()) }
    }
}
