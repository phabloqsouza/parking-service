# Parking Garage Management Microservice

A production-ready microservice for managing parking garage operations, including vehicle entry/exit, spot management, dynamic pricing, and revenue tracking.

## Technology Stack

- **Java 21** - Modern Java features and performance improvements
- **Spring Boot 3.2.x** - Enterprise-grade framework
- **Spring Data JPA** - Data persistence layer
- **Spring Cloud OpenFeign** - Declarative REST client for external integration
- **MySQL 8.0** - Relational database
- **Flyway** - Database migration tool
- **MapStruct** - Compile-time DTO-entity mapping
- **Lombok** - Boilerplate code reduction
- **Maven** - Build and dependency management
- **Docker & Docker Compose** - Containerization
- **Cucumber & RestAssured** - BDD component testing

## Architecture

This microservice follows **Clean Architecture** principles with clear separation of concerns:

```
┌─────────────────────────────────────┐
│         API Layer (REST)            │
│  - Controllers (Webhook, Revenue)   │
│  - DTOs & Mappers                   │
└─────────────────────────────────────┘
                ↓
┌─────────────────────────────────────┐
│      Application Services           │
│  - ParkingEventService              │
│  - GarageInitializationService      │
│  - RevenueService                   │
└─────────────────────────────────────┘
                ↓
┌─────────────────────────────────────┐
│        Domain Layer                 │
│  - Entities (Garage, Sector, etc.)  │
│  - Services (PricingService, etc.)  │
│  - Business Rules                   │
└─────────────────────────────────────┘
                ↓
┌─────────────────────────────────────┐
│    Infrastructure Layer             │
│  - JPA Repositories                 │
│  - External Clients (Feign)         │
│  - Database                         │
└─────────────────────────────────────┘
```

### Key Design Patterns

- **Repository Pattern** - Data access abstraction
- **Strategy Pattern** - Database-driven dynamic pricing
- **Template Method Pattern** - Fee calculation algorithm
- **Facade Pattern** - PricingService orchestrator
- **Dependency Injection** - Spring IoC container

### SOLID Principles

- **Single Responsibility** - Each class has one reason to change
- **Open/Closed** - Extensible pricing strategies without modification
- **Liskov Substitution** - Proper inheritance hierarchies
- **Interface Segregation** - Focused interfaces
- **Dependency Inversion** - Depend on abstractions

## Features

- ✅ Vehicle entry/exit management (ENTRY, PARKED, EXIT events)
- ✅ Dynamic pricing based on occupancy (0-25%: -10%, 25-50%: 0%, 50-75%: +10%, 75-100%: +25%)
- ✅ Capacity management (100% closure, optimistic locking)
- ✅ Spot matching by coordinates (GPS tolerance-based)
- ✅ Revenue tracking by date and sector
- ✅ First 30 minutes free, hourly rate with ceiling rounding
- ✅ Automatic garage initialization from simulator
- ✅ Multi-garage support (prepared, currently uses default garage)

## Setup Instructions

### Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose
- MySQL 8.0 (or use Docker)

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/phabloqsouza/parking-service.git
   cd parking-service
   ```

2. **Start the simulator**
   ```bash
   docker run -d --network="host" cfontes0estapar/garage-sim:1.0.0
   ```

3. **Configure database** (if not using Docker Compose)
   - Create database: `parking_db`
   - User: `parking_user`
   - Password: `parking_password`

4. **Update application.yml** if needed
   - Database connection details
   - Simulator URL (default: `http://localhost:8080`)

5. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

   The application will:
   - Automatically initialize the garage from the simulator (via ApplicationRunner)
   - Run Flyway migrations
   - Start on port 3003

### Docker Compose (Recommended)

1. **Start all services**
   ```bash
   docker-compose up -d
   ```

   This will start:
   - MySQL database
   - Parking simulator
   - Parking service (with automatic initialization)

2. **Check logs**
   ```bash
   docker-compose logs -f parking-service
   ```

