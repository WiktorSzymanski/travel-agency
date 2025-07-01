package generator

import loader.AttractionTemplate
import loader.YamlConfigLoader
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class AttractionGenerator(
    private val loader: YamlConfigLoader,
    private val filePath: String,
    private val plusSeconds: Long,
    private val clock: Clock = Clock.systemDefaultZone(),
) : Generator<AttractionTemplate, Attraction> {
    override fun generate(): List<Attraction> {
        val templates = loader.loadConfig<AttractionTemplate>(filePath)
        return templates.map { toDomainModel(it) }
    }

    override fun toDomainModel(template: AttractionTemplate): Attraction =
        Attraction(
            attractionId = UUID.randomUUID(),
            name = template.name,
            location = LocationEnum.valueOf(template.location.uppercase()),
            date =
                randomDateTimeBetween(
                    LocalDateTime.now(clock),
                    LocalDateTime.now(clock).plusSeconds(plusSeconds / 2),
                ),
            capacity = template.capacity,
        )
}
