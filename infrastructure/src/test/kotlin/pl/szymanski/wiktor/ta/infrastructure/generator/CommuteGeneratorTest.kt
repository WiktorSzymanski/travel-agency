package pl.szymanski.wiktor.ta.infrastructure.generator

import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Seat
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class CommuteGeneratorTest {
    private val fixedTime = LocalDateTime.of(2025, 6, 30, 12, 0, 0)
    private val zone = ZoneId.of("UTC")
    private val fixedClock = Clock.fixed(fixedTime.atZone(zone).toInstant(), zone)

    private val plusSeconds = 5L

    private val commuteGenerator =
        CommuteGenerator(
            plusSeconds,
            listOf(
                CommuteTemplate(
                    "London to Paris",
                    "LONDON",
                    "PARIS",
                    listOf(Seat("A", "1"), Seat("A", "2")),
                ),
                CommuteTemplate(
                    "Paris to Berlin",
                    "PARIS",
                    "BERLIN",
                    listOf(Seat("B", "3"), Seat("B", "4")),
                ),
            ),
            fixedClock,
        )

    @Test
    fun `generate commute from yaml`() {
        val actual = commuteGenerator.generate()

        assertEquals(actual[0].name, "London to Paris")
        assertEquals(actual[0].departure.location, LocationEnum.LONDON)
        assertEquals(actual[0].arrival.location, LocationEnum.PARIS)
        assertEquals(actual[0].seats, listOf(Seat("A", "1"), Seat("A", "2")))

        assert(actual[0].departure.time.isBefore(fixedTime.plusSeconds(plusSeconds / 2)))
        assert(actual[0].departure.time.isAfter(fixedTime))
        assert(actual[0].departure.time.isBetween(fixedTime, fixedTime.plusSeconds(plusSeconds / 2)))
        assert(actual[0].arrival.time.isBetween(actual[0].departure.time, fixedTime.plusSeconds(plusSeconds)))

        assertEquals(actual[1].name, "Paris to Berlin")
        assertEquals(actual[1].departure.location, LocationEnum.PARIS)
        assertEquals(actual[1].arrival.location, LocationEnum.BERLIN)
        assertEquals(actual[1].seats, listOf(Seat("B", "3"), Seat("B", "4")))

        assert(actual[1].departure.time.isBefore(fixedTime.plusSeconds(plusSeconds / 2)))
        assert(actual[1].departure.time.isAfter(fixedTime))
        assert(actual[1].departure.time.isBetween(fixedTime, fixedTime.plusSeconds(plusSeconds / 2)))
        assert(actual[1].arrival.time.isBetween(actual[1].departure.time, fixedTime.plusSeconds(plusSeconds)))
    }
}
