package pl.szymanski.wiktor.ta.infrastructure.generator

import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import java.time.Clock
import java.time.LocalDateTime

class AttractionGenerator(
    private val inAdvanceSeconds: Long,
    private val creationWindowSeconds: Long,
    private val templates: List<AttractionTemplate>,
    private val clock: Clock = Clock.systemDefaultZone(),
) : Generator<AttractionTemplate, Attraction> {
    override fun generate(): List<Attraction> = templates.map { toDomainModel(it) }

    override fun toDomainModel(template: AttractionTemplate): Attraction =
        Attraction(
            name = template.name,
            location = LocationEnum.valueOf(template.location.uppercase()),
            date =
                randomDateTimeBetween(
                    LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds),
                    LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds + creationWindowSeconds / 2),
                ),
            capacity = template.capacity,
        )
}
