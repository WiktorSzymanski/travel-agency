package pl.szymanski.wiktor.ta.offerMaker

import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import pl.szymanski.wiktor.ta.DummyCommandBus
import pl.szymanski.wiktor.ta.command.CreateTravelOfferCommand
import pl.szymanski.wiktor.ta.commandHandler.*
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.saga.DummyEventBus
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

    val offerMaker = spyk(
        OfferMaker(
            eventBus = eventBus,
            commandBus = commandBus,
            resourceService = resourceService,
            creationWindowSeconds = 3
        )
    )

    @Test
    fun `should add commuteEvent`() = runBlocking {
        val eventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 10L)

        eventBus.publish(eventEnvelope)

        val commutes = offerMaker.getActiveCommutes()

        assertEquals(1, commutes.size)
        assertEquals(eventEnvelope.event.commuteId, commutes[0].id)
        assertEquals(0, offerMaker.getActiveAttractions().size)
        assertEquals(0, offerMaker.getActiveAccommodations().size)
    }

    @Test
    fun `should add attractionEvent`() = runBlocking {
        val eventEnvelope = getAttractionCreatedEvent(LocationEnum.LONDON, 10L)
        eventBus.publish(eventEnvelope)

        val attractions = offerMaker.getActiveAttractions()

        assertEquals(1, attractions.size)
        assertEquals(eventEnvelope.event.attractionId, attractions[0].id)
        assertEquals(0, offerMaker.getActiveCommutes().size)
        assertEquals(0, offerMaker.getActiveAccommodations().size)
    }

    @Test
    fun `should add accommodationEvent`() = runBlocking {
        val eventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)

        eventBus.publish(eventEnvelope)

        val accommodations = offerMaker.getActiveAccommodations()

        assertEquals(1, accommodations.size)
        assertEquals(eventEnvelope.event.accommodationId, accommodations[0].id)
        assertEquals(0, offerMaker.getActiveAttractions().size)
        assertEquals(0, offerMaker.getActiveCommutes().size)
    }

    @Test
    fun `should create travel offer without attraction`() = runBlocking {
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 6L)

        eventBus.publish(accommodationEventEnvelope)
        eventBus.publish(commuteEventEnvelope)

        coVerify(exactly = 1) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }

    @Test
    fun `should create travel offer with and without attraction`() = runBlocking {
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 6L)
        val attractionEventEnvelope = getAttractionCreatedEvent(LocationEnum.LONDON, 10L)

        eventBus.publish(accommodationEventEnvelope)
        eventBus.publish(commuteEventEnvelope)
        eventBus.publish(attractionEventEnvelope)

        coVerify(exactly = 2) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }

    @Test
    fun `should not create travel offer when location mismatch`() = runBlocking {
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.PARIS, 5L)

        eventBus.publish(accommodationEventEnvelope)
        eventBus.publish(commuteEventEnvelope)

        coVerify(exactly = 0) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }

    @Test
    fun `should not create travel offer when commute arrives too early`() = runBlocking {
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 4L)

        eventBus.publish(accommodationEventEnvelope)
        eventBus.publish(commuteEventEnvelope)

        coVerify(exactly = 0) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }

    @Test
    fun `should not create travel offer when commute arrives after accommodation starts`() = runBlocking {
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 5L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 10L)

        eventBus.publish(accommodationEventEnvelope)
        eventBus.publish(commuteEventEnvelope)

        coVerify(exactly = 0) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }

    @Test
    fun `should not create travel offer with attraction when attraction is before accommodation starts`() = runBlocking {
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
    fun `should not create travel offer with attraction when attraction is after accommodation ends`() = runBlocking {
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
    fun `should not create duplicate offers`() = runBlocking {
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 8L)

        eventBus.publish(accommodationEventEnvelope)
        eventBus.publish(commuteEventEnvelope)
        eventBus.publish(commuteEventEnvelope)

        coVerify(exactly = 1) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }

    @Test
    fun `should not process events that already started`() = runBlocking {
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, -10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, -5L)

        eventBus.publish(accommodationEventEnvelope)
        eventBus.publish(commuteEventEnvelope)

        coVerify(exactly = 0) { travelOfferCommandHandler.handle(any()) }
        assertEquals(0, offerMaker.getActiveAccommodations().size)
        assertEquals(0, offerMaker.getActiveCommutes().size)
    }

    @Test
    fun `should clean up expired events from active lists`() = runBlocking {
        val expiredCommute = getCommuteCreatedEvent(LocationEnum.LONDON, 1L)
        eventBus.publish(expiredCommute)
        assertEquals(1, offerMaker.getActiveCommutes().size)

        kotlinx.coroutines.delay(2000)

        val newCommute = getCommuteCreatedEvent(LocationEnum.LONDON, 10L)
        eventBus.publish(newCommute)

        kotlinx.coroutines.delay(200)

        assertEquals(1, offerMaker.getActiveCommutes().size)
        assertEquals(newCommute.event.commuteId, offerMaker.getActiveCommutes()[0].id)
    }

    @Test
    fun `should create multiple offers with attraction when attraction matches multiple pairs`() = runBlocking {
        val accommodation1 = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val accommodation2 = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commute = getCommuteCreatedEvent(LocationEnum.LONDON, 8L)

        eventBus.publish(accommodation1)
        eventBus.publish(accommodation2)
        eventBus.publish(commute)

        val attraction = getAttractionCreatedEvent(LocationEnum.LONDON, 10L)
        eventBus.publish(attraction)

        kotlinx.coroutines.delay(200)

        coVerify(exactly = 4) { travelOfferCommandHandler.handle(ofType<CreateTravelOfferCommand>()) }
    }
}