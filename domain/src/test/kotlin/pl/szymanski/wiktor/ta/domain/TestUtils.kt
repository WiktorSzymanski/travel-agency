//package pl.szymanski.wiktor.ta.domain
//
//import pl.szymanski.wiktor.ta.domain.event.AccommodationBookFailedEvent
//import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
//import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCancelFailedEvent
//import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
//import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
//import pl.szymanski.wiktor.ta.domain.event.AccommodationExpireFailedEvent
//import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
//import pl.szymanski.wiktor.ta.domain.event.AttractionBookFailedEvent
//import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
//import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCancelFailedEvent
//import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
//import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
//import pl.szymanski.wiktor.ta.domain.event.AttractionExpireFailedEvent
//import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
//import pl.szymanski.wiktor.ta.domain.event.CommuteBookSeatFailedEvent
//import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
//import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
//import pl.szymanski.wiktor.ta.domain.event.CommuteCancelBookedSeatFailedEvent
//import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
//import pl.szymanski.wiktor.ta.domain.event.CommuteExpireFailedEvent
//import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
//import pl.szymanski.wiktor.ta.domain.event.Event
//import pl.szymanski.wiktor.ta.domain.event.FailedEvent
//import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookFailedEvent
//import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
//import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCancelFailedEvent
//import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
//import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
//import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpireFailedEvent
//import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
//import pl.szymanski.wiktor.ta.domain.event.TravelOfferMakeAvailableFailedEvent
//import pl.szymanski.wiktor.ta.domain.event.TravelOfferMakeUnavailableFailedEvent
//import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseCompleteFailedEvent
//import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
//import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCancelFailedEvent
//import pl.szymanski.wiktor.ta.domain.event.TravelOfferReserveFailedEvent
//import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
//import kotlin.test.assertEquals
//import kotlin.test.fail
//
//private fun assertFailedEventEquals(
//    expected: FailedEvent,
//    actual: FailedEvent,
//    message: String?,
//) {
//    assertEquals(expected.message, actual.message, message ?: "message differs")
//}
//
//private fun assertCommuteEventEquals(
//    expected: CommuteEvent,
//    actual: CommuteEvent,
//    message: String?,
//) {
//    assertEquals(expected.commuteId, actual.commuteId, message ?: "commuteId differs")
//
//    when (expected) {
//        is CommuteBookedEvent -> {
//            actual as CommuteBookedEvent
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//            assertEquals(expected.seat, actual.seat, message ?: "seat differs")
//        }
//        is CommuteBookingCanceledEvent -> {
//            actual as CommuteBookingCanceledEvent
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//            assertEquals(expected.seat, actual.seat, message ?: "seat differs")
//        }
//        is CommuteExpiredEvent -> {
//            // Only commuteId needs to be checked, which is already done above
//        }
//        is CommuteExpireFailedEvent -> {
//            assertFailedEventEquals(expected, actual as CommuteExpireFailedEvent, message)
//        }
//        is CommuteBookSeatFailedEvent -> {
//            actual as CommuteBookSeatFailedEvent
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//            assertEquals(expected.seat, actual.seat, message ?: "seat differs")
//            assertFailedEventEquals(expected, actual, message)
//        }
//        is CommuteCancelBookedSeatFailedEvent -> {
//            actual as CommuteCancelBookedSeatFailedEvent
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//            assertFailedEventEquals(expected, actual, message)
//        }
//        else -> throw IllegalArgumentException("Unsupported CommuteEvent type: ${expected::class}")
//    }
//}
//
//private fun assertAccommodationEventEquals(
//    expected: AccommodationEvent,
//    actual: AccommodationEvent,
//    message: String?,
//) {
//    assertEquals(expected.accommodationId, actual.accommodationId, message ?: "accommodationId differs")
//
//    when (expected) {
//        is AccommodationBookedEvent -> {
//            actual as AccommodationBookedEvent
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//        }
//        is AccommodationBookingCanceledEvent -> {
//            actual as AccommodationBookingCanceledEvent
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//        }
//        is AccommodationExpiredEvent -> {
//            // Only accommodationId needs to be checked, which is already done above
//        }
//        is AccommodationExpireFailedEvent -> {
//            assertFailedEventEquals(expected, actual as FailedEvent, message)
//        }
//        is AccommodationBookFailedEvent -> {
//            actual as AccommodationBookFailedEvent
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//            assertFailedEventEquals(expected, actual as FailedEvent, message)
//        }
//        is AccommodationBookingCancelFailedEvent -> {
//            actual as AccommodationBookingCancelFailedEvent
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//            assertFailedEventEquals(expected, actual as FailedEvent, message)
//        }
//        else -> throw IllegalArgumentException("Unsupported AccommodationEvent type: ${expected::class}")
//    }
//}
//
//private fun assertAttractionEventEquals(
//    expected: AttractionEvent,
//    actual: AttractionEvent,
//    message: String?,
//) {
//    assertEquals(expected.attractionId, actual.attractionId, message ?: "attractionId differs")
//
//    when (expected) {
//        is AttractionBookedEvent -> {
//            actual as AttractionBookedEvent
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//        }
//        is AttractionBookingCanceledEvent -> {
//            actual as AttractionBookingCanceledEvent
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//        }
//        is AttractionExpiredEvent -> {
//            // Only attractionId needs to be checked, which is already done above
//        }
//        is AttractionExpireFailedEvent -> {
//            assertFailedEventEquals(expected, actual as AttractionExpireFailedEvent, message)
//        }
//        is AttractionBookFailedEvent -> {
//            actual as AttractionBookFailedEvent
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//            assertFailedEventEquals(expected, actual, message)
//        }
//        is AttractionBookingCancelFailedEvent -> {
//            actual as AttractionBookingCancelFailedEvent
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//            assertFailedEventEquals(expected, actual, message)
//        }
//        else -> throw IllegalArgumentException("Unsupported AttractionEvent type: ${expected::class}")
//    }
//}
//
//private fun assertTravelOfferEventEquals(
//    expected: TravelOfferEvent,
//    actual: TravelOfferEvent,
//    message: String?,
//) {
//    assertEquals(expected.travelOfferId, actual.travelOfferId, message ?: "travelOfferId differs")
//
//    when (expected) {
//        is TravelOfferReservedEvent -> {
//            actual as TravelOfferReservedEvent
//            assertEquals(expected.accommodationId, actual.accommodationId, message ?: "accommodationId differs")
//            assertEquals(expected.commuteId, actual.commuteId, message ?: "commuteId differs")
//            assertEquals(expected.attractionId, actual.attractionId, message ?: "attractionId differs")
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//            assertEquals(expected.seat, actual.seat, message ?: "seat differs")
//        }
//        is TravelOfferReleaseEvent -> {
//            actual as TravelOfferReleaseEvent
//            assertEquals(expected.accommodationId, actual.accommodationId, message ?: "accommodationId differs")
//            assertEquals(expected.commuteId, actual.commuteId, message ?: "commuteId differs")
//            assertEquals(expected.attractionId, actual.attractionId, message ?: "attractionId differs")
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
////            assertEquals(expected.seat, actual.seat, message ?: "seat differs")
//        }
//        is TravelOfferBookedEvent -> {
//            actual as TravelOfferBookedEvent
//            assertEquals(expected.accommodationId, actual.accommodationId, message ?: "accommodationId differs")
//            assertEquals(expected.commuteId, actual.commuteId, message ?: "commuteId differs")
//            assertEquals(expected.attractionId, actual.attractionId, message ?: "attractionId differs")
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//            assertEquals(expected.seat, actual.seat, message ?: "seat differs")
//        }
//        is TravelOfferBookingCanceledEvent -> {
//            actual as TravelOfferBookingCanceledEvent
//            assertEquals(expected.accommodationId, actual.accommodationId, message ?: "accommodationId differs")
//            assertEquals(expected.commuteId, actual.commuteId, message ?: "commuteId differs")
//            assertEquals(expected.attractionId, actual.attractionId, message ?: "attractionId differs")
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
////            assertEquals(expected.seat, actual.seat, message ?: "seat differs")
//        }
//        is TravelOfferExpiredEvent -> {
//            // Only travelOfferId needs to be checked, which is already done above
//        }
//        is TravelOfferBookFailedEvent -> {
//            actual as TravelOfferBookFailedEvent
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//            assertFailedEventEquals(expected, actual, message)
//        }
//        is TravelOfferReserveFailedEvent -> {
//            actual as TravelOfferReserveFailedEvent
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//            assertFailedEventEquals(expected, actual, message)
//        }
//        is TravelOfferMakeUnavailableFailedEvent -> {
//            assertFailedEventEquals(expected, actual as TravelOfferMakeUnavailableFailedEvent, message)
//        }
//        is TravelOfferMakeAvailableFailedEvent -> {
//            assertFailedEventEquals(expected, actual as TravelOfferMakeAvailableFailedEvent, message)
//        }
//        is TravelOfferExpireFailedEvent -> {
//            assertFailedEventEquals(expected, actual as TravelOfferExpireFailedEvent, message)
//        }
//        is TravelOfferReservationCancelFailedEvent -> {
//            actual as TravelOfferReservationCancelFailedEvent
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//            assertFailedEventEquals(expected, actual, message)
//        }
//        is TravelOfferBookingCancelFailedEvent -> {
//            actual as TravelOfferBookingCancelFailedEvent
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//            assertFailedEventEquals(expected, actual, message)
//        }
//        is TravelOfferReleaseCompleteFailedEvent -> {
//            actual as TravelOfferReleaseCompleteFailedEvent
//            assertEquals(expected.bookingId, actual.bookingId, message ?: "bookingId differs")
//            assertFailedEventEquals(expected, actual, message)
//        }
//        else -> throw IllegalArgumentException("Unsupported TravelOfferEvent type: ${expected::class}")
//    }
//}
//
//fun assertEventEquals(
//    expected: Event,
//    actual: Event,
//    message: String? = null,
//) {
//    if (expected === actual) {
//        return
//    }
//
//    if (expected::class != actual::class) {
//        fail(message ?: "Events are not of the same type: expected ${expected::class}, actual ${actual::class}")
//    }
//
//    when (expected) {
//        is CommuteEvent -> assertCommuteEventEquals(expected, actual as CommuteEvent, message)
//        is AccommodationEvent -> assertAccommodationEventEquals(expected, actual as AccommodationEvent, message)
//        is AttractionEvent -> assertAttractionEventEquals(expected, actual as AttractionEvent, message)
//        is TravelOfferEvent -> assertTravelOfferEventEquals(expected, actual as TravelOfferEvent, message)
//        else -> fail(message ?: "Unsupported event type: ${expected::class}")
//    }
//}
