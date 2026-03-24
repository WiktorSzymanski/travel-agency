# Transactional Outbox — Implementation Plan

## Previous Proposal (INVALID)

The previously proposed approach of using `ReactiveMongoTransactionManager` with `@Transactional` on `suspend` functions is **invalid** for this project because:

1. **The project uses `spring-boot-starter-data-mongodb` (imperative)**, not `spring-boot-starter-data-mongodb-reactive`. `ReactiveMongoTransactionManager` and `ReactiveMongoDatabaseFactory` are not on the classpath.

2. **Two separate MongoDB client stacks coexist:**
   - **Spring Data imperative** (`MongoRepository<T, ID>`) — used by aggregate repositories (`BookingDocumentMongoRepository`, `CommuteDocumentMongoRepository`, etc.)
   - **Raw MongoDB Kotlin Coroutine Driver** (`com.mongodb.kotlin.client.coroutine.MongoClient`) — used by `OutboxRepositoryMongo`, `SagaRepositoryImpl`, `DeadLetterQueueRepositoryMongo`

3. **`OutboxPortImpl.save()` calls across both stacks:**
   - `repository.save(entity, metadata)` → Spring Data imperative `MongoRepository.save()`
   - `outboxRepository.save(entry)` → raw coroutine driver `collection.insertOne()`

4. **These are two separate `MongoClient` instances.** Spring Data manages its own, and `MongoConfiguration` creates another. A single `@Transactional` cannot span two unrelated clients.

5. **MongoDB in `docker-compose.yaml` runs standalone** (no `--replSet`), so multi-document transactions are not supported at all.

## Corrected Approach

There are several valid options, ordered from cleanest to most pragmatic:

### Option A: Unify onto a single MongoDB client + `MongoTransactionManager` (cleanest)

Migrate `OutboxRepositoryMongo` to use Spring Data `MongoRepository` (like the aggregate repositories already do). This way all operations go through the same imperative `MongoClient` managed by Spring, and `MongoTransactionManager` can coordinate them.

**Steps:**
1. Create an `OutboxEntryDocument` data class with `@Document` and `@Id` annotations.
2. Create an `OutboxDocumentMongoRepository` extending `MongoRepository<OutboxEntryDocument, UUID>`.
3. Rewrite `OutboxRepositoryMongo` to delegate to `OutboxDocumentMongoRepository` instead of the raw coroutine driver.
4. Register a `MongoTransactionManager` bean:
   ```kotlin
   @Bean
   fun transactionManager(dbFactory: MongoDatabaseFactory): MongoTransactionManager {
       return MongoTransactionManager(dbFactory)
   }
   ```
5. Annotate `OutboxPortImpl.create()` and `OutboxPortImpl.save()` with `@Transactional`.
6. Since the `@Transactional` methods are `suspend` functions but the underlying `MongoRepository` is blocking, the `withContext(Dispatchers.IO)` calls in the aggregate repositories ensure they run on an IO thread. Spring's `MongoTransactionManager` will bind the transaction to the thread, and as long as both the aggregate save and the outbox save happen on the same thread within the same `@Transactional` boundary, the transaction will work.
7. Configure MongoDB as a replica set (required for multi-document transactions).

**Pros:**
- Standard Spring `@Transactional` — well-understood, well-tested.
- No manual session management.
- Single MongoDB client to maintain.

**Cons:**
- Requires migrating `OutboxRepositoryMongo` from raw driver to Spring Data.
- Requires MongoDB replica set.
- Need to be careful with `suspend` + imperative `@Transactional` threading — the transaction is thread-bound, so all transactional operations must happen on the same thread (no `withContext(Dispatchers.IO)` boundary crossing within the transaction).

### Option B: Use raw MongoDB sessions with the coroutine driver

Keep the raw coroutine driver but use a shared `ClientSession` to coordinate transactions manually.

**Steps:**
1. Migrate aggregate repositories to also use the raw coroutine driver (or vice versa — unify on one client).
2. Start a `ClientSession` with `startTransaction()`.
3. Pass the session to both `collection.insertOne(session, ...)` and `collection.replaceOne(session, ...)`.
4. Commit or abort the session.

**Pros:**
- Full control over transaction boundaries.
- Native coroutine support.

**Cons:**
- Manual transaction management (begin/commit/rollback).
- Must unify on a single `MongoClient` instance.
- More boilerplate.
- Still requires replica set.

### Option C: Write aggregate change + outbox entry in a single document (no transactions needed)

Instead of writing two separate documents (aggregate + outbox entry) in two collections, embed the outbox event inside the aggregate document itself (or use a single atomic `findAndModify` that also inserts the outbox entry into the same document).

**Steps:**
1. Add an `outboxEvents: List<OutboxEntryEmbedded>` field to each aggregate document.
2. When saving, include the outbox event in the same document write.
3. A separate poller reads unpublished events from aggregate documents.

**Pros:**
- No transactions needed — single document write is always atomic in MongoDB.
- Works on standalone MongoDB (no replica set required).

**Cons:**
- Outbox events are scattered across collections — poller becomes more complex.
- Document size increases.
- Less clean separation of concerns.

### Option D: Accept eventual consistency with idempotent retry (pragmatic)

Instead of making the operations transactional, make the system resilient to partial failures:

1. **Save the outbox entry FIRST**, then save the aggregate.
2. If the aggregate save fails, the outbox entry exists but the aggregate is unchanged — the outbox poller will publish the event, but the consumer must be idempotent (check current state before applying).
3. If the outbox save fails, nothing happens — the operation can be retried.

**Pros:**
- No transactions needed.
- No replica set required.
- Minimal code changes.

**Cons:**
- Requires all event consumers to be idempotent.
- Temporary inconsistency window.

## Recommendation

**Option A** is the cleanest if you are willing to:
- Migrate `OutboxRepositoryMongo` to Spring Data `MongoRepository`
- Set up MongoDB as a replica set

**Option D** is the most pragmatic if you want minimal changes and can ensure consumer idempotency (which you should have anyway in an event-driven system).

## MongoDB Replica Set Setup (Required for Options A and B)

Update `docker-compose.yaml`:

```yaml
services:
  mongodb:
    image: mongo:8.0
    container_name: mongodb
    restart: unless-stopped
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: root
      MONGO_INITDB_ROOT_PASSWORD: password
      MONGO_INITDB_DATABASE: travel_agency
    volumes:
      - mongodb_data:/data/db
    command: ["mongod", "--replSet", "rs0", "--bind_ip_all"]
    healthcheck:
      test: echo "try { rs.status() } catch (err) { rs.initiate() }" | mongosh -u root -p password --authenticationDatabase admin
      interval: 10s
      start_period: 30s
```

