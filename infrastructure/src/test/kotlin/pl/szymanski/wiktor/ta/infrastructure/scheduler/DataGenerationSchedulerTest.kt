package pl.szymanski.wiktor.ta.infrastructure.scheduler

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.CreateAccommodationCommand
import pl.szymanski.wiktor.ta.command.CreateAttractionCommand
import pl.szymanski.wiktor.ta.command.CreateCommuteCommand
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.infrastructure.config.DataGenerationSchedulerConfig
import pl.szymanski.wiktor.ta.infrastructure.generator.AccommodationTemplate
import pl.szymanski.wiktor.ta.infrastructure.generator.AttractionTemplate
import pl.szymanski.wiktor.ta.infrastructure.generator.CommuteTemplate
import java.util.UUID
import kotlin.test.Test

@ExperimentalCoroutinesApi
class DataGenerationSchedulerTest {
    private val mockAccommodationCommandHandler = mockk<AccommodationCommandHandler>()
    private val mockAttractionCommandHandler = mockk<AttractionCommandHandler>()
    private val mockCommuteCommandHandler = mockk<CommuteCommandHandler>()
    private val scheduler = DataGenerationScheduler

    @BeforeEach
    fun setup() {
        coEvery { mockAccommodationCommandHandler.handle(any<AccommodationCommand>()) } answers {
            val command = firstArg<CreateAccommodationCommand>()

            AccommodationCreatedEvent(
                eventId = UUID.randomUUID(),
                correlationId = command.correlationId,
                accommodationId = command.accommodationId,
                name = command.name,
                location = command.location,
                rent = command.rent,
            )
        }
        coEvery { mockAttractionCommandHandler.handle(any<AttractionCommand>()) } answers {
            val command = firstArg<CreateAttractionCommand>()

            AttractionCreatedEvent(
                eventId = UUID.randomUUID(),
                correlationId = command.correlationId,
                attractionId = command.attractionId,
                name = command.name,
                location = command.location,
                date = command.date,
                capacity = command.capacity,
            )
        }
        coEvery { mockCommuteCommandHandler.handle(any<CommuteCommand>()) } answers {
            val command = firstArg<CreateCommuteCommand>()

            CommuteCreatedEvent(
                eventId = UUID.randomUUID(),
                correlationId = command.correlationId,
                commuteId = command.commuteId,
                name = command.name,
                departure = LocationAndTime(command.departure.location, command.departure.time),
                arrival = LocationAndTime(command.arrival.location, command.arrival.time),
                seats = command.seats,
            )
        }

        scheduler.init(
            DataGenerationSchedulerConfig(
                5,
                5,
                5,
                listOf(
                    AccommodationTemplate(
                        "Test Accommodation",
                        "POZNAN",
                    ),
                ),
                listOf(
                    AttractionTemplate(
                        "Test Attraction",
                        "POZNAN",
                        10,
                    ),
                ),
                listOf(
                    CommuteTemplate(
                        "Test Commute",
                        "POZNAN",
                        "LONDON",
                        listOf(Seat("1", "A")),
                    ),
                ),
            ),
            mockAccommodationCommandHandler,
            mockAttractionCommandHandler,
            mockCommuteCommandHandler,
        )
    }

    @AfterEach
    fun cleanup() {
        scheduler.stop()
        clearMocks(mockAccommodationCommandHandler, mockAttractionCommandHandler, mockCommuteCommandHandler)
    }

    @Test
    fun `should generate and persist data when started`() =
        runTest {
            scheduler.start(this)
            runCurrent()

            coVerify(atLeast = 1) { mockAccommodationCommandHandler.handle(any<CreateAccommodationCommand>()) }
            coVerify(atLeast = 1) { mockAttractionCommandHandler.handle(any<CreateAttractionCommand>()) }
            coVerify(atLeast = 1) { mockCommuteCommandHandler.handle(any<CreateCommuteCommand>()) }
        }

    @Test
    fun `should stop generating data when stopped`() =
        runTest {
            scheduler.start(this)
            runCurrent()

            delay(1000)
            scheduler.stop()
            runCurrent()

            coVerify(exactly = 1) { mockAccommodationCommandHandler.handle(any<CreateAccommodationCommand>()) }
            coVerify(exactly = 1) { mockAttractionCommandHandler.handle(any<CreateAttractionCommand>()) }
            coVerify(exactly = 1) { mockCommuteCommandHandler.handle(any<CreateCommuteCommand>()) }
        }
}
