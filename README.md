# gRPC Client Project

This project implements a client for a gRPC-based calculation service. It provides a REST API that allows clients to submit calculation jobs, which are then processed asynchronously by communicating with an external gRPC computation service.
This is the corresponding client for the [gRPC Mock Service](https://github.com/claboran/grpc-mock).

## Features

- Asynchronous job processing using Kotlin coroutines
- REST API for job submission and status checking
- Integration with external gRPC computation service
- In-memory job storage with status tracking

## Getting Started

[guidelines.md](.junie/guidelines.md) for additional development guidelines and best practices.
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

## API Usage

### Submit a Calculation Job

```
POST /api/v1/calculations
Content-Type: application/json

{
  "inputs": ["value1", "value2", "value3"]
}
```

Response:
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Check Job Status

```
GET /api/v1/calculations/{jobId}/status
```

Response:
```json
{
  "status": "PROCESSING"
}
```

### Get Job Results

```
GET /api/v1/calculations/{jobId}/results
```

Response:
```json
{
  "results": ["result1", "result2", "result3"]
}
```

## Architecture

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

## Testing

```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "de.laboranowitsch.poc.grpcclient.infrastructure.persistence.InMemoryCalculationJobRepositoryTest"
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.