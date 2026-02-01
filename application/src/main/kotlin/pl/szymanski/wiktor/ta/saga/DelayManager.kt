package pl.szymanski.wiktor.ta.saga

import kotlin.random.Random

class DelayManager (
    retryCount: Int,
    initialDelayMs: Long = 100,
    private val maxDelayMs: Long = 10_000,
    private val backoffMultiplier: Double = 2.0,
    private val jitterFactor: Double = 0.1,
) {
    init {
        require(retryCount >= 0) { "Retry count must be non-negative" }
        repeat(retryCount) { getCurrentDelay() }
    }

    private var currentDelay = initialDelayMs

    private fun jitter() = currentDelay * jitterFactor * Random.nextDouble()

    fun getCurrentDelay(): Long {
        currentDelay = (currentDelay * backoffMultiplier).toLong().coerceAtMost(maxDelayMs)
        return (currentDelay + jitter()).toLong()
    }
}