3. **Stop all services**
   ```bash
   docker-compose down
   ```

### Building Docker Image

```bash
docker build -t parking-service:1.0.0 .
docker run -p 3003:3003 parking-service:1.0.0
```

## API Documentation

### Webhook Endpoint

**POST** `/webhook`

Accepts vehicle events (ENTRY, PARKED, EXIT) from the simulator.

**Request Headers:**
- `X-Garage-Id` (optional): UUID of the garage (defaults to default garage)

**Request Body Examples:**

**ENTRY Event:**
```json
{
  "license_plate": "ABC1234",
  "entry_time": "2025-01-01T12:00:00.000Z",
  "event": "ENTRY",
  "sector": "A"
}
```

**PARKED Event:**
```json
{
  "license_plate": "ABC1234",
  "lat": -23.561684,
  "lng": -46.655981,
  "event": "PARKED"
}
```

**EXIT Event:**
```json
{
  "license_plate": "ABC1234",
  "exit_time": "2025-01-01T14:00:00.000Z",
  "event": "EXIT"
}
```

**Response:** HTTP 200 (OK) or HTTP 409/400 (Error)

### Revenue Query Endpoint

**POST** `/revenue`

Query revenue by date and sector.

**Request Body:**
```json
{
  "date": "2025-01-01",
  "sector": "A"
}
```

**Response:**
```json
{
  "amount": 150.00,
  "currency": "BRL",
  "timestamp": "2025-01-01T12:00:00.000Z"
}
```

**Note:** Only completed sessions (with `exit_time` and `final_price`) are included in revenue.

### Internal Initialization Endpoint

**POST** `/internal/initialize`

Manually trigger garage initialization from simulator (used by Docker entrypoint).

**Response:** HTTP 200 (Success) or HTTP 500 (Error)

## Business Rules

### Entry Rules

- Check sector availability (not at 100% capacity)
- Reserve capacity (increment `occupied_count`) using optimistic locking
- Calculate dynamic pricing based on current occupancy
- Create parking session with `spot_id = null` (assigned on PARKED event)

### Parked Rules

- Match spot by coordinates within tolerance (0.000001 degrees ≈ 0.1 meters)
- Handle gracefully if spot not found (keep `spot_id = null`, allow EXIT)
- Handle duplicate PARKED events idempotently
- Capacity already counted on ENTRY (not incremented on PARKED)

### Exit Rules

- First 30 minutes are free
- After 30 minutes, charge hourly rate (rounded up with CEILING)
- Calculate final price using base price with dynamic pricing multiplier (from entry time)
- Free spot if `spot_id` is set
- Decrement sector `occupied_count`
- Record revenue

### Pricing Rules

Dynamic pricing multipliers based on occupancy at **entry time**:

| Occupancy | Multiplier | Description |
|-----------|------------|-------------|
| 0-25%     | 0.90       | Low occupancy discount (-10%) |
| 25-50%    | 1.00       | Normal pricing (0%) |
| 50-75%    | 1.10       | High occupancy increase (+10%) |
| 75-100%   | 1.25       | Full occupancy increase (+25%) |

### Capacity Rules

- Sector closes at 100% capacity (no new entries allowed)
- Capacity is incremented on ENTRY event (not on PARKED)
- PARKED events (spot not found, already parked) still count toward capacity
- Capacity is decremented on EXIT event

## Assumptions and Design Decisions

### Garage Identification

**Current Implementation:**
- All operations use the default garage (first garage created is set as `is_default = true`)
- If `X-Garage-Id` header is missing, the system treats the request as belonging to the default garage

**Future Implementation:**
- Garage information can be passed via HTTP header `X-Garage-Id` (UUID)
- System is prepared for multi-garage support with minimal refactoring

**Alternative Approaches Considered:**
- Path parameter: `/garages/{garageId}/webhook` (more RESTful but requires URL changes)
- Request body field: Optional `garage_id` in webhook DTO (requires API contract changes)
- JWT token claim: For authenticated multi-tenant scenarios (requires auth infrastructure)

