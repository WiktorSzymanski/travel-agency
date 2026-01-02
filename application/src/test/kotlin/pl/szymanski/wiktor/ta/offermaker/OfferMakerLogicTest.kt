package pl.szymanski.wiktor.ta.offermaker

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import pl.szymanski.wiktor.ta.DummyCommandBus
import pl.szymanski.wiktor.ta.command.CreateTravelOfferCommand
import pl.szymanski.wiktor.ta.commandhandler.*
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import java.time.LocalDateTime
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OfferMakerLogicTest {
    private val travelOfferCommandHandler = mockk<TravelOfferCommandHandler>(relaxed = true)
    private val bookingCommandHandler = mockk<BookingCommandHandler>(relaxed = true)
    private val commuteCommandHandler = mockk<CommuteCommandHandler>(relaxed = true)
    private val attractionCommandHandler = mockk<AttractionCommandHandler>(relaxed = true)
    private val accommodationCommandHandler = mockk<AccommodationCommandHandler>(relaxed = true)

    private val commandBus = DummyCommandBus(
        travelOfferCommandHandler,
        bookingCommandHandler,
        commuteCommandHandler,
        attractionCommandHandler,
        accommodationCommandHandler
    )

    private val resourceRepository = mockk<ActiveResourceRepository>(relaxed = true)
    private val resourceService = ActiveResourceService(resourceRepository)

    private val logic = OfferMakerLogic(
        commandBus = commandBus,
        resourceService = resourceService,
        creationWindowSeconds = 3
    )

    @Test
    fun `should add Commute`() = runTest {
        val eventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 10L)
        logic.onCommuteCreatedEvent(eventEnvelope)
        verify(exactly = 1)  { resourceRepository.saveCommute(any()) }
    }

    @Test
    fun `should add Attraction`() = runTest {
        val eventEnvelope = getAttractionCreatedEvent(LocationEnum.LONDON, 10L)
        logic.onAttractionCreatedEvent(eventEnvelope)

        val attractions = resourceService.getAttractions()

        assertEquals(1, attractions.size)
        assertEquals(eventEnvelope.event.attractionId, attractions[0].id)
    }

    @Test
    fun `should add accommodationEvent`() = runTest {
        val eventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        logic.onAccommodationCreatedEvent(eventEnvelope)

        val accommodations = resourceService.getAccommodations()

        assertEquals(1, accommodations.size)
        assertEquals(eventEnvelope.event.accommodationId, accommodations[0].id)
    }

    @Test
    fun `should create travel offer without attraction`() = runTest {
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 6L)

        logic.onAccommodationCreatedEvent(accommodationEventEnvelope)
        logic.onCommuteCreatedEvent(commuteEventEnvelope)

        coVerify(exactly = 1) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }

    @Test
    fun `should create travel offer with and without attraction`() = runTest {
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 6L)
        val attractionEventEnvelope = getAttractionCreatedEvent(LocationEnum.LONDON, 10L)

        logic.onAccommodationCreatedEvent(accommodationEventEnvelope)
        logic.onCommuteCreatedEvent(commuteEventEnvelope)
        logic.onAttractionCreatedEvent(attractionEventEnvelope)

        coVerify(exactly = 2) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }

    @Test
    fun `should not process events that already started`() = runTest {
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, -10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, -5L)

        logic.onAccommodationCreatedEvent(accommodationEventEnvelope)
        logic.onCommuteCreatedEvent(commuteEventEnvelope)

        coVerify(exactly = 0) { travelOfferCommandHandler.handle(any()) }
        assertEquals(0, resourceService.getAccommodations().size)
        assertEquals(0, resourceService.getCommutes().size)
    }

    @Test
    fun `should clean up expired events from active lists`() = runTest {
        mockkStatic(LocalDateTime::class)
        try {
            val startTime = LocalDateTime.of(2025, 1, 1, 12, 0)
            every { LocalDateTime.now() } returns startTime

            val expiredCommute = getCommuteCreatedEvent(LocationEnum.LONDON, 1L)
            logic.onCommuteCreatedEvent(expiredCommute)

            assertEquals(1, resourceService.getCommutes().size)

            every { LocalDateTime.now() } returns startTime.plusSeconds(5)

            val newCommute = getCommuteCreatedEvent(LocationEnum.LONDON, 10L)
            logic.onCommuteCreatedEvent(newCommute)

            assertEquals(1, resourceService.getCommutes().size)
            assertEquals(newCommute.event.commuteId, resourceService.getCommutes()[0].id)
        } finally {
            unmockkStatic(LocalDateTime::class)
        }
    }
}
