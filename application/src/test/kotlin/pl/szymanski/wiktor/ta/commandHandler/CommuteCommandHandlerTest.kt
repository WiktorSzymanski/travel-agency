package pl.szymanski.wiktor.ta.commandHandler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.command.BookCommuteCommand
import pl.szymanski.wiktor.ta.command.CancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.CreateCommuteCommand
import pl.szymanski.wiktor.ta.command.ExpireCommuteCommand
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class CommuteCommandHandlerTest {
    private val commuteRepository = mockk<CommuteRepository>()
    private val handler = CommuteCommandHandler(commuteRepository)
    private val commute = mockk<Commute>(relaxed = true)

    init {
        mockkObject(Commute.Companion)
        coEvery { commuteRepository.findById(any()) } returns commute
    }

    @Test
    fun `handle CreateCommuteCommand should delegate to Commute_create`() = runTest {
        // Given
        val command = CreateCommuteCommand(
            commuteId = UUID.randomUUID(),
            correlationId = UUID.randomUUID(),
            name = "CommuteName",
            departure = LocationAndTime(LocationEnum.PARIS, LocalDateTime.now().plusDays(1)),
            arrival = LocationAndTime(LocationEnum.LONDON, LocalDateTime.now().plusDays(2)),
            seats = listOf(Seat("1", "A")),
        )

        // When
        handler.handle(command)

        // Then
        coVerify(exactly = 0) { commuteRepository.findById(any()) }
        verify(exactly = 1) { Commute.create(any(), any(), any(), any()) }
    }

    @Test
    fun `handle BookCommuteCommand should call commute_bookSeat`() = runTest {
        // Given
        val command = BookCommuteCommand(
            commuteId = UUID.randomUUID(),
            correlationId = UUID.randomUUID(),
            bookingId = UUID.randomUUID(),
            seat = Seat("1", "B"),
        )

        // When
        handler.handle(command)

        // Then
        coVerify(exactly = 1) { commuteRepository.findById(any()) }
        verify(exactly = 1) { commute.bookSeat(any(), any()) }
    }

    @Test
    fun `handle CancelCommuteBookingCommand should call commute_cancelBookedSeat`() = runTest {
        // Given
        val command = CancelCommuteBookingCommand(
            commuteId = UUID.randomUUID(),
            correlationId = UUID.randomUUID(),
            bookingId = UUID.randomUUID(),
        )

        // When
        handler.handle(command)

        // Then
        coVerify(exactly = 1) { commuteRepository.findById(any()) }
        verify(exactly = 1) { commute.cancelBookedSeat(any()) }
    }

    @Test
    fun `handle ExpireCommuteCommand should call commute_expire`() = runTest {
        // Given
        val command = ExpireCommuteCommand(
            commuteId = UUID.randomUUID(),
            correlationId = UUID.randomUUID(),
        )

        // When
        handler.handle(command)

        // Then
        coVerify(exactly = 1) { commuteRepository.findById(any()) }
        verify(exactly = 1) { commute.expire() }
    }

    @Test
    fun `compensate CommuteBookedEvent should call commute_compensateBookSeat`() = runTest {
        // Given
        val event = CommuteBookedEvent(
            commuteId = UUID.randomUUID(),
            bookingId = UUID.randomUUID(),
            seat = Seat("2", "C"),
        )

        // When
        handler.compensate(event)

        // Then
        coVerify(exactly = 1) { commuteRepository.findById(any()) }
        verify(exactly = 1) { commute.compensateBookSeat(any()) }
    }

    @Test
    fun `compensate CommuteBookingCanceledEvent should call commute_compensateCancelBookedSeat`() = runTest {
        // Given
        val event = CommuteBookingCanceledEvent(
            commuteId = UUID.randomUUID(),
            bookingId = UUID.randomUUID(),
            seat = Seat("3", "D"),
        )

        // When
        handler.compensate(event)

        // Then
        coVerify(exactly = 1) { commuteRepository.findById(any()) }
        verify(exactly = 1) { commute.compensateCancelBookedSeat(any(), any()) }
    }
}
