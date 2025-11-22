package pl.szymanski.wiktor.ta.commandHandler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.command.BookAttractionCommand
import pl.szymanski.wiktor.ta.command.CancelAttractionBookingCommand
import pl.szymanski.wiktor.ta.command.CompensateBookAttractionCommand
import pl.szymanski.wiktor.ta.command.CompensateCancelAttractionBookingCommand
import pl.szymanski.wiktor.ta.command.CreateAttractionCommand
import pl.szymanski.wiktor.ta.command.ExpireAttractionCommand
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class AttractionCommandHandlerTest {
    private val attractionRepository = mockk<AttractionRepository>()
    private val handler = AttractionCommandHandler(attractionRepository)
    private val attraction = mockk<Attraction>(relaxed = true)

    init {
        mockkObject(Attraction.Companion)
        coEvery { attractionRepository.findById(any()) } returns attraction
    }

    @Test
    fun `handle CreateAttractionCommand should delegate to Attraction_create`() =
        runTest {
            // Given
            val command =
                CreateAttractionCommand(
                    attractionId = UUID.randomUUID(),
                    correlationId = UUID.randomUUID(),
                    name = "AttractionName",
                    location = LocationEnum.PARIS,
                    date = LocalDateTime.now().plusDays(1),
                    capacity = 10,
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 0) { attractionRepository.findById(any()) }
            verify(exactly = 1) { Attraction.create(any(), any(), any(), any()) }
        }

    @Test
    fun `handle BookAttractionCommand should call attraction_book`() =
        runTest {
            // Given
            val command =
                BookAttractionCommand(
                    attractionId = UUID.randomUUID(),
                    correlationId = UUID.randomUUID(),
                    bookingId = UUID.randomUUID(),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { attractionRepository.findById(any()) }
            verify(exactly = 1) { attraction.book(any()) }
        }

    @Test
    fun `handle CancelAttractionBookingCommand should call attraction_cancelBooking`() =
        runTest {
            // Given
            val command =
                CancelAttractionBookingCommand(
                    attractionId = UUID.randomUUID(),
                    correlationId = UUID.randomUUID(),
                    bookingId = UUID.randomUUID(),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { attractionRepository.findById(any()) }
            verify(exactly = 1) { attraction.cancelBooking(any()) }
        }

    @Test
    fun `handle ExpireAttractionCommand should call attraction_expire`() =
        runTest {
            // Given
            val command =
                ExpireAttractionCommand(
                    attractionId = UUID.randomUUID(),
                    correlationId = UUID.randomUUID(),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { attractionRepository.findById(any()) }
            verify(exactly = 1) { attraction.expire() }
        }

    @Test
    fun `compensate AttractionBookedEvent should call attraction_compensateBook`() =
        runTest {
            // Given
            val command =
                CompensateBookAttractionCommand(
                    attractionId = UUID.randomUUID(),
                    correlationId = UUID.randomUUID(),
                    eventId = UUID.randomUUID(),
                    bookingId = UUID.randomUUID(),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { attractionRepository.findById(any()) }
            verify(exactly = 1) { attraction.compensateBook(any()) }
        }

    @Test
    fun `compensate AttractionBookingCanceledEvent should call attraction_compensateCancelBooking`() =
        runTest {
            // Given
            val command =
                CompensateCancelAttractionBookingCommand(
                    attractionId = UUID.randomUUID(),
                    correlationId = UUID.randomUUID(),
                    eventId = UUID.randomUUID(),
                    bookingId = UUID.randomUUID(),
                )

            // When
            handler.handle(command)

            // Then
            coVerify(exactly = 1) { attractionRepository.findById(any()) }
            verify(exactly = 1) { attraction.compensateCancelBooking(any()) }
        }
}
