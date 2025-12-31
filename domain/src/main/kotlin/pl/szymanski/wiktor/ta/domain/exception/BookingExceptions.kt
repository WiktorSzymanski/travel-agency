package pl.szymanski.wiktor.ta.domain.exception

import pl.szymanski.wiktor.ta.domain.aggregate.BookingId

open class BookingException(message: String) : DomainException(message)

class BookingProcessFailedException(bookingId: BookingId) : BookingException("Booking $bookingId can only be processed in NEW state")

class BookingCompleteFailedException(bookingId: BookingId) : BookingException("Booking $bookingId can only be completed in PROCESSING state")

class BookingCancelRequestFailedException(bookingId: BookingId) : BookingException("Booking $bookingId can only be cancelled in SUCCEEDED state")

class BookingCancelFailedException(bookingId: BookingId) : BookingException("Booking $bookingId can only be cancelled in PROCESSING or NEW state")

class BookingProcessCancellationFailedException(bookingId: BookingId) : BookingException("Booking $bookingId cancellation can only be process in CANCEL_REQUESTED state")

class BookingFailFailedException(bookingId: BookingId, originalMessage: String) : BookingException(
    "Booking $bookingId can only be failed in PROCESSING or NEW state. Original message: $originalMessage",
)

class BookingFailCancellationFailedException(bookingId: BookingId, originalMessage: String) : BookingException(
    "Cancel Booking $bookingId can only be failed in PROCESSING_CANCELLATION or CANCEL_REQUESTED state. Original message: $originalMessage",
)

class BookingEmptyEventListException : BookingException("Booking events list cannot be empty")

class BookingMissingCreatedEventException(bookingId: BookingId) : BookingException("First event must be BookingCreatedEvent ($bookingId)")
