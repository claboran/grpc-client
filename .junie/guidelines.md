# gRPC Client Project Guidelines

This document provides essential information for developers working on the gRPC Client project.

## Build/Configuration Instructions

### Prerequisites
- JDK 21
- Gradle (wrapper included)

### Building the Project
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

### Configuration
The application is configured via `src/main/resources/application.properties`:

- **Application Port**: The REST API runs on port 8489 by default
- **gRPC Client Configuration**: 
  - The client connects to a gRPC server at `localhost:9090` by default
  - Uses plaintext communication (no TLS)
  - Additional gRPC tuning parameters are available but commented out

To modify these settings for different environments, you can:
1. Edit the `application.properties` file
2. Override properties via command line: `./gradlew bootRun --args='--grpc.client.computation-service.address=static://other-host:9090'`
3. Use Spring profiles for different environments

## Testing Information

### Running Tests
```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "de.laboranowitsch.poc.grpcclient.infrastructure.persistence.InMemoryCalculationJobRepositoryTest"

# Run a specific test method
./gradlew test --tests "de.laboranowitsch.poc.grpcclient.infrastructure.persistence.InMemoryCalculationJobRepositoryTest.should save and retrieve a job"
```

### Test Structure
- Tests are located in `src/test/kotlin`
- The project uses JUnit 5 (Jupiter) for testing
- Spring Boot test utilities are available for integration tests

### Writing Tests
1. **Unit Tests**: Test individual components in isolation
   - Example: `InMemoryCalculationJobRepositoryTest` tests the repository without dependencies
   - Follow the Arrange-Act-Assert pattern for clarity

2. **Integration Tests**: Test interactions between components
   - Use `@SpringBootTest` for tests that require the Spring context
   - Configure mock gRPC services for testing without external dependencies

3. **Test Example**:
```kotlin
@Test
fun `should save and retrieve a job`() {
    // Arrange
    val repository = InMemoryCalculationJobRepository()
    val job = CalculationJob(
        id = "test-job-1",
        inputCount = 5,
        status = "ACCEPTED"
    )

    // Act
    repository.saveInitial(job)
    val retrievedJob = repository.findById("test-job-1")

    // Assert
    assertNotNull(retrievedJob)
    assertEquals("test-job-1", retrievedJob?.id)
    assertEquals(5, retrievedJob?.inputCount)
    assertEquals("ACCEPTED", retrievedJob?.status)
    assertTrue(retrievedJob?.results?.isEmpty() ?: false)
}
```

## Additional Development Information

### Project Architecture
The project follows a hexagonal (ports and adapters) architecture:

- **Application Core**: Contains business logic and domain models
  - `CalculationService`: Core service implementing the `CalculationPort` interface
  - `model`: Domain models like `CalculationJob` and `ProcessedResult`
  - `ports`: Interfaces for external dependencies

- **Infrastructure**: Contains adapters for external systems
  - `grpc`: gRPC client adapter for external computation service
  - `persistence`: Repository implementations for data storage

- **Interfaces**: Contains API controllers and DTOs
  - `api`: REST controllers and request/response models

### gRPC Integration
- Proto files are located in `src/main/proto`
- Generated code is placed in `build/generated/source/proto`
- The gRPC client is configured using the `grpc-client-spring-boot-starter` library
- The `GrpcExternalComputationAdapter` handles communication with the external gRPC service

### Kotlin and Coroutines
- The project uses Kotlin coroutines for asynchronous processing
- The gRPC client uses Kotlin coroutine stubs for non-blocking communication
- The `CoroutineConfig` provides a coroutine scope for background processing

### Error Handling
- The application uses a combination of try-catch blocks and coroutine error handling
- gRPC-specific errors are handled in the `GrpcExternalComputationAdapter`
- REST API errors are handled in the `CalculationController`

### Logging
- The application uses SLF4J for logging
- The `LoggingAware` interface provides a consistent logging approach
- Log levels can be configured in `application.properties`