package pl.szymanski.wiktor.ta.infrastructure.config

import kotlinx.serialization.Serializable

@Serializable
data class OfferMakerSchedulerConfig(
    val intervalSeconds: Double,
    // min active time for entities
)
