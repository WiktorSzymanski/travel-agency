package pl.szymanski.wiktor.ta.domain

enum class BookingState {
    NEW,
    PROCESSING,
    SUCCEEDED,
    CANCELED,
    FAILED,
}