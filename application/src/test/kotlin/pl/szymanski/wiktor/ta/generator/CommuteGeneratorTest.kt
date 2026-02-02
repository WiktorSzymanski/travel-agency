package pl.szymanski.wiktor.ta.generator

import pl.szymanski.wiktor.ta.domain.Seat
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommuteGeneratorTest {

    private val clock = Clock.fixed(Instant.parse("2026-02-02T18:00:00Z"), ZoneId.of("UTC"))

    @Test
    fun `should generate CreateCommuteCommand and DateMetEvent from template`() {
        // given
        val generator = CommuteGenerator(
            inAdvanceSeconds = 3600,
            creationWindowSeconds = 7200,
            clock = clock
        )
        val seats = listOf(Seat.Picked("1", "A"), Seat.Picked("1", "B"))
        val template = CommuteTemplate(
            name = "Flight",
            departureLocation = "London",
            arrivalLocation = "Paris",
            seats = seats
        )

        // when
        val results = generator.generate(listOf(template))

        // then
        assertEquals(1, results.size)
        val result = results[0]
        val command = result.command
        val event = result.event
        
        assertEquals("Flight", command.name)
        assertEquals("LONDON", command.departure.location.name)
        assertEquals("PARIS", command.arrival.location.name)
        assertEquals(seats, command.seats)
        assertEquals(command.commuteId, event.commuteId)
        assertEquals(command.departure.time, result.scheduleDate)
        
        val now = clock.instant().atZone(clock.zone).toLocalDateTime()
        assertTrue(command.departure.time.isAfter(now.plusSeconds(3599)))
        assertTrue(command.departure.time.isBefore(now.plusSeconds(3600 + 3601)))
        assertTrue(command.arrival.time.isAfter(command.departure.time.plusHours(1).minusSeconds(1)))
        assertTrue(command.arrival.time.isBefore(now.plusSeconds(3600 + 7201)))
    }
}
