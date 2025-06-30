package generator

import java.time.LocalDateTime

fun LocalDateTime.isBetween(
    start: LocalDateTime,
    end: LocalDateTime,
): Boolean =
    (this.isAfter(start) || this.isEqual(start)) &&
        (this.isBefore(end) || this.isEqual(end))
