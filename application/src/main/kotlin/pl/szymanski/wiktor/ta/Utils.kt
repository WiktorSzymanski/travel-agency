package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute

fun Attraction.timeMet(): Boolean {
    try {
        this.expire()
    } catch (e: Exception) {
        return false
    }
    return true
}

fun Accommodation.timeMet(): Boolean {
    try {
        this.expire()
    } catch (e: Exception) {
        return false
    }
    return true
}

fun Commute.timeMet(): Boolean {
    try {
        this.cancel()
    } catch (e: Exception) {
        try {
            this.depart()
        } catch (e: Exception) {
            return false
        }
        return true
    }
    return true
}
