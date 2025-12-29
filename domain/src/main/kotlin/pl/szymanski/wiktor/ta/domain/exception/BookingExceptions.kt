package pl.szymanski.wiktor.ta.domain.exception

open class BookingException(message: String) : DomainException(message)

class BookingProcessFailedException : BookingException("Booking can only be processed in NEW state")

class BookingCompleteFailedException : BookingException("Booking can only be completed in PROCESSING state")

class BookingCancelRequestFailedException : BookingException("Booking can only be cancelled in SUCCEEDED state")

class BookingCancelFailedException : BookingException("Booking can only be cancelled in PROCESSING or NEW state")

class BookingProcessCancellationFailedException : BookingException("Booking cancelation can only be process in CANCEL_REQUESTED state")

class BookingFailFailedException(originalMessage: String) : BookingException(
    "Booking can only be failed in PROCESSING or NEW state. Original message: $originalMessage",
)

class BookingFailCancellationFailedException(originalMessage: String) : BookingException(
    "Cancel Booking can only be failed in PROCESSING_CANCELLATION or CANCEL_REQUESTED state. Original message: $originalMessage",
)

class BookingEmptyEventListException : BookingException("Booking events list cannot be empty")

class BookingMissingCreatedEventException : BookingException("First event must be BookingCreatedEvent")
