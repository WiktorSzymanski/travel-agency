package pl.szymanski.wiktor.ta.infrastructure.scheduler

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.infrastructure.config.SchedulerConfig
import pl.szymanski.wiktor.ta.infrastructure.generator.AccommodationTemplate
import pl.szymanski.wiktor.ta.infrastructure.generator.AttractionTemplate
import pl.szymanski.wiktor.ta.infrastructure.generator.CommuteTemplate
import pl.szymanski.wiktor.ta.infrastructure.generator.Generator
import java.time.LocalDateTime
import java.util.UUID

@ExperimentalCoroutinesApi
class SchedulerTest {
    private val mockAccommodationRepository = mockk<AccommodationRepository>(relaxed = true)
    private val mockAttractionRepository = mockk<AttractionRepository>(relaxed = true)
    private val mockCommuteRepository = mockk<CommuteRepository>(relaxed = true)

    // Mock generators
    private val mockAccommodationGenerator = mockk<Generator<Any, Accommodation>>()
    private val mockAttractionGenerator = mockk<Generator<Any, Attraction>>()
    private val mockCommuteGenerator = mockk<Generator<Any, Commute>>()

    private val scheduler = Scheduler

    @BeforeEach
    fun setup() {
        every { mockAccommodationGenerator.generate() } answers { listOf(createMockAccommodation()) }
        every { mockAttractionGenerator.generate() } answers { listOf(createMockAttraction()) }
        every { mockCommuteGenerator.generate() } answers { listOf(createMockCommute()) }

        coEvery { mockAccommodationRepository.save(any()) } coAnswers { firstArg() }
        coEvery { mockAttractionRepository.save(any()) } coAnswers { firstArg() }
        coEvery { mockCommuteRepository.save(any()) } coAnswers { firstArg() }

        scheduler.init(
            SchedulerConfig(
                5,
                5,
                5,
                listOf(
                    AccommodationTemplate(
                        "Test Accommodation",
                        "POZNAN",
                    ),
                ),
                listOf(
                    AttractionTemplate(
                        "Test Attraction",
                        "POZNAN",
                        10,
                    ),
                ),
                listOf(
                    CommuteTemplate(
                        "Test Commute",
                        "POZNAN",
                        "LONDON",
                        listOf(Seat("1", "A")),
                    ),
                ),
            ),
            mockAccommodationRepository,
            mockAttractionRepository,
            mockCommuteRepository,
        )
    }

    @AfterEach
    fun cleanup() {
        scheduler.stop()
        clearMocks(mockAccommodationRepository, mockAttractionRepository, mockCommuteRepository)
    }

    @Test
    fun `should generate and persist data when started`() =
        runTest {
            scheduler.start()

            delay(5000)

            coVerify(atLeast = 1) { mockAccommodationRepository.save(any()) }
            coVerify(atLeast = 1) { mockAttractionRepository.save(any()) }
            coVerify(atLeast = 1) { mockCommuteRepository.save(any()) }
        }

    @Test
    fun `should stop generating data when stopped`() =
        runTest {
            // Given
            scheduler.start()

            // When
            delay(1000)
            scheduler.stop()
            delay(5000)

            // Then
            coVerify(exactly = 1) { mockAccommodationRepository.save(any()) }
            coVerify(exactly = 1) { mockAttractionRepository.save(any()) }
            coVerify(exactly = 1) { mockCommuteRepository.save(any()) }
        }

    // Helper methods to create mock domain objects
    private fun createMockAccommodation(): Accommodation =
        Accommodation(
            _id = UUID.randomUUID(),
            name = "Test Accommodation",
            location = LocationEnum.POZNAN,
            rent =
                Rent(
                    from = LocalDateTime.now(),
                    till = LocalDateTime.now().plusSeconds(5),
                ),
        )

    private fun createMockAttraction(): Attraction =
        Attraction(
            _id = UUID.randomUUID(),
            name = "Test Attraction",
            location = LocationEnum.POZNAN,
            date = LocalDateTime.now(),
            capacity = 10,
        )

    private fun createMockCommute(): Commute =
        Commute(
            name = "Test Commute",
            departure =
                LocationAndTime(
                    LocationEnum.POZNAN,
                    LocalDateTime.now(),
                ),
            arrival =
                LocationAndTime(
                    LocationEnum.LONDON,
                    LocalDateTime.now().plusSeconds(3),
                ),
            seats = listOf(),
        )
}
