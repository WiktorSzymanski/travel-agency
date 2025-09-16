package pl.szymanski.wiktor.ta.infrastructure.scheduler

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class PersistedEvent (
    val id: UUID,
    val stream: String,
    val type: String,
    val correlationid: UUID,
    val timestamp: String,
    val revision: Long,
    val domainevent: String,
    val source: String = "/TravelAgencyApp",
    val specversion: String = "1.0",
    @JsonProperty("_etag") val etag: String? = null
)
