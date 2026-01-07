//package pl.szymanski.wiktor.ta.offermaker
//
//import io.mockk.coVerify
//import io.mockk.mockk
//import kotlinx.coroutines.test.runTest
//import org.junit.jupiter.api.Assertions.assertEquals
//import pl.szymanski.wiktor.ta.CommandBus
//import pl.szymanski.wiktor.ta.domain.LocationEnum
//import kotlin.test.Test
//
//class OfferMakerLogicTest {
//    private val commandBus = mockk<CommandBus>(relaxed = true)
//
//    private val resourceService = ActiveResourceService(InMemoryActiveResourceRepository())
//
//    private val logic = OfferMakerLogic(
//        commandBus = commandBus,
//        resourceService = resourceService,
//        creationWindowSeconds = 3
//    )
//
//    @Test
//    fun `should add Commute`() = runTest {
//        val eventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 10L)
//        logic.onCommuteCreatedEvent(eventEnvelope)
//
//        val commutes = resourceService.getCommutes()
//
//        assertEquals(1, commutes.size)
//        assertEquals(eventEnvelope.event.commuteId, commutes[0].id)
//    }
//
//    @Test
//    fun `should add Attraction`() = runTest {
//        val eventEnvelope = getAttractionCreatedEvent(LocationEnum.LONDON, 10L)
//        logic.onAttractionCreatedEvent(eventEnvelope)
//
//        val attractions = resourceService.getAttractions()
//
//        assertEquals(1, attractions.size)
//        assertEquals(eventEnvelope.event.attractionId, attractions[0].id)
//    }
//
//    @Test
//    fun `should add Accommodation`() = runTest {
//        val eventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
//        logic.onAccommodationCreatedEvent(eventEnvelope)
//
//        val accommodations = resourceService.getAccommodations()
//
//        assertEquals(1, accommodations.size)
//        assertEquals(eventEnvelope.event.accommodationId, accommodations[0].id)
//    }
//
//    @Test
//    fun `should create travel offer without attraction`() = runTest {
//        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
//        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 6L)
//
//        logic.onAccommodationCreatedEvent(accommodationEventEnvelope)
//        logic.onCommuteCreatedEvent(commuteEventEnvelope)
//
//        coVerify(exactly = 1) { commandBus.dispatchAndForget(ofType<CreateTravelOfferCommand>()) }
//    }
//
//    @Test
//    fun `should create travel offer with and without attraction`() = runTest {
//        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
//        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 6L)
//        val attractionEventEnvelope = getAttractionCreatedEvent(LocationEnum.LONDON, 10L)
//
//        logic.onAccommodationCreatedEvent(accommodationEventEnvelope)
//        logic.onCommuteCreatedEvent(commuteEventEnvelope)
//        logic.onAttractionCreatedEvent(attractionEventEnvelope)
//
//        coVerify(exactly = 2) { commandBus.dispatchAndForget(ofType<CreateTravelOfferCommand>()) }
//    }
//
//    @Test
//    fun `should not process events that already started`() = runTest {
//        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, -10L)
//        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, -5L)
//
//        logic.onAccommodationCreatedEvent(accommodationEventEnvelope)
//        logic.onCommuteCreatedEvent(commuteEventEnvelope)
//
//        coVerify(exactly = 0) { commandBus.dispatchAndForget(any()) }
//        assertEquals(0, resourceService.getAccommodations().size)
//        assertEquals(0, resourceService.getCommutes().size)
//    }
//}
