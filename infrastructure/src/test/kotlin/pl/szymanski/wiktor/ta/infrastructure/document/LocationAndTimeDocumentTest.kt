package pl.szymanski.wiktor.ta.infrastructure.document

import kotlin.test.Test
import kotlin.test.assertEquals
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import java.time.LocalDateTime

class LocationAndTimeDocumentTest {
    @Test
    fun `fromDomain maps location name and time string`() {
        val lat = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.of(2024, 12, 31, 23, 59, 0))
        val document = LocationAndTimeDocument.fromDomain(lat)

        assertEquals("POZNAN", document.location)
        assertEquals("2024-12-31T23:59", document.time)
    }
}

