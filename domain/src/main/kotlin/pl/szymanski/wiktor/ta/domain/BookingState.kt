package pl.szymanski.wiktor.ta.domain

enum class BookingState {
    NEW,
    PROCESSING,
    SUCCEEDED,
    CANCEL_REQUESTED,
    PROCESSING_CANCELLATION,
    CANCELED,
    FAILED,
}