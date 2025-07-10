package pl.szymanski.wiktor.ta.infrastructure.generator

import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import java.time.Clock
import java.time.LocalDateTime

class AccommodationGenerator(
    private val inAdvanceSeconds: Long,
    private val creationWindowSeconds: Long,
    private val templates: List<AccommodationTemplate>,
    private val clock: Clock = Clock.systemDefaultZone(),
) : Generator<AccommodationTemplate, Accommodation> {
    override fun generate(): List<Accommodation> = templates.map { toDomainModel(it) }

    override fun toDomainModel(template: AccommodationTemplate): Accommodation {
        val fromTime =
            randomDateTimeBetween(
                LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds),
                LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds + creationWindowSeconds / 2),
            )

        val tillTime =
            randomDateTimeBetween(
                fromTime,
                LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds + creationWindowSeconds),
            )

        return Accommodation(
            name = template.name,
            location = LocationEnum.valueOf(template.location.uppercase()),
            rent =
                Rent(
                    from = fromTime,
                    till = tillTime,
                ),
        )
    }
}
