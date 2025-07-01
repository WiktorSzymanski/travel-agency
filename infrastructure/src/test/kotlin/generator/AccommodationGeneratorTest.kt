package generator

import loader.YamlConfigLoader
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
    private val plusDays = 5L

    private val accommodationGenerator =
        AccommodationGenerator(
            YamlConfigLoader(),
            plusDays,
            fixedClock,
        )

    @Test
    fun `generate accommodation from yaml`() {
        val actual = accommodationGenerator.generate("src/test/resources/accommodations.yaml")

        assertEquals("Hotel", actual[0].name)
        assertEquals(LocationEnum.LONDON, actual[0].location)
        assert(actual[0].rent.from.isBetween(fixedTime, fixedTime.plusDays(plusDays / 2)))
        assert(actual[0].rent.till.isBetween(actual[0].rent.from, fixedTime.plusDays(plusDays)))

        assertEquals("Hostel", actual[1].name)
        assertEquals(LocationEnum.BERLIN, actual[1].location)
        assert(actual[1].rent.from.isBetween(fixedTime, fixedTime.plusDays(plusDays / 2)))
        assert(actual[1].rent.till.isBetween(actual[1].rent.from, fixedTime.plusDays(plusDays)))
    }
}
