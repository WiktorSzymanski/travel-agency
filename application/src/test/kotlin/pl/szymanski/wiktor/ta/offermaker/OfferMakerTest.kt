package pl.szymanski.wiktor.ta.offermaker

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import pl.szymanski.wiktor.ta.DummyCommandBus
import pl.szymanski.wiktor.ta.command.CreateTravelOfferCommand
import pl.szymanski.wiktor.ta.commandhandler.*
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.saga.DummyEventBus
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class OfferMakerTest {
    private val eventBus = DummyEventBus()
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

    private val resourceRepository = InMemoryActiveResourceRepository()
    private val resourceService = ActiveResourceService(resourceRepository)

    private fun createOfferMaker(scope: kotlinx.coroutines.CoroutineScope) =
        OfferMaker(
            eventBus = eventBus,
            commandBus = commandBus,
            resourceService = resourceService,
            creationWindowSeconds = 3,
            scope = scope
        )

    @Test
    fun `should add Commute`() = runTest(UnconfinedTestDispatcher()) {
        createOfferMaker(backgroundScope)
        val eventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 10L)

        eventBus.publish(eventEnvelope)

        val commutes = resourceService.getCommutes()

        assertEquals(1, commutes.size)
        assertEquals(eventEnvelope.event.commuteId, commutes[0].id)
        assertEquals(0, resourceService.getAttractions().size)
        assertEquals(0, resourceService.getAccommodations().size)
    }

    @Test
    fun `should add Attraction`() = runTest(UnconfinedTestDispatcher()) {
        createOfferMaker(backgroundScope)
        val eventEnvelope = getAttractionCreatedEvent(LocationEnum.LONDON, 10L)
        eventBus.publish(eventEnvelope)

        val attractions = resourceService.getAttractions()

        assertEquals(1, attractions.size)
        assertEquals(eventEnvelope.event.attractionId, attractions[0].id)
        assertEquals(0, resourceService.getCommutes().size)
        assertEquals(0, resourceService.getAccommodations().size)
    }

    @Test
    fun `should add accommodationEvent`() = runTest(UnconfinedTestDispatcher()) {
        createOfferMaker(backgroundScope)
        val eventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)

        eventBus.publish(eventEnvelope)

        val accommodations = resourceService.getAccommodations()

        assertEquals(1, accommodations.size)
        assertEquals(eventEnvelope.event.accommodationId, accommodations[0].id)
        assertEquals(0, resourceService.getAttractions().size)
        assertEquals(0, resourceService.getCommutes().size)
    }

    @Test
    fun `should create travel offer without attraction`() = runTest(UnconfinedTestDispatcher()) {
        createOfferMaker(backgroundScope)
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 6L)

        eventBus.publish(accommodationEventEnvelope)
        eventBus.publish(commuteEventEnvelope)

        coVerify(exactly = 1) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }

    @Test
    fun `should create travel offer with and without attraction`() = runTest(UnconfinedTestDispatcher()) {
        createOfferMaker(backgroundScope)
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 6L)
        val attractionEventEnvelope = getAttractionCreatedEvent(LocationEnum.LONDON, 10L)

        eventBus.publish(accommodationEventEnvelope)
        eventBus.publish(commuteEventEnvelope)
        eventBus.publish(attractionEventEnvelope)

        coVerify(exactly = 2) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }

    @Test
    fun `should not create travel offer when location mismatch`() = runTest(UnconfinedTestDispatcher()) {
        createOfferMaker(backgroundScope)
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.PARIS, 5L)

        eventBus.publish(accommodationEventEnvelope)
        eventBus.publish(commuteEventEnvelope)

        coVerify(exactly = 0) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }

    @Test
    fun `should not create travel offer when commute arrives too early`() = runTest(UnconfinedTestDispatcher()) {
        createOfferMaker(backgroundScope)
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 4L)

        eventBus.publish(accommodationEventEnvelope)
        eventBus.publish(commuteEventEnvelope)

        coVerify(exactly = 0) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }

    @Test
    fun `should not create travel offer when commute arrives after accommodation starts`() = runTest(UnconfinedTestDispatcher()) {
        createOfferMaker(backgroundScope)
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 5L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 10L)

        eventBus.publish(accommodationEventEnvelope)
        eventBus.publish(commuteEventEnvelope)

        coVerify(exactly = 0) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }

    @Test
    fun `should not create travel offer with attraction when attraction is before accommodation starts`() = runTest(UnconfinedTestDispatcher()) {
        createOfferMaker(backgroundScope)
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 8L)
        val attractionEventEnvelope = getAttractionCreatedEvent(LocationEnum.LONDON, 5L)

        eventBus.publish(accommodationEventEnvelope)
        eventBus.publish(commuteEventEnvelope)
        eventBus.publish(attractionEventEnvelope)

        // Should create 1 offer (without attraction)
        coVerify(exactly = 1) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }

    @Test
    fun `should not create travel offer with attraction when attraction is after accommodation ends`() = runTest(UnconfinedTestDispatcher()) {
        createOfferMaker(backgroundScope)
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 8L)
        val attractionEventEnvelope = getAttractionCreatedEvent(LocationEnum.LONDON, 15L)

        eventBus.publish(accommodationEventEnvelope)
        eventBus.publish(commuteEventEnvelope)
        eventBus.publish(attractionEventEnvelope)

        // Should create 1 offer (without attraction)
        coVerify(exactly = 1) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }

    @Test
    fun `should not create duplicate offers`() = runTest(UnconfinedTestDispatcher()) {
        createOfferMaker(backgroundScope)
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 8L)

        eventBus.publish(accommodationEventEnvelope)
        eventBus.publish(commuteEventEnvelope)
        eventBus.publish(commuteEventEnvelope)

        coVerify(exactly = 1) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }

    @Test
    fun `should not process events that already started`() = runTest(UnconfinedTestDispatcher()) {
        createOfferMaker(backgroundScope)
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, -10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, -5L)

        eventBus.publish(accommodationEventEnvelope)
        eventBus.publish(commuteEventEnvelope)

        coVerify(exactly = 0) { travelOfferCommandHandler.handle(any()) }
        assertEquals(0, resourceService.getAccommodations().size)
        assertEquals(0, resourceService.getCommutes().size)
    }

    @Test
    fun `should clean up expired events from active lists`() = runTest(UnconfinedTestDispatcher()) {
        mockkStatic(LocalDateTime::class)
        try {
            val startTime = LocalDateTime.of(2025, 1, 1, 12, 0)
            every { LocalDateTime.now() } returns startTime

            createOfferMaker(backgroundScope)
            val expiredCommute = getCommuteCreatedEvent(LocationEnum.LONDON, 1L)
            eventBus.publish(expiredCommute)

            assertEquals(1, resourceService.getCommutes().size)

            every { LocalDateTime.now() } returns startTime.plusSeconds(5)

            val newCommute = getCommuteCreatedEvent(LocationEnum.LONDON, 10L)
            eventBus.publish(newCommute)

            assertEquals(1, resourceService.getCommutes().size)
            assertEquals(newCommute.event.commuteId, resourceService.getCommutes()[0].id)
        } finally {
            unmockkStatic(LocalDateTime::class)
        }
    }

    @Test
    fun `should create multiple offers with attraction when attraction matches multiple pairs`() = runTest(UnconfinedTestDispatcher()) {
        createOfferMaker(backgroundScope)
        val accommodation1 = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val accommodation2 = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commute = getCommuteCreatedEvent(LocationEnum.LONDON, 8L)

        eventBus.publish(accommodation1)
        eventBus.publish(accommodation2)
        eventBus.publish(commute)

        val attraction = getAttractionCreatedEvent(LocationEnum.LONDON, 10L)
        eventBus.publish(attraction)

        coVerify(exactly = 4) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }
}