### Multi-Garage Support

The system is designed for future multi-garage support:
- `Garage` entity with `is_default` flag
- `GarageResolver` service for garage resolution
- All tables include `garage_id` foreign key
- Horizontal scalability ready (stateless design, optimistic locking, UUIDs)

### Scalability

- **Stateless Design** - No session state stored in application memory
- **Optimistic Locking** - Concurrency control using `@Version` on entities
- **UUID Primary Keys** - Distributed system friendly
- **Indexed Queries** - Optimized database access patterns
- **Horizontal Scaling** - Can run multiple containers behind load balancer

### Database Design

- **Singular Table Names** - `garage`, `sector`, `parking_spot`, `parking_session`
- **Normalized Schema** - `parking_spot` doesn't have `garage_id` (accessed via `sector.garage_id`)
- **Optimized Indexes** - Only essential indexes for query patterns
- **Optimistic Locking** - `@Version` field on entities that need concurrency control

### Precision Handling

- **BigDecimal** - Currency (scale 2), coordinates (scale 8), all numeric calculations
- **Instant** - Datetime fields (UTC by default for API responses)
- **Timezone** - Application uses UTC-3 (America/Sao_Paulo) for business logic, UTC for API serialization

## Testing

### Unit Tests

Run unit tests (default profile):
```bash
mvn test
```

Or explicitly:
```bash
mvn test -Punit-tests
```

### Component Tests (BDD with Cucumber)

Component tests use Cucumber BDD framework with RestAssured to test the full application stack. They require the application and its dependencies (MySQL, simulator) to be running.

**Prerequisites:**
- Start all services using Docker Compose:
  ```bash
  docker-compose -f docker-compose-component-tests.yml up -d
  ```
  
  Or start services manually:
  - MySQL on port 3307 with database `parking_db_test`, user `parking_user_test`, password `parking_password_test`
  - Simulator on port 8080
  - Parking service on port 3004

**Run component tests:**

Using Docker Compose (recommended):
```bash
docker-compose -f docker-compose-component-tests.yml up component-tests
```

Using Maven directly:
```bash
# Set environment variables (required)
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/parking_db_test?useSSL=false&allowPublicKeyRetrieval=true
export SPRING_DATASOURCE_USERNAME=parking_user_test
export SPRING_DATASOURCE_PASSWORD=parking_password_test
export PARKING_SIMULATOR_URL=http://localhost:8080
export PARKING_SERVICE_URL=http://localhost:3004

# Run component tests with the profile
mvn verify -Pcomponent-tests \
  -Dspring.datasource.url=$SPRING_DATASOURCE_URL \
  -Dspring.datasource.username=$SPRING_DATASOURCE_USERNAME \
  -Dspring.datasource.password=$SPRING_DATASOURCE_PASSWORD \
  -Dparking.simulator.url=$PARKING_SIMULATOR_URL \
  -Dparking.service.url=$PARKING_SERVICE_URL
```

On Windows (PowerShell):
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3307/parking_db_test?useSSL=false&allowPublicKeyRetrieval=true"
$env:SPRING_DATASOURCE_USERNAME="parking_user_test"
$env:SPRING_DATASOURCE_PASSWORD="parking_password_test"
$env:PARKING_SIMULATOR_URL="http://localhost:8080"
$env:PARKING_SERVICE_URL="http://localhost:3004"

mvn verify -Pcomponent-tests `
  -Dspring.datasource.url=$env:SPRING_DATASOURCE_URL `
  -Dspring.datasource.username=$env:SPRING_DATASOURCE_USERNAME `
  -Dspring.datasource.password=$env:SPRING_DATASOURCE_PASSWORD `
  -Dparking.simulator.url=$env:PARKING_SIMULATOR_URL `
  -Dparking.service.url=$env:PARKING_SERVICE_URL
```

