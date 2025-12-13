package pl.szymanski.wiktor.ta.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import java.time.LocalDateTime

class LocationAndTimeDtoTest {
    @Test
    fun `fromDomain maps location name and time string`() {
        val lat = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.of(2024, 12, 31, 23, 59, 0))
        val dto = LocationAndTimeDto.fromDomain(lat)

        assertEquals("POZNAN", dto.location)
        assertEquals("2024-12-31T23:59", dto.time)
    }
}
