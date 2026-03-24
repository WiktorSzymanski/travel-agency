# Plan: Fix Kafka Event Handlers Silently Swallowing Exceptions

## Problem

When a `BookingCancelSagaCompletedEvent` is published to Kafka and handled by `SagaEventHandler`, if the downstream command handler throws (e.g., `OptimisticLockingFailureException` from MongoDB `@Version` conflict), the exception is caught by `runCatching`, logged, and the Kafka offset is committed. The event is **never retried**, and the aggregate state change is **lost forever**.

This affects all Kafka consumers in the application, not just bookings.

## Root Cause

### 1. Kafka event handlers silently swallow all exceptions

Every Kafka `@KafkaHandler` uses `runCatching { ... }.onFailure { log.error(...) }`:
- The exception is logged but the Kafka offset is **committed** (message consumed)
- The event is **never retried**
- The aggregate state change is **lost forever**

### 2. Non-transactional save + outbox (`OutboxPortImpl`)

In `OutboxPortImpl.save()`:
```kotlin
repository.save(entity, metadata)  // Step 1 — can throw OptimisticLockingFailureException
entries.forEach { entry -> outboxRepository.save(entry) }  // Step 2
```
These two operations are not transactional. If Step 1 succeeds but Step 2 fails, the aggregate is changed but the event is lost. Conversely, they can diverge in other failure scenarios.

## All Affected Places

### Kafka Consumers (silent exception swallowing = lost events)

**1. `SagaEventHandler.onSagaEvent()`** — `infrastructure/.../eventHandler/SagaEventHandler.kt` line 32
- `BookingSagaStartedEvent` → ProcessBooking (Booking state)
- `BookingSagaCompletedEvent` → CompleteBooking (Booking state)
- `BookingSagaFailedEvent` → FailBooking (Booking state)
- `BookingCancelSagaStartedEvent` → ProcessCancelBooking (Booking state)
- `BookingCancelSagaCompletedEvent` → CancelBooking (Booking state) ← **the observed bug**
- `BookingCancelSagaFailedEvent` → FailCancelBooking (Booking state)

**2. `BookingEventHandler.onBookingEvent()`** — `infrastructure/.../eventHandler/BookingEventHandler.kt` line 24
- `BookingCreatedEvent` → starts BookingSaga (saga execution lost)
- `BookingCancelRequestedEvent` → starts CancelBookingSaga (saga execution lost)

**3. `DateMetEventHandler.onDateMetEvent()`** — `infrastructure/.../eventHandler/DateMetEventHandler.kt` line 31
- `AccommodationDateMetEvent` → ExpireAccommodation (Accommodation state)
- `CommuteDateMetEvent` → ExpireCommute (Commute state)
- `AttractionDateMetEvent` → ExpireAttraction (Attraction state)

### Non-transactional outbox saves (aggregate ↔ outbox inconsistency)

**4. `OutboxPortImpl.save()`** — `infrastructure/.../outbox/OutboxPortImpl.kt` lines 25-38
- If `repository.save()` succeeds but `outboxRepository.save()` fails → aggregate changed but event lost
- Used by **every** command handler via the outbox pattern — affects all aggregates: Booking, Accommodation, Attraction, Commute

**5. `OutboxPortImpl.create()`** — `infrastructure/.../outbox/OutboxPortImpl.kt` lines 17-23
- Same non-transactional issue for entity creation

## Summary Table

| Problem | Location | Impact |
|---|---|---|
| `runCatching` swallows exceptions | All 3 Kafka handlers | Events silently lost on any failure |
| No retry on `OptimisticLockingFailureException` | `SagaEventHandler`, `BookingEventHandler`, `DateMetEventHandler` | Concurrent writes cause permanent state inconsistency |
| Non-transactional save + outbox | `OutboxPortImpl.save()` and `.create()` | Aggregate and outbox can diverge |

## Recommended Fix

### Phase 1: Stop swallowing exceptions in Kafka handlers

Remove `runCatching` from all three Kafka handlers. Let exceptions propagate so Spring Kafka can retry the message. Configure a `DefaultErrorHandler` with:
- Exponential backoff retry for transient failures (`OptimisticLockingFailureException`, `ConcurrentModificationException`)
- Dead-letter topic (DLT) for messages that exhaust retries
- Non-retryable exception classification for domain invariant violations (`IllegalArgumentException`, `IllegalStateException`) that should go directly to DLT

### Phase 2: Address non-transactional outbox

Options:
- Use MongoDB multi-document transactions to make `repository.save()` + `outboxRepository.save()` atomic
- Alternatively, use the "listen to yourself" pattern where the aggregate change event is the source of truth and projections are rebuilt from events

