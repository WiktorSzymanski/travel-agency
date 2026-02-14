package pl.szymanski.wiktor.ta.infrastructure.dto

import kotlin.test.Test
import kotlin.test.assertEquals

class CommuteStatisticDtoTest {
    @Test
    fun `constructing CommuteStatisticDto holds given values`() {
        val arrivals = listOf(
            ArrivalLocationDto(location = "BERLIN", commutesNumber = 3, passengersNumber = 120),
            ArrivalLocationDto(location = "PARIS", commutesNumber = 2, passengersNumber = 80),
        )

        val dto = CommuteStatisticDto(
            time = "2025-12-12T10:15:30",
            totalCommuteCount = 5,
            totalBookingsCount = 200,
            arrivalLocations = arrivals,
        )

        assertEquals("2025-12-12T10:15:30", dto.time)
        assertEquals(5, dto.totalCommuteCount)
        assertEquals(200, dto.totalBookingsCount)
        assertEquals(arrivals, dto.arrivalLocations)
    }
}