On Windows (CMD):
```cmd
set SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/parking_db_test?useSSL=false&allowPublicKeyRetrieval=true
set SPRING_DATASOURCE_USERNAME=parking_user_test
set SPRING_DATASOURCE_PASSWORD=parking_password_test
set PARKING_SIMULATOR_URL=http://localhost:8080
set PARKING_SERVICE_URL=http://localhost:3004

mvn verify -Pcomponent-tests ^
  -Dspring.datasource.url=%SPRING_DATASOURCE_URL% ^
  -Dspring.datasource.username=%SPRING_DATASOURCE_USERNAME% ^
  -Dspring.datasource.password=%SPRING_DATASOURCE_PASSWORD% ^
  -Dparking.simulator.url=%PARKING_SIMULATOR_URL% ^
  -Dparking.service.url=%PARKING_SERVICE_URL%
```

**Cucumber feature files** are located in `src/test/resources/component/features/`:
- `parking-events.feature` - Entry, parked, exit flow scenarios
- `revenue.feature` - Revenue query scenarios
- `capacity-management.feature` - Capacity and closure rules
- `pricing.feature` - Dynamic pricing and fee calculation
- `spot-matching.feature` - Coordinate matching scenarios

### Run All Tests (Unit + Component)

```bash
mvn verify -Pall-tests \
  -Dspring.datasource.url=jdbc:mysql://localhost:3307/parking_db_test?useSSL=false&allowPublicKeyRetrieval=true \
  -Dspring.datasource.username=parking_user_test \
  -Dspring.datasource.password=parking_password_test \
  -Dparking.simulator.url=http://localhost:8080 \
  -Dparking.service.url=http://localhost:3004
```

### Unit Tests

- Domain services (PricingService, SpotLocationMatcher)
- Application services
- BigDecimal precision validation

### Integration Tests

- Repository layer with Testcontainers
- Database operations
- Optimistic locking scenarios

## Configuration

### Application Properties

Key configuration in `application.yml`:

```yaml
server:
  port: 3003

parking:
  simulator:
    url: http://localhost:8080
    retry:
      max-attempts: 5
      initial-interval-millis: 2000
      max-interval-millis: 32000
  
  spot:
    coordinate-tolerance: 0.000001  # degrees
  
  application:
    timezone: America/Sao_Paulo  # UTC-3
  
  api:
    use-local-timezone: false  # UTC for API responses
```

## Project Structure

```
src/
├── main/
│   ├── java/com/estapar/parking/
│   │   ├── domain/          # Domain entities and services
│   │   ├── application/     # Application services
│   │   ├── infrastructure/  # JPA repositories, external clients
│   │   ├── api/             # REST controllers, DTOs
│   │   └── config/          # Configuration classes
│   └── resources/
│       ├── application.yml
│       └── db/migration/    # Flyway migrations
├── test/
│   ├── java/                # Test classes
│   └── resources/
│       └── component/features/  # Cucumber feature files
├── docker/
│   └── docker-entrypoint.sh
├── Dockerfile
├── docker-compose.yml
└── README.md
```

## Troubleshooting

### Application won't start

- Check MySQL is running and accessible
- Verify database credentials in `application.yml`
- Check simulator is running on port 8080
- Review application logs for errors

### Garage initialization fails

- Verify simulator is accessible at configured URL
- Check network connectivity
- Review Feign client retry logs
- Manually trigger initialization: `POST /internal/initialize`

### Database connection issues

- Verify MySQL is running: `docker ps`
- Check connection string in `application.yml`
- Verify database exists: `mysql -u parking_user -p`
- Check Flyway migration status in logs

## Future Enhancements

- [ ] Authentication and authorization (JWT/OAuth2)
- [ ] Multi-garage support with garage selection UI
- [ ] Real-time capacity monitoring via WebSocket
- [ ] Advanced reporting and analytics
- [ ] Payment integration
- [ ] Mobile app API
- [ ] Event sourcing for audit trail
- [ ] Caching layer (Redis) for frequently accessed data

## License

This project is part of a technical evaluation for Estapar.

## Contact

For questions or issues, please contact the development team.
