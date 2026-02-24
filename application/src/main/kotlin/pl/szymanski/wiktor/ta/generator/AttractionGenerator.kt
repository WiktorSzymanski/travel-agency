package pl.szymanski.wiktor.ta.generator

import pl.szymanski.wiktor.ta.commands.attraction.create.CreateAttractionCommand
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class AttractionGenerator(
    inAdvanceSeconds: Long,
    creationWindowSeconds: Long,
    templates: List<AttractionTemplate>,
    clock: Clock = Clock.systemDefaultZone(),
) : TemplateGenerator<AttractionTemplate, CreateAttractionCommand, AttractionDateMetEvent>(
    inAdvanceSeconds,
    creationWindowSeconds,
    templates,
    clock
) {
    override fun create(template: AttractionTemplate): GeneratedResult<CreateAttractionCommand, AttractionDateMetEvent> {
        val date = randomDateTimeBetween(
            LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds),
            LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds + creationWindowSeconds),
        )

        val id = AttractionId.generate()
        val command = CreateAttractionCommand(
            attractionId = id,
            correlationId = UUID.randomUUID(),
            name = template.name,
            location = LocationEnum.valueOf(template.location.uppercase()),
            date = date,
            capacity = template.capacity,
        )
        val event = AttractionDateMetEvent(attractionId = id, date = date)
        return GeneratedResult(command, event, date)
    }
}
