package pl.szymanski.wiktor.ta.service

import java.util.UUID

class TravelOfferNotFoundException : RuntimeException {
    constructor(id: UUID) : super("TravelOffer with id '$id' was not found")
}

class CommuteNotFoundException : RuntimeException {
    constructor(id: UUID) : super("Commute with id '$id' was not found")
}

class AccommodationNotFoundException : RuntimeException {
    constructor(id: UUID) : super("Accommodation with id '$id' was not found")
}
