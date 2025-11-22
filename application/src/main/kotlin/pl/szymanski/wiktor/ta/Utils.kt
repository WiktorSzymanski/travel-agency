package pl.szymanski.wiktor.ta

import kotlinx.coroutines.delay
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import java.time.LocalDateTime
import kotlin.random.Random
import kotlin.reflect.KClass

fun Attraction.timeMet(): Boolean = this.date.isBefore(LocalDateTime.now())

fun Accommodation.timeMet(): Boolean = this.rent.from.isBefore(LocalDateTime.now())

fun Commute.timeMet(): Boolean = this.departure.time.isBefore(LocalDateTime.now())

// suspend fun <T> withRetry(
//    maxRetries: Int,
//    onException: KClass<out Exception> = ConcurrentModificationException::class,
//    action: suspend () -> T,
// ): T {
//    var lastException: Throwable? = null
//    repeat(maxRetries) {
//        val res = runCatching { action() }
//        if (res.isSuccess) return res.getOrThrow()
//
//        lastException = res.exceptionOrNull()
//        if (!onException.isInstance(lastException)) throw lastException!!
//    }
//    throw lastException ?: IllegalStateException("No attempt made")
// }

suspend fun <T> withRetry(
    maxRetries: Int = 3,
    initialDelayMs: Long = 100,
    maxDelayMs: Long = 10000,
    backoffMultiplier: Double = 2.0,
    jitterFactor: Double = 0.1,
    onException: KClass<out Exception> = ConcurrentModificationException::class,
    action: suspend () -> T,
): T {
    var lastException: Throwable? = null
    var currentDelay = initialDelayMs

    repeat(maxRetries) { attempt ->
        val result = runCatching { action() }
        if (result.isSuccess) return result.getOrThrow()

        lastException = result.exceptionOrNull()
        if (!onException.isInstance(lastException)) throw lastException!!

        if (attempt > 25) {
            println("ERROR: ${lastException!!.message} \t Retrying attempt: $attempt")
        }

        if (attempt < maxRetries - 1) {
            val jitter = currentDelay * jitterFactor * Random.nextDouble()
            val delayWithJitter = currentDelay + jitter
            delay(delayWithJitter.toLong())

            currentDelay = (currentDelay * backoffMultiplier).toLong().coerceAtMost(maxDelayMs)
        }
    }
    // TODO: WHEN THIS HAPPENS OPERATION SHOULD FAIL IN CONTROLLED MANER
    throw lastException ?: IllegalStateException("No attempt made")
}
