package pl.szymanski.wiktor.ta.eventhandler

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.ExpireAccommodationCommand
import pl.szymanski.wiktor.ta.command.ExpireAttractionCommand
import pl.szymanski.wiktor.ta.command.ExpireCommuteCommand
import pl.szymanski.wiktor.ta.commandhandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent
import java.util.UUID
import kotlin.test.Test

class DateMetEventHandleLogicTest {
    private val commandBus = mockk<CommandBus>(relaxed = true)
    private val logic = DateMetEventHandleLogic(commandBus)

    @Test
    fun `should handle CommuteDateMetEvent`() = runTest {
        val commuteId = CommuteId.generate()
        val correlationId = UUID.randomUUID()

        logic.onCommuteDateMetEvent(EventEnvelope(
            CommuteDateMetEvent(
                commuteId = commuteId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify {
            commandBus.dispatch<ExpireCommuteCommand, Commute>(
                ExpireCommuteCommand(
                    commuteId = commuteId,
                    correlationId = correlationId,
                )
            )
        }
    }

    @Test
    fun `should handle AccommodationDateMetEvent`() = runTest {
        val accommodationId = AccommodationId.generate()
        val correlationId = UUID.randomUUID()

        logic.onAccommodationDateMetEvent(EventEnvelope(
            AccommodationDateMetEvent(
                accommodationId = accommodationId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify {
            commandBus.dispatch<ExpireAccommodationCommand, Accommodation>(
                ExpireAccommodationCommand(
                    accommodationId = accommodationId,
                    correlationId = correlationId,
                )
            )
        }
    }

    @Test
    fun `should handle AttractionDateMetEvent`() = runTest {
        val attractionId = AttractionId.generate() as AttractionId.Present
        val correlationId = UUID.randomUUID()

        logic.onAttractionDateMetEvent(EventEnvelope(
            AttractionDateMetEvent(
                attractionId = attractionId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify {
            commandBus.dispatch<ExpireAttractionCommand, Attraction>(
                ExpireAttractionCommand(
                    attractionId = attractionId,
                    correlationId = correlationId,
                )
            )
        }
    }
}
