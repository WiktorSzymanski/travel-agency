//package pl.szymanski.wiktor.ta.eventHandlerLogic
//
//import io.mockk.coVerify
//import io.mockk.mockk
//import kotlinx.coroutines.test.runTest
//import pl.szymanski.wiktor.ta.EventEnvelope
//import pl.szymanski.wiktor.ta.Metadata
//import pl.szymanski.wiktor.ta.commands.accommodation.expire.ExpireAccommodationCommand
//import pl.szymanski.wiktor.ta.commands.accommodation.expire.ExpireAccommodationCommandHandler
//import pl.szymanski.wiktor.ta.commands.attraction.expire.ExpireAttractionCommand
//import pl.szymanski.wiktor.ta.commands.attraction.expire.ExpireAttractionCommandHandler
//import pl.szymanski.wiktor.ta.commands.commute.expire.ExpireCommuteCommand
//import pl.szymanski.wiktor.ta.commands.commute.expire.ExpireCommuteCommandHandler
//import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
//import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
//import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
//import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
//import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
//import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent
//import java.util.UUID
//import kotlin.test.Test
//
//class DateMetEventHandleLogicTest {
//    private val expireCommuteCommandHandler = mockk<ExpireCommuteCommandHandler>(relaxed = true)
//    private val expireAccommodationCommandHandler = mockk<ExpireAccommodationCommandHandler>(relaxed = true)
//    private val expireAttractionCommandHandler = mockk<ExpireAttractionCommandHandler>(relaxed = true)
//
//    @Test
//    fun `should handle CommuteDateMetEvent`() = runTest {
//        val commuteId = CommuteId.generate()
//        val correlationId = UUID.randomUUID()
//
//        onCommuteDateMetEvent(
//            expireCommuteCommandHandler,
//            EventEnvelope(
//                CommuteDateMetEvent(commuteId = commuteId),
//                Metadata(correlationId, 0)
//            )
//        )
//
//        coVerify {
//            expireCommuteCommandHandler.handle(
//                ExpireCommuteCommand(
//                    correlationId = correlationId,
//                    commuteId = commuteId,
//                )
//            )
//        }
//    }
//
//    @Test
//    fun `should handle AccommodationDateMetEvent`() = runTest {
//        val accommodationId = AccommodationId.generate()
//        val correlationId = UUID.randomUUID()
//
//        onAccommodationDateMetEvent(
//            expireAccommodationCommandHandler,
//            EventEnvelope(
//                AccommodationDateMetEvent(accommodationId = accommodationId),
//                Metadata(correlationId, 0)
//            )
//        )
//
//        coVerify {
//            expireAccommodationCommandHandler.handle(
//                ExpireAccommodationCommand(
//                    accommodationId = accommodationId,
//                    correlationId = correlationId,
//                )
//            )
//        }
//    }
//
//    @Test
//    fun `should handle AttractionDateMetEvent`() = runTest {
//        val attractionId = AttractionId.generate() as AttractionId.Present
//        val correlationId = UUID.randomUUID()
//
//        onAttractionDateMetEvent(
//            expireAttractionCommandHandler,
//            EventEnvelope(
//                AttractionDateMetEvent(attractionId = attractionId),
//                Metadata(correlationId, 0)
//            )
//        )
//
//        coVerify {
//            expireAttractionCommandHandler.handle(
//                ExpireAttractionCommand(
//                    attractionId = attractionId,
//                    correlationId = correlationId,
//                )
//            )
//        }
//    }
//}
