package pl.szymanski.wiktor.ta.infrastructure.generator

import pl.szymanski.wiktor.ta.domain.LocationEnum
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class AccommodationGeneratorTest {
    private val fixedTime = LocalDateTime.of(2025, 6, 30, 12, 0, 0)
    private val zone = ZoneId.of("UTC")
    private val fixedClock = Clock.fixed(fixedTime.atZone(zone).toInstant(), zone)
    private val inAdvanceSeconds = 5L
    private val creationWindowSeconds = 5L

    private val accommodationGenerator =
        AccommodationGenerator(
            inAdvanceSeconds,
            creationWindowSeconds,
            listOf(
                AccommodationTemplate(
                    "Hotel",
                    "LONDON",
                ),
                AccommodationTemplate(
                    "Hostel",
                    "BERLIN",
                ),
            ),
            fixedClock,
        )

    @Test
    fun `generate accommodation from yaml`() {
        val actual = accommodationGenerator.generate()

        assertEquals("Hotel", actual[0].name)
        assertEquals(LocationEnum.LONDON, actual[0].location)
        assert(
            actual[0].rent.from.isBetween(
                fixedTime.plusSeconds(inAdvanceSeconds),
                fixedTime.plusSeconds(
                    inAdvanceSeconds + creationWindowSeconds / 2,
                ),
            ),
        )
        assert(
            actual[0].rent.till.isBetween(
                actual[0].rent.from,
                fixedTime.plusSeconds(inAdvanceSeconds + creationWindowSeconds),
            ),
        )

        assertEquals("Hostel", actual[1].name)
        assertEquals(LocationEnum.BERLIN, actual[1].location)
        assert(
            actual[1].rent.from.isBetween(
                fixedTime.plusSeconds(inAdvanceSeconds),
                fixedTime.plusSeconds(
                    inAdvanceSeconds + creationWindowSeconds / 2,
                ),
            ),
        )
        assert(
            actual[1].rent.till.isBetween(
                actual[1].rent.from,
                fixedTime.plusSeconds(inAdvanceSeconds + creationWindowSeconds),
            ),
        )
    }
}
