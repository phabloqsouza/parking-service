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
- ✅ Spot matching by exact coordinates
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
   - Or set environment variables: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`

4. **Database configuration** (optional)
   - Default values are in `application.yml` for local development
   - Override with environment variables for different environments (see Configuration section)
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
  "event": "ENTRY"
}
```

**Note:** Sector is not specified on ENTRY event. It is determined when the vehicle parks (PARKED event) based on the matched spot's sector.

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

- Check garage availability (not at 100% garage capacity)
- Capacity is conceptually reserved via garage occupancy calculation (sessions without sector count toward garage capacity)
- Create parking session with `spot_id = null` (assigned on PARKED event)
- Calculate dynamic pricing multiplier based on garage occupancy percentage at entry time
- Store pricing multiplier in parking session for use on exit event
- Multiplier is determined by current garage occupancy and pricing strategy ranges

### Parked Rules

- Match spot by exact coordinates (lat/lng must match precisely)
- Assign spot to parking session (sets `spot_id`)
- Increment sector `occupied_count` when spot is assigned
- No pricing calculation on parked event (pricing multiplier was already calculated and stored on entry event)
- Handle gracefully if spot not found (keep `spot_id = null`, allow EXIT, but session still counts toward garage capacity)
- Handle duplicate PARKED events idempotently (ignore if already has spot assigned)

### Exit Rules

- First 30 minutes are free (configurable via `parking.fee.free-minutes`)
- After 30 minutes, charge hourly rate (rounded up with CEILING - total minutes divided by 60, rounded up)
- Calculate final price using:
  - Stored `pricingMultiplier` from entry event
  - `basePrice` from spot's sector (or zero if no spot assigned)
  - Formula: `effectivePrice = basePrice * pricingMultiplier`
  - Apply fee calculation based on parking duration
- Free spot if `spot_id` is set
- Decrement sector `occupied_count` (if spot was assigned)
- Record revenue (only completed sessions with `exit_time` and `final_price` are included)

### Pricing Rules

Dynamic pricing multipliers based on occupancy at **entry time**:

| Occupancy | Multiplier | Description |
|-----------|------------|-------------|
| 0-25%     | 0.90       | Low occupancy discount (-10%) |
| 25-50%    | 1.00       | Normal pricing (0%) |
| 50-75%    | 1.10       | High occupancy increase (+10%) |
| 75-100%   | 1.25       | Full occupancy increase (+25%) |

**Pricing Calculation Flow:**
1. **ENTRY Event**: Multiplier is calculated based on garage occupancy percentage and stored in `parking_session.pricing_multiplier`
2. **EXIT Event**: 
   - Get `basePrice` from spot's sector (or zero if no spot was assigned)
   - Calculate `effectivePrice = basePrice * pricingMultiplier`
   - Apply fee calculation based on parking duration (free for first 30 minutes, then hourly rate rounded up)
3. **No Spot Assigned**: If vehicle entered but never parked, uses zero basePrice (free parking) with multiplier applied

### Capacity Rules

- Garage closes at 100% garage capacity (no new entries allowed)
- Capacity is tracked at two levels:
  - **Garage level**: Counts sessions without sector (entered but not parked) + sum of all sector `occupied_count`
  - **Sector level**: `occupied_count` is incremented when spot is assigned (on PARKED event)
