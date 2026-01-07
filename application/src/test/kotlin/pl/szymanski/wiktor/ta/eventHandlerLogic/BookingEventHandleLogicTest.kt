//package pl.szymanski.wiktor.ta.eventHandlerLogic
//
//import io.mockk.coVerify
//import io.mockk.mockk
//import kotlinx.coroutines.test.runTest
//import pl.szymanski.wiktor.ta.CommandBus
//import pl.szymanski.wiktor.ta.EventEnvelope
//import pl.szymanski.wiktor.ta.Metadata
//import pl.szymanski.wiktor.ta.command.ReleaseTravelOfferCommand
//import pl.szymanski.wiktor.ta.command.ReserveTravelOfferCommand
//import pl.szymanski.wiktor.ta.command.TravelOfferCommand
//import pl.szymanski.wiktor.ta.domain.Seat
//import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
//import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
//import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
//import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
//import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
//import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferId
//import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
//import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
//import java.util.UUID
//import kotlin.test.Test
//
//class BookingEventHandleLogicTest {
//    private val commandBus = mockk<CommandBus>(relaxed = true)
//    private val logic = BookingEventHandleLogic(commandBus)
//
//    @Test
//    fun `should handle BookingCreatedEvent`() = runTest {
//        val bookingId = BookingId.generate()
//        val userId = UUID.randomUUID()
//        val correlationId = UUID.randomUUID()
//        val seat = Seat.Any
//
//        logic.onCreatedEvent(EventEnvelope(
//            BookingCreatedEvent(
//                travelOffer = TravelOffer(
//                    CommuteId.generate(),
//                    AccommodationId.generate(),
//                    AttractionId.Empty,
//                ),
//                bookingId = bookingId,
//                userId = userId,
//                seat = seat,
//            ),
//            Metadata(
//                correlationId,
//                0
//            )
//        ))
//
//        coVerify {
//            commandBus.dispatch<TravelOfferCommand, TravelOffer>(
//                ReserveTravelOfferCommand(
//                    travelOfferId,
//                    correlationId,
//                    bookingId,
//                    seat,
//                )
//            )
//        }
//    }
//
//    @Test
//    fun `should handle BookingCancelRequestedEvent`() = runTest {
//        val travelOfferId = TravelOfferId.from(UUID.randomUUID())
//        val bookingId = BookingId.generate()
//        val correlationId = UUID.randomUUID()
//        val seat = Seat.Any
//
//        logic.onCancelRequestedEvent(EventEnvelope(
//            BookingCancelRequestedEvent(
//                travelOfferId = travelOfferId,
//                bookingId = bookingId,
//                seat = seat,
//            ),
//            Metadata(
//                correlationId,
//                0
//            )
//        ))
//
//        coVerify {
//            commandBus.dispatch<TravelOfferCommand, TravelOffer>(
//                ReleaseTravelOfferCommand(
//                    travelOfferId,
//                    correlationId,
//                    bookingId,
//                    seat,
//                )
//            )
//        }
//    }
//}
