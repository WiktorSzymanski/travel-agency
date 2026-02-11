package pl.szymanski.wiktor.ta.infrastructure

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.DateMetEventHandleLogic
import java.util.UUID
import kotlin.test.Test

class DateMetEventHandlerTest {
    private val eventBus = mockk<EventBus>()
    private val logic = mockk<DateMetEventHandleLogic>(relaxed = true)
    private val accommodationRepository = mockk<AccommodationRepository>(relaxed = true)
    private val commuteRepository = mockk<CommuteRepository>(relaxed = true)
    private val attractionRepository = mockk<AttractionRepository>(relaxed = true)
    private val handler = DateMetEventHandler(eventBus, accommodationRepository, commuteRepository, attractionRepository, logic)

    @Test
    fun `should subscribe and delegate events to logic`() = runTest {
        val callbackSlot = slot<suspend (EventEnvelope<CommuteDateMetEvent>) -> Unit>()
        coEvery { 
            eventBus.subscribe(CommuteDateMetEvent::class, capture(callbackSlot)) 
        } returns Unit
        coEvery {
            eventBus.subscribe(pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent::class, any<suspend (EventEnvelope<pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent>) -> Unit>())
        } returns Unit
        coEvery {
            eventBus.subscribe(pl.szymanski.wiktor.ta.event.AttractionDateMetEvent::class, any<suspend (EventEnvelope<pl.szymanski.wiktor.ta.event.AttractionDateMetEvent>) -> Unit>())
        } returns Unit

        handler.subscribe()

        val envelope = EventEnvelope(
            CommuteDateMetEvent(commuteId = CommuteId.generate()),
            Metadata(UUID.randomUUID(), 0)
        )
        callbackSlot.captured.invoke(envelope)

        coVerify { logic.onCommuteDateMetEvent(envelope) }
    }
}
