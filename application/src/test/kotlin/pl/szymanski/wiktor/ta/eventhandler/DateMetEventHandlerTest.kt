package pl.szymanski.wiktor.ta.eventhandler

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.ExpireAccommodationCommand
import pl.szymanski.wiktor.ta.command.ExpireAttractionCommand
import pl.szymanski.wiktor.ta.command.ExpireCommuteCommand
import pl.szymanski.wiktor.ta.commandhandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent
import pl.szymanski.wiktor.ta.saga.DummyEventBus
import java.util.UUID
import kotlin.test.Test

class DateMetEventHandlerTest {
    private val eventBus = DummyEventBus()
    private val attractionCommandHandler = mockk<AttractionCommandHandler>(relaxed = true)
    private val commuteCommandHandler = mockk<CommuteCommandHandler>(relaxed = true)
    private val accommodationCommandHandler = mockk<AccommodationCommandHandler>(relaxed = true)

    @Test
    fun `should handle CommuteDateMetEvent`() = runTest(UnconfinedTestDispatcher()) {
        DateMetEventHandler(
            eventBus,
            attractionCommandHandler,
            commuteCommandHandler,
            accommodationCommandHandler,
            backgroundScope
        )

        val commuteId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()

        eventBus.publish(EventEnvelope(
            CommuteDateMetEvent(
                commuteId = commuteId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify {
            commuteCommandHandler.handle(
                ExpireCommuteCommand(
                    commuteId = commuteId,
                    correlationId = correlationId,
                )
            )
        }
    }

    @Test
    fun `should handle AccommodationDateMetEvent`() = runTest(UnconfinedTestDispatcher()) {
        DateMetEventHandler(
            eventBus,
            attractionCommandHandler,
            commuteCommandHandler,
            accommodationCommandHandler,
            backgroundScope
        )

        val accommodationId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()

        eventBus.publish(EventEnvelope(
            AccommodationDateMetEvent(
                accommodationId = accommodationId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify {
            accommodationCommandHandler.handle(
                ExpireAccommodationCommand(
                    accommodationId = accommodationId,
                    correlationId = correlationId,
                )
            )
        }
    }

    @Test
    fun `should handle AttractionDateMetEvent`() = runTest(UnconfinedTestDispatcher()) {
        DateMetEventHandler(
            eventBus,
            attractionCommandHandler,
            commuteCommandHandler,
            accommodationCommandHandler,
            backgroundScope
        )

        val attractionId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()

        eventBus.publish(EventEnvelope(
            AttractionDateMetEvent(
                attractionId = attractionId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify {
            attractionCommandHandler.handle(
                ExpireAttractionCommand(
                    attractionId = attractionId,
                    correlationId = correlationId,
                )
            )
        }
    }
}
