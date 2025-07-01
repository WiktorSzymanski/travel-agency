package generator

import loader.CommuteTemplate
import loader.YamlConfigLoader
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import java.time.Clock
import java.time.LocalDateTime

class CommuteGenerator(
    val loader: YamlConfigLoader,
    private val filePath: String,
    val plusSeconds: Long,
    val clock: Clock = Clock.systemDefaultZone(),
) : Generator<CommuteTemplate, Commute> {
    override fun generate(): List<Commute> {
        val templates = loader.loadConfig<CommuteTemplate>(filePath)
        return templates.map { toDomainModel(it) }
    }

    override fun toDomainModel(template: CommuteTemplate): Commute {
        val dTime =
            randomDateTimeBetween(
                LocalDateTime.now(clock),
                LocalDateTime.now(clock).plusSeconds(plusSeconds / 2),
            )

        val aTime =
            randomDateTimeBetween(
                dTime,
                LocalDateTime.now(clock).plusSeconds(plusSeconds),
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
