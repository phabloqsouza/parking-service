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

## Testing

Run unit tests:
```bash
mvn test
```

## Design Decisions

- **Stateless Design** - No session state stored in application memory
- **Optimistic Locking** - Concurrency control using `@Version` on entities
- **UUID Primary Keys** - Distributed system friendly
- **BigDecimal** - Currency (scale 2), coordinates (scale 8)
- **Transaction Management** - `REPEATABLE_READ` isolation for event handlers, `MANDATORY` propagation for capacity services
- **Multi-Garage Ready** - System prepared for multi-garage support (currently uses default garage)


## Configuration

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
  fee:
    free-minutes: 30
```

Environment variables can override database configuration:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Next Steps

- Add `Hibernate Envers` for auditing purposes
- Implement component tests with cucumber

## License

This project is part of a technical evaluation for Estapar.
