package pl.szymanski.wiktor.ta.infrastructure.generator

import pl.szymanski.wiktor.ta.command.CreateAttractionCommand
import pl.szymanski.wiktor.ta.domain.LocationEnum
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class AttractionGenerator(
    private val inAdvanceSeconds: Long,
    private val creationWindowSeconds: Long,
    private val templates: List<AttractionTemplate>,
    private val clock: Clock = Clock.systemDefaultZone(),
) : Generator<AttractionTemplate, CreateAttractionCommand> {
    override fun generate(): List<CreateAttractionCommand> = templates.map { toCommand(it) }

    override fun toCommand(template: AttractionTemplate): CreateAttractionCommand =
        CreateAttractionCommand(
            name = template.name,
            location = LocationEnum.valueOf(template.location.uppercase()),
            date =
                randomDateTimeBetween(
                    LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds),
                    LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds + creationWindowSeconds / 2),
                ),
            capacity = template.capacity,
            attractionId = UUID.randomUUID(),
            correlationId = UUID.randomUUID(),
        )
}
