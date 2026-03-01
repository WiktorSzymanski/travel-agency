package pl.szymanski.wiktor.ta.infrastructure.document

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.Rent
import java.time.LocalDateTime

@Serializable
data class RentDocument(
    val from: String,
    val till: String,
) {
    companion object {
        fun fromDomain(rent: Rent) =
            RentDocument(
                from = rent.from.toString(),
                till = rent.till.toString(),
            )
    }

    fun toDomain(): Rent =
        Rent(
            from = LocalDateTime.parse(from),
            till = LocalDateTime.parse(till)
        )
}

