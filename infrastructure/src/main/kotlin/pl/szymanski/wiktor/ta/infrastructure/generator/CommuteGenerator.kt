package pl.szymanski.wiktor.ta.infrastructure.generator

import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import java.time.Clock
import java.time.LocalDateTime

class CommuteGenerator(
    val inAdvanceSeconds: Long,
    val creationWindowSeconds: Long,
    val templates: List<CommuteTemplate>,
    val clock: Clock = Clock.systemDefaultZone(),
) : Generator<CommuteTemplate, Commute> {
    override fun generate(): List<Commute> = templates.map { toDomainModel(it) }

    override fun toDomainModel(template: CommuteTemplate): Commute {
        val dTime =
            randomDateTimeBetween(
                LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds),
                LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds + creationWindowSeconds / 2),
            )

        val aTime =
            randomDateTimeBetween(
                dTime,
                LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds + creationWindowSeconds),
            )

        return Commute(
            name = template.name,
            departure =
                LocationAndTime(
                    LocationEnum.valueOf(template.departureLocation.uppercase()),
                    dTime,
                ),
            arrival =
                LocationAndTime(
                    LocationEnum.valueOf(template.arrivalLocation.uppercase()),
                    aTime,
                ),
            seats = template.seats,
        )
    }
}
