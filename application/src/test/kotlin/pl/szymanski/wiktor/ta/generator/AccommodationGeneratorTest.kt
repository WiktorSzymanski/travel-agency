package pl.szymanski.wiktor.ta.generator

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccommodationGeneratorTest {

    private val clock = Clock.fixed(Instant.parse("2026-02-02T18:00:00Z"), ZoneId.of("UTC"))

    @Test
    fun `should generate CreateAccommodationCommand and DateMetEvent from template`() {
        // given
        val generator = AccommodationGenerator(
            inAdvanceSeconds = 3600,
            creationWindowSeconds = 7200,
            clock = clock
        )
        val template = AccommodationTemplate(name = "Hotel", location = "Paris")

        // when
        val results = generator.generate(listOf(template))

        // then
        assertEquals(1, results.size)
        val result = results[0]
        val command = result.command
        val event = result.event
        
        assertEquals("Hotel", command.name)
        assertEquals("PARIS", command.location.name)
        assertEquals(command.accommodationId, event.accommodationId)
        assertEquals(command.rent.from, result.scheduleDate)
        
        val now = clock.instant().atZone(clock.zone).toLocalDateTime()
        assertTrue(command.rent.from.isAfter(now.plusSeconds(3599)))
        assertTrue(command.rent.from.isBefore(now.plusSeconds(3600 + 3601)))
        assertTrue(command.rent.till.isAfter(command.rent.from.minusSeconds(1)))
        assertTrue(command.rent.till.isBefore(now.plusSeconds(3600 + 7201)))
    }
}
