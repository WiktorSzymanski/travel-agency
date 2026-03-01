package pl.szymanski.wiktor.ta.infrastructure.document

import kotlin.test.Test
import kotlin.test.assertEquals
import pl.szymanski.wiktor.ta.domain.Rent
import java.time.LocalDateTime

class RentDocumentTest {
    @Test
    fun `fromDomain maps dates to strings`() {
        val from = LocalDateTime.of(2025, 1, 2, 3, 4, 5)
        val till = LocalDateTime.of(2025, 1, 5, 6, 7, 8)
        val rent = Rent(from = from, till = till)

        val document = RentDocument.fromDomain(rent)

        assertEquals(from.toString(), document.from)
        assertEquals(till.toString(), document.till)
    }
}

