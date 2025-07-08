package pl.szymanski.wiktor.ta.infrastructure.generator

import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class AccommodationGenerator(
    private val plusSeconds: Long,
    private val templates: List<AccommodationTemplate>,
    private val clock: Clock = Clock.systemDefaultZone(),
) : Generator<AccommodationTemplate, Accommodation> {
    override fun generate(): List<Accommodation> = templates.map { toDomainModel(it) }

    override fun toDomainModel(template: AccommodationTemplate): Accommodation {
        val fromTime =
            randomDateTimeBetween(
                LocalDateTime.now(clock),
                LocalDateTime.now(clock).plusSeconds(plusSeconds / 2),
            )

        val tillTime =
            randomDateTimeBetween(
                fromTime,
                LocalDateTime.now(clock).plusSeconds(plusSeconds),
            )

        return Accommodation(
            accommodationId = UUID.randomUUID(),
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
