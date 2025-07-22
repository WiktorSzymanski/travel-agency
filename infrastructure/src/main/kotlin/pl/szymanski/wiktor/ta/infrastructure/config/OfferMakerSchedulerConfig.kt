package pl.szymanski.wiktor.ta.infrastructure.config

import kotlinx.serialization.Serializable

@Serializable
data class OfferMakerSchedulerConfig(
    val intervalSeconds: Long,
    // min active time for entities
)
