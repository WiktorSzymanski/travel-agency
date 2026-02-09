package pl.szymanski.wiktor.ta.generator

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttractionGeneratorTest {

    private val clock = Clock.fixed(Instant.parse("2026-02-02T18:00:00Z"), ZoneId.of("UTC"))

    @Test
    fun `should generate CreateAttractionCommand and DateMetEvent from template`() {
        // given
        val template = AttractionTemplate(name = "Eiffel Tower", location = "Paris", capacity = 100)
        val generator = AttractionGenerator(
            inAdvanceSeconds = 3600,
            creationWindowSeconds = 7200,
            listOf(template),
            clock = clock
        )

        // when
        val results = generator.generate()

        // then
        assertEquals(1, results.size)
        val result = results[0]
        val command = result.command
        val event = result.event
        
        assertEquals("Eiffel Tower", command.name)
        assertEquals("PARIS", command.location.name)
        assertEquals(100, command.capacity)
        assertEquals(command.attractionId, event.attractionId)
        assertEquals(command.date, result.scheduleDate)
        
        val now = clock.instant().atZone(clock.zone).toLocalDateTime()
        assertTrue(command.date.isAfter(now.plusSeconds(3599)))
        assertTrue(command.date.isBefore(now.plusSeconds(3600 + 7201)))
    }
}