- If spot is not found on PARKED event, session remains without sector and still counts toward garage capacity
- Sector `occupied_count` is decremented on EXIT event (if spot was assigned)
- Sessions that entered but never parked (no PARKED event) count toward garage capacity but not sector capacity

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
- **Parking Session Structure**:
  - `pricing_multiplier` - Stored on entry event, used on exit event
  - `spot_id` - Nullable, assigned on parked event
  - No `sector_id` field (sector accessed via `spot.sector`)
  - No `base_price` field (basePrice comes from spot's sector)
- **Optimized Indexes** - Only essential indexes for query patterns
- **Optimistic Locking** - `@Version` field on entities that need concurrency control

### Precision Handling

- **BigDecimal** - Currency (scale 2), coordinates (scale 8), all numeric calculations
- **Instant** - Datetime fields (UTC by default for API responses)
- **Timezone** - Application uses UTC-3 (America/Sao_Paulo) for business logic, UTC for API serialization

### Transaction Configuration

The application uses strategic transaction management for data consistency and concurrency control:

- **Event Handlers** (`BaseEventHandler`):
  - Isolation Level: `REPEATABLE_READ` - Ensures consistent capacity calculations and pricing multiplier determination during concurrent events
  - Timeout: 30 seconds - Prevents long-running transactions
  - Prevents non-repeatable reads and phantom reads when checking garage capacity

- **Capacity Services** (`ParkingSpotService`, `SectorCapacityService`):
  - Propagation: `MANDATORY` - Must run within existing transaction context
  - Ensures atomicity of spot assignment and capacity updates
  - Enforces transaction boundaries for data consistency

- **Read-Only Operations**:
  - `ParkingSessionService`, `GarageResolver`, `PricingService.getRevenue()`, `PricingStrategyResolver`
  - Marked as `readOnly = true` for query optimization

- **Locking Strategy**:
  - **Optimistic Locking**: Used for spot lookup (`OPTIMISTIC` lock mode)
  - **Version Fields**: `@Version` on `ParkingSession`, `ParkingSpot`, and `Sector` entities
  - Detects concurrent modifications and prevents lost updates

- **Initialization**:
  - `GarageInitializationService`: `REPEATABLE_READ` isolation with 60-second timeout for external service calls

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

  -Dspring.datasource.username=parking_user_test \
  -Dspring.datasource.password=parking_password_test \
  -Dparking.simulator.url=http://localhost:8080 \
  -Dparking.service.url=http://localhost:3004
```

### Unit Tests

- Domain services (PricingService, SpotLocationMatcher)
- Application services
- BigDecimal precision validation


## Configuration

### Application Properties

Key configuration in `application.yml`:

```yaml
server:
  port: 3003

spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/parking_db?useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true}
    username: ${SPRING_DATASOURCE_USERNAME:parking_user}
    password: ${SPRING_DATASOURCE_PASSWORD:parking_password}

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

### Environment Variables

Database configuration can be overridden using environment variables (best practice for production):

**Database Configuration:**
- `SPRING_DATASOURCE_URL` - Database connection URL (default: `jdbc:mysql://localhost:3306/parking_db?useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true`)
- `SPRING_DATASOURCE_USERNAME` - Database username (default: `parking_user`)
- `SPRING_DATASOURCE_PASSWORD` - Database password (default: `parking_password`)

**Example (Linux/Mac):**
```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://production-db:3306/parking_db
export SPRING_DATASOURCE_USERNAME=prod_user
export SPRING_DATASOURCE_PASSWORD=prod_password
mvn spring-boot:run
```

**Example (Windows PowerShell):**
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://production-db:3306/parking_db"
$env:SPRING_DATASOURCE_USERNAME="prod_user"
$env:SPRING_DATASOURCE_PASSWORD="prod_password"
mvn spring-boot:run
```

**For Docker Compose:**
Environment variables can be set in `docker-compose.yml` or a `.env` file for different environments.

**For Manual Flyway Migrations (Maven Plugin):**
```bash
mvn flyway:migrate \
  -Dflyway.url=${SPRING_DATASOURCE_URL} \
  -Dflyway.user=${SPRING_DATASOURCE_USERNAME} \
  -Dflyway.password=${SPRING_DATASOURCE_PASSWORD}
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
│   └── java/                # Test classes
├── docker/
│   └── docker-entrypoint.sh
├── Dockerfile
├── docker-compose.yml
└── README.md
```

## Troubleshooting

### Application won't start

- Check MySQL is running and accessible
- Verify database credentials in `application.yml` or environment variables (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`)
- Check simulator is running on port 8080
- Review application logs for errors

### Garage initialization fails

- Verify simulator is accessible at configured URL
- Check network connectivity
- Review Feign client retry logs
- Manually trigger initialization: `POST /internal/initialize`

### Database connection issues

- Verify MySQL is running: `docker ps`
- Check connection string in `application.yml` or environment variables
- Verify database exists: `mysql -u parking_user -p` (or use your configured credentials)
- Check Flyway migration status in logs
- Ensure environment variables are properly set (if overriding defaults)

## Future Enhancements

- [ ] Authentication and authorization (JWT/OAuth2)
- [ ] Multi-garage support with garage selection UI
- [ ] Real-time capacity monitoring via WebSocket
- [ ] Advanced reporting and analytics
- [ ] Payment integration
- [ ] Mobile app API
- [ ] Event sourcing for audit trail
- [ ] Caching layer (Redis) for frequently accessed data
- [ ] Coordinate tolerance-based spot matching - Currently requires exact coordinate match. Future: Add configurable tolerance (e.g., 0.000001 degrees ≈ 0.1 meters) to handle GPS drift and coordinate precision variations.

## License

This project is part of a technical evaluation for Estapar.

## Contact

For questions or issues, please contact the development team.
