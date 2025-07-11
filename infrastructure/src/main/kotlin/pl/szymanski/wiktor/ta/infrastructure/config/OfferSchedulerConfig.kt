package pl.szymanski.wiktor.ta.infrastructure.config

import kotlinx.serialization.Serializable

@Serializable
data class OfferSchedulerConfig(
    val intervalSeconds: Long,
    // min active time for entities
)
