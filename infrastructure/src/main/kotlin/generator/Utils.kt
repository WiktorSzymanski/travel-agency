package generator

import java.time.Duration
import java.time.LocalDateTime
import kotlin.random.Random

const val TO_SECONDS_MULTIPLAYER = 1_000_000

fun randomDateTimeBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
): LocalDateTime {
    require(!endDate.isBefore(startDate)) {
        "endDate must not be before startDate"
    }

    val randomMillis =
        Random.nextLong(
            0,
            Duration.between(startDate, endDate).toMillis() + 1,
        )

    return startDate.plusNanos(randomMillis * TO_SECONDS_MULTIPLAYER)
}
