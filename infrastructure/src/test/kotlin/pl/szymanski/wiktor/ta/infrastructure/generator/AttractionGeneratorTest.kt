package pl.szymanski.wiktor.ta.infrastructure.generator

import pl.szymanski.wiktor.ta.domain.LocationEnum
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class AttractionGeneratorTest {
    private val fixedTime = LocalDateTime.of(2025, 6, 30, 12, 0, 0)
    private val zone = ZoneId.of("UTC")
    private val fixedClock = Clock.fixed(fixedTime.atZone(zone).toInstant(), zone)
    private val inAdvanceSeconds = 5L
    private val creationWindowSeconds = 5L

    private val attractionGenerator =
        AttractionGenerator(
            inAdvanceSeconds,
            creationWindowSeconds,
            listOf(
                AttractionTemplate(
                    "Big Ben tour",
                    "LONDON",
                    10,
                ),
                AttractionTemplate(
                    "City tour by boat",
                    "BERLIN",
                    30,
                ),
            ),
            fixedClock,
        )

    @Test
    fun `generate attraction from yaml`() {
        val actual = attractionGenerator.generate()

        assertEquals("Big Ben tour", actual[0].name)
        assertEquals(LocationEnum.LONDON, actual[0].location)
        assertEquals(10, actual[0].capacity)
        assert(
            actual[0].date.isBetween(
                fixedTime.plusSeconds(inAdvanceSeconds),
                fixedTime.plusSeconds(
                    inAdvanceSeconds + creationWindowSeconds / 2,
                ),
            ),
        )

        assertEquals("City tour by boat", actual[1].name)
        assertEquals(LocationEnum.BERLIN, actual[1].location)
        assertEquals(30, actual[1].capacity)
        assert(
            actual[1].date.isBetween(
                fixedTime.plusSeconds(inAdvanceSeconds),
                fixedTime.plusSeconds(
                    inAdvanceSeconds + creationWindowSeconds / 2,
                ),
            ),
        )
    }
}
