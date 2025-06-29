package pl.szymanski.wiktor.ta.service

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.repository.BookingCoordinator
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class TravelOfferServiceTest {
    @MockK private lateinit var bookingCoordinator: BookingCoordinator

    private lateinit var travelOfferService: TravelOfferService

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        travelOfferService = TravelOfferService(bookingCoordinator)
    }

    @Test
    fun bookTravelOffer() =
        runTest {
            coEvery { bookingCoordinator.bookTravelOffer(any(), any(), any()) } answers {
                TravelOffer(
                    args[0] as UUID,
                    "travel_offer",
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                )
            }

            travelOfferService.bookTravelOffer(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Seat("A", "1"),
            )

            coVerify {
                bookingCoordinator.bookTravelOffer(any(), any(), any())
            }
        }

    @Test
    fun cancelTravelOfferBooking() =
        runTest {
            coEvery { bookingCoordinator.cancelTravelOfferBooking(any(), any()) } answers {
                TravelOffer(
                    args[0] as UUID,
                    "travel_offer",
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                )
            }

            travelOfferService.cancelTravelOfferBooking(
                UUID.randomUUID(),
                UUID.randomUUID(),
            )

            coVerify {
                bookingCoordinator.cancelTravelOfferBooking(any(), any())
            }
        }
}
