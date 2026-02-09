package pl.szymanski.wiktor.ta.infrastructure.config

import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.BookingRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoAccommodationRepository
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoAttractionRepository
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoBookingRepository
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoCommuteRepository

fun provideAccommodationRepository(databaseProvider: DatabaseProvider): AccommodationRepository =
    MongoAccommodationRepository(databaseProvider)

fun provideAttractionRepository(databaseProvider: DatabaseProvider): AttractionRepository =
    MongoAttractionRepository(databaseProvider)

fun provideBookingRepository(databaseProvider: DatabaseProvider): BookingRepository =
    MongoBookingRepository(databaseProvider)

fun provideCommuteRepository(databaseProvider: DatabaseProvider): CommuteRepository =
    MongoCommuteRepository(databaseProvider)
