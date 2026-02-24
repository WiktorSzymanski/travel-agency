# Universal OutboxPooler Design

## Overview

The Universal OutboxPooler is a comprehensive, flexible solution for reliable event publishing using the Outbox Pattern. It's designed to work with different types of outbox implementations while providing consistent functionality for event processing, retry logic, and failure handling.

## Key Components

### 1. Core Interfaces

#### `OutboxEntry`
Base interface for all outbox entries, providing common fields:
- `eventId`: Unique identifier for the event
- `eventEnvelope`: The wrapped event with metadata
- `published`: Whether the event has been successfully published
- `publishedAt`: Timestamp when the event was published
- `createdAt`: When the entry was created
- `attempts`: Number of publishing attempts
- `lastAttemptAt`: When the last attempt was made
- `nextAttemptAt`: When the next retry should happen
- `error`: Last error message if any

#### `OutboxPort<T>`
Generic port interface that any outbox implementation can implement:
- `saveEntry()`: Save a new outbox entry
- `getPendingEvents()`: Get unpublished events
- `markAsPublished()`: Mark event as successfully published
- `markAsFailed()`: Mark event as failed with retry information
- `incrementAttempts()`: Increment attempt counter
- `deleteOldPublishedEvents()`: Cleanup old published events

#### `UniversalOutboxPort`
Extended port with additional universal functionality:
- `saveStateWithEvent()`: Save state with event (context-aware)
- `getPendingEventsByContext()`: Get events by context type/ID
- `getFailedEvents()`: Get events that need retry

### 2. Universal OutboxPooler

#### `UniversalOutboxPooler<T>`
The main pooler class that processes outbox entries:
- **Configurable**: Batch size, polling interval, retry logic, cleanup
- **Resilient**: Exponential backoff, max retries, error handling
- **Extensible**: Works with any OutboxPort implementation
- **Observable**: Comprehensive logging and monitoring

#### `OutboxPoolerConfig`
Configuration class for customizing pooler behavior:
```kotlin
data class OutboxPoolerConfig(
    val batchSize: Int = 100,           // Events per batch
    val pollingIntervalMs: Long = 1000, // How often to check for events
    val maxRetries: Int = 3,            // Maximum retry attempts
    val retryDelayMs: Long = 5000,      // Base delay between retries
    val exponentialBackoff: Boolean = true, // Use exponential backoff
    val cleanupIntervalMs: Long = 3600000,  // Cleanup frequency
    val cleanupAfterDays: Long = 7      // Delete published events after X days
)
```

### 3. Event Publishers

#### `EventPublisher`
Interface for different event publishing strategies:
- `EventBusPublisher`: Default publisher using your EventBus
- `LoggingEventPublisher`: Wraps another publisher with logging
- `FilteringEventPublisher`: Filters events based on criteria
- `TransformingEventPublisher`: Transforms events before publishing
- `CompositeEventPublisher`: Tries multiple publishers in sequence

### 4. Factory and Management

#### `OutboxPoolerFactory`
Factory for creating and managing poolers:
- Creates specialized poolers (saga, domain, etc.)
- Starts/stops poolers with tracking
- Health monitoring
- Graceful shutdown

## Architecture Benefits

### 1. **Universal Design**
- Works with any outbox implementation through ports
- Consistent interface regardless of underlying storage
- Easy to add new outbox types

### 2. **Flexible Event Publishing**
- Pluggable event publishers
- Filtering, logging, transformation support
- Fallback mechanisms with composite publisher

### 3. **Robust Retry Logic**
- Configurable retry attempts and delays
- Exponential backoff to prevent overwhelming downstream services
- Failed event tracking and recovery

### 4. **Monitoring and Observability**
- Comprehensive logging at different levels
- Health checks and status monitoring
- Cleanup of old events to prevent storage bloat

### 5. **Type Safety**
- Generic design maintains type safety
- Compile-time checking for outbox entry types
- Proper serialization support

## Usage Examples

### Basic Setup
```kotlin
// Setup with existing SagaOutboxPort
val sagaOutboxPort: SagaOutboxPort = // your implementation
OutboxPoolerSetup.setupSagaOutboxPooler(sagaOutboxPort)
```

### Advanced Setup with Custom Publisher
```kotlin
val outboxPort: UniversalOutboxPort = // your implementation

// Create custom publisher with logging and filtering
val eventPublisher = FilteringEventPublisher(
    delegate = LoggingEventPublisher(EventBusPublisher()),
    filter = { entry -> entry.contextType == "SAGA" }
)

val pooler = OutboxPoolerFactory.createGenericPooler(
    outboxPort = outboxPort,
    eventPublisher = eventPublisher,
    config = OutboxPoolerConfig(
        batchSize = 50,
        pollingIntervalMs = 2000,
        maxRetries = 5
    )
)

OutboxPoolerFactory.startPooler("custom-pooler", pooler)
```

### Multiple Poolers for Different Contexts
```kotlin
// Separate poolers for different event types
OutboxPoolerSetup.setupMultiplePoolers(
    sagaOutboxPort = sagaOutboxPort,
    domainOutboxPort = domainOutboxPort
)
```

### High Availability Setup
```kotlin
OutboxPoolerExamples.createHighAvailabilitySetup(sagaOutboxPort)
```

## Implementation Strategy

### Phase 1: Basic Integration
1. Implement `UniversalOutboxPort` for your existing outbox
2. Create pooler with default configuration
3. Replace direct EventBus publishing with outbox pattern

### Phase 2: Enhanced Features
1. Add retry logic and failure handling to your outbox implementation
2. Implement custom event publishers as needed
3. Add monitoring and alerting

### Phase 3: Advanced Features
1. Multiple context support (saga, domain, integration events)
2. Priority-based processing
3. Advanced filtering and transformation
4. Metrics and monitoring integration

## Best Practices

### 1. **Configuration**
- Start with conservative batch sizes (20-50)
- Use exponential backoff for retry logic
- Set appropriate cleanup intervals to prevent storage issues

### 2. **Monitoring**
- Monitor pooler health and status
- Track failed events and retry patterns
- Set up alerts for persistent failures

### 3. **Error Handling**
- Implement idempotent event handlers
- Use circuit breaker pattern for downstream failures
- Log detailed error information for debugging

### 4. **Performance**
- Adjust batch size based on event volume
- Consider multiple poolers for high throughput
- Monitor polling intervals vs. latency requirements

### 5. **Testing**
- Test retry logic with failing publishers
- Verify cleanup functionality
- Test graceful shutdown scenarios

## Migration from Existing Solutions

### From Direct EventBus Usage
1. Implement outbox saving alongside existing event publishing
2. Gradually replace direct publishing with outbox pattern
3. Monitor both approaches during transition period

### From Simple Outbox Implementation
1. Extend existing outbox port to implement `OutboxPort<T>`
2. Add retry and failure tracking fields
3. Replace custom pooler with universal pooler

This universal approach provides a solid foundation for reliable event publishing that can grow with your application's needs while maintaining flexibility and type safety.
