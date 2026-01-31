package pl.szymanski.wiktor.ta.offermaker

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.queryrepository.AccommodationQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.AttractionQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.CommuteQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.TravelOfferQueryRepository
import kotlin.test.Test

class OfferMakerLogicTest {
    private val travelOfferQueryRepository = mockk<TravelOfferQueryRepository>(relaxed = true)
    private val commuteQueryRepository = mockk<CommuteQueryRepository>(relaxed = true)
    private val accommodationQueryRepository = mockk<AccommodationQueryRepository>(relaxed = true)
    private val attractionQueryRepository = mockk<AttractionQueryRepository>(relaxed = true)

    private val logic = OfferMakerLogic(
        travelOfferQueryRepository = travelOfferQueryRepository,
        commuteQueryRepository = commuteQueryRepository,
        accommodationQueryRepository = accommodationQueryRepository,
        attractionQueryRepository = attractionQueryRepository,
        creationWindowSeconds = 3
    )

    @Test
    fun `should add Commute`() = runTest {
        val eventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 10L)

        logic.onCommuteCreatedEvent(eventEnvelope)

        coVerify(exactly = 1) { accommodationQueryRepository.findByLocationAndDate(LocationEnum.LONDON, any()) }
        coVerify(exactly = 0) { travelOfferQueryRepository.save(any()) }
    }

    @Test
    fun `should add Attraction`() = runTest {
        val eventEnvelope = getAttractionCreatedEvent(LocationEnum.LONDON, 10L)

        logic.onAttractionCreatedEvent(eventEnvelope)

        coVerify(exactly = 1) { accommodationQueryRepository.findByLocationAndRentContainsDate(LocationEnum.LONDON, any()) }
        coVerify(exactly = 0) { commuteQueryRepository.findByLocationAndArrivalDate(any(), any()) }
        coVerify(exactly = 0) { travelOfferQueryRepository.save(any()) }
    }

    @Test
    fun `should add Accommodation`() = runTest {
        val eventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)

        logic.onAccommodationCreatedEvent(eventEnvelope)

        coVerify(exactly = 1) { commuteQueryRepository.findByLocationAndArrivalDate(LocationEnum.LONDON, any()) }
        coVerify(exactly = 0) { travelOfferQueryRepository.save(any()) }
    }

    @Test
    fun `should create travel offer without attraction`() = runTest {
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 6L)
        val accommodation = Accommodation.fromEvents(listOf(accommodationEventEnvelope.event))

        coEvery { accommodationQueryRepository.findByLocationAndDate(LocationEnum.LONDON, any()) } returns listOf(accommodation)
        coEvery { attractionQueryRepository.findByLocationAndDate(LocationEnum.LONDON, any()) } returns emptyList()

        logic.onCommuteCreatedEvent(commuteEventEnvelope)

        coVerify(exactly = 1) { travelOfferQueryRepository.save(any()) }
    }

    @Test
    fun `should create travel offer with and without attraction`() = runTest {
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, 10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, 6L)
        val attractionEventEnvelope = getAttractionCreatedEvent(LocationEnum.LONDON, 10L)
        val accommodation = Accommodation.fromEvents(listOf(accommodationEventEnvelope.event))
        val attraction = Attraction.fromEvents(listOf(attractionEventEnvelope.event))

        coEvery { accommodationQueryRepository.findByLocationAndDate(LocationEnum.LONDON, any()) } returns listOf(accommodation)
        coEvery { attractionQueryRepository.findByLocationAndDate(LocationEnum.LONDON, any()) } returns listOf(attraction)

        logic.onCommuteCreatedEvent(commuteEventEnvelope)

        coVerify(exactly = 2) { travelOfferQueryRepository.save(any()) }
    }

    @Test
    fun `should not process events that already started`() = runTest {
        val accommodationEventEnvelope = getAccommodationCreatedEvent(LocationEnum.LONDON, -10L)
        val commuteEventEnvelope = getCommuteCreatedEvent(LocationEnum.LONDON, -5L)

        logic.onAccommodationCreatedEvent(accommodationEventEnvelope)
        logic.onCommuteCreatedEvent(commuteEventEnvelope)

        coVerify(exactly = 0) { commuteQueryRepository.findByLocationAndArrivalDate(any(), any()) }
        coVerify(exactly = 0) { accommodationQueryRepository.findByLocationAndDate(any(), any()) }
        coVerify(exactly = 0) { travelOfferQueryRepository.save(any()) }
    }
}
