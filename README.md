# Notes API - Backend

A RESTful API for managing personal notes with JWT authentication, tagging, full-text search, optimistic locking, and rate limiting.

## 📋 Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture & Design Decisions](#architecture--design-decisions)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Running the Application](#running-the-application)
- [Running Tests](#running-tests)
- [API Documentation](#api-documentation)
- [Rate Limiting](#rate-limiting)
- [Database](#database)
- [Docker Setup](#docker-setup)
- [Project Structure](#project-structure)
- [Security](#security)

## ✨ Features

- **User Authentication**
    - JWT-based authentication with access and refresh tokens
    - Secure password hashing with BCrypt
    - Token refresh endpoint

- **Notes Management**
    - Create, read, update, and delete notes
    - Tag-based organization (comma-separated tags)
    - Full-text search across title and content
    - Soft delete with restore capability
    - Optimistic locking with version field (prevents concurrent update conflicts)
    - Pagination and sorting

- **Security & Performance**
    - Rate limiting per endpoint type and user
    - ETags for conditional requests
    - Comprehensive error handling with ProblemDetail
    - CORS configuration

- **Developer Experience**
    - OpenAPI/Swagger documentation
    - H2 console for database inspection
    - Flyway migrations for version control

## 🛠 Tech Stack

- **Java 21** - Programming language
- **Spring Boot 3.4.0** - Application framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Data persistence with Hibernate
- **H2 Database** - Embedded database (file-based for data persistence)
- **Flyway** - Database migrations
- **JWT (io.jsonwebtoken)** - Token-based authentication
- **Bucket4j** - Rate limiting
- **Caffeine** - In-memory caching for rate limits
- **Lombok** - Reduces boilerplate code
- **SpringDoc OpenAPI** - API documentation (Swagger UI)
- **Maven** - Build and dependency management
- **JUnit 5 & MockMvc** - Integration testing

## 🎯 Architecture & Design Decisions

### 1. **Layered Architecture**
```
Controller → Service → Repository → Entity
     ↓           ↓          ↓
   DTOs    Business    Data Access
           Logic
```

**Decision**: Separation of concerns makes the codebase maintainable and testable. Each layer has a clear responsibility.

### 2. **JWT Authentication with Refresh Tokens**
- **Access Token**: 1 hour expiration (short-lived for security)
- **Refresh Token**: 7 days expiration (allows token renewal)
- Stateless authentication (no server-side sessions)

**Decision**: Enables horizontal scaling and reduces server memory usage. Refresh tokens balance security and user experience.

### 3. **Soft Delete Pattern**
- Notes are marked with `deletedAt` timestamp instead of being physically deleted
- `@Where(clause = "deleted_at IS NULL")` automatically filters deleted notes in queries
- Restore functionality available via dedicated endpoint

**Decision**: Prevents accidental data loss, maintains data integrity, and provides audit trail capabilities.

### 4. **Optimistic Locking with @Version**
- Each note has a `version` field that auto-increments on updates
- ETags returned in HTTP responses contain version number
- Client must send `If-Match` header with version for updates

**Decision**: Better performance than pessimistic locking for read-heavy workloads. Prevents lost updates when multiple users edit the same note.

### 5. **Rate Limiting with Bucket4j**
- Token bucket algorithm for rate limiting
- Different limits for different endpoint types
- User-based limiting for authenticated users
- IP-based limiting for unauthenticated requests
- In-memory cache (Caffeine) for performance

**Decision**: Prevents API abuse, ensures fair usage, and protects against DDoS attacks. Token bucket algorithm provides smooth rate limiting without sudden request blocks.

### 6. **File-Based H2 Database**
- Data persists in `./data/notesdb.mv.db`
- Survives application restarts
- Easy setup without external database

**Decision**: Simplifies development setup while still maintaining data persistence. No need for external database installation.

### 7. **Flyway for Database Migrations**
- All schema changes version-controlled in SQL files
- Migrations run automatically on startup
- Ensures consistent database state

**Decision**: Prevents schema drift between environments and makes database evolution traceable.

### 8. **DTOs Separate from Entities**
- Request/Response DTOs for API contracts
- Validation annotations on DTOs
- Never expose entities directly

**Decision**: Decouples internal data model from API contract. Prevents over-posting attacks and allows independent evolution.

### 9. **Native Query for Deleted Notes**
- `findDeletedNoteByIdAndUserId` uses native SQL
- Bypasses `@Where` clause to find soft-deleted notes

**Decision**: Necessary to access deleted notes for restore functionality while maintaining automatic filtering for all other queries.

## 📦 Prerequisites

- **Java 21 or higher**
- **Maven 3.8+**
- **Docker** (optional, for containerization)

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/notes-api.git
cd notes-api
```

### 2. Configure Environment Variables (Optional)

Create a `.env` file or set environment variables:
```bash
export JWT_SECRET="your-secure-secret-key-at-least-32-characters-long"
export PORT=8082
```

### 3. Build the Application
```bash
mvn clean install
```

This will:
- Download dependencies
- Compile the code
- Run Flyway migrations
- Run all tests
- Package the application as a JAR

## 🏃 Running the Application

### Option 1: Using Maven
```bash
mvn spring-boot:run
```

### Option 2: Using the JAR File
```bash
java -jar target/notes-0.0.1-SNAPSHOT.jar
```

### Option 3: Using Docker
```bash
docker build -t notes-api .
docker run -p 8082:8082 notes-api
```

The application will start at **`http://localhost:8082`**

### First Time Setup

On first run, Flyway will automatically:
1. Create the database file at `./data/notesdb.mv.db`
2. Run migration `V1__create_users_table.sql`
3. Run migration `V2__create_notes_table.sql`

## 🧪 Running Tests

### Run All Tests
```bash
mvn test
```

Expected output:
```
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Run Specific Test
```bash
mvn test -Dtest=NotesIntegrationTest
```

### Test Coverage

The project includes:
- **Integration Tests** (`NotesIntegrationTest.java`)
    - Full CRUD lifecycle tests
    - Optimistic locking tests
    - Search and filtering tests
    - Authentication flow tests
- **Rate Limiting Tests** (`RateLimitIntegrationTest.java`)
    - Endpoint-specific rate limit verification
    - Rate limit exceeded scenarios

All tests use:
- `@SpringBootTest` for full context loading
- `MockMvc` for HTTP request simulation
- In-memory H2 database for test isolation

**All tests must pass before considering the build successful.**

## 📚 API Documentation

### Access Swagger UI

Once the application is running, access the interactive API documentation at:
```
http://localhost:8082/api-docs
```

This provides:
- Complete API endpoint listing
- Request/response schemas
- Try-it-out functionality
- Authentication testing

### Quick Start Examples

#### 1. Register a New User
```bash
curl -X POST http://localhost:8082/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "password123"
  }'
```

Response:
```json
{
  "userId": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

#### 2. Login
```bash
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

#### 3. Create a Note
```bash
curl -X POST http://localhost:8082/api/notes \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "My First Note",
    "content": "This is the content of my note",
    "tags": ["important", "work"]
  }'
```

#### 4. Get All Notes (with pagination)
```bash
curl -X GET "http://localhost:8082/api/notes?page=0&size=10&sortBy=updatedAt" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 5. Search Notes
```bash
curl -X GET "http://localhost:8082/api/notes?search=first&tag=work" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 6. Update a Note (with optimistic locking)
```bash
curl -X PUT http://localhost:8082/api/notes/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "If-Match: 0" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Title",
    "content": "Updated content",
    "tags": ["updated"]
  }'
```

#### 7. Soft Delete a Note
```bash
curl -X DELETE http://localhost:8082/api/notes/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 8. Restore a Deleted Note
```bash
curl -X POST http://localhost:8082/api/notes/1/restore \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 9. Refresh Access Token
```bash
curl -X POST http://localhost:8082/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }'
```

### API Endpoints Summary

| Method | Endpoint | Description | Auth Required | Rate Limit |
|--------|----------|-------------|---------------|------------|
| POST | `/api/auth/signup` | Register new user | No | 5/min |
| POST | `/api/auth/login` | Login user | No | 5/min |
| POST | `/api/auth/refresh` | Refresh access token | No | 5/min |
| GET | `/api/notes` | Get all notes (paginated) | Yes | 100/min |
| POST | `/api/notes` | Create new note | Yes | 20/min |
| GET | `/api/notes/{id}` | Get specific note | Yes | 100/min |
| PUT | `/api/notes/{id}` | Update note | Yes | 30/min |
| DELETE | `/api/notes/{id}` | Soft delete note | Yes | 100/min |
| POST | `/api/notes/{id}/restore` | Restore deleted note | Yes | 100/min |

## 🚦 Rate Limiting

The API implements rate limiting to prevent abuse and ensure fair usage.

### Rate Limit Configuration

| Endpoint Type | Rate Limit | Duration | Applies To |
|---------------|------------|----------|------------|
| Authentication (`/api/auth/*`) | 5 requests | per minute | IP address |
| Create Notes (`POST /api/notes`) | 20 requests | per minute | Authenticated user |
| Update Notes (`PUT /api/notes/{id}`) | 30 requests | per minute | Authenticated user |
| General API | 100 requests | per minute | Authenticated user |

### Rate Limit Headers

When you make a request, the response includes rate limit information:
```http
X-Rate-Limit-Remaining: 95
```

When rate limit is exceeded:
```http
HTTP/1.1 429 Too Many Requests
X-Rate-Limit-Retry-After-Seconds: 45
Content-Type: application/json

{
  "message": "Rate limit exceeded. Try again in 45 seconds",
  "data": null
}
```

### Testing Rate Limits
```bash
# Test authentication rate limit (exceeds after 5 requests)
for i in {1..7}; do
  echo "Request $i:"
  curl -w "\nStatus: %{http_code}\n" \
    -X POST http://localhost:8082/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@example.com","password":"test123"}'
  echo "---"
done
```

### Rate Limiting Strategy

- **Authenticated users**: Rate limited by email address
- **Unauthenticated users**: Rate limited by IP address
- **Algorithm**: Token bucket (via Bucket4j)
- **Storage**: In-memory cache (Caffeine)
- **Cache expiration**: 10 minutes after last access

## 🗄 Database

### H2 Console Access

Access the H2 database console at:
```
http://localhost:8082/h2-console
```

**Connection Settings:**
- **JDBC URL**: `jdbc:h2:file:./data/notesdb`
- **Username**: `gideon`
- **Password**: `gideon`
- **Driver**: `org.h2.Driver`

### Database Schema

#### Users Table
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    user_role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
```

#### Notes Table
```sql
CREATE TABLE notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    tags VARCHAR(1000),
    user_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notes_user_id ON notes(user_id);
CREATE INDEX idx_notes_deleted_at ON notes(deleted_at);
CREATE INDEX idx_notes_updated_at ON notes(updated_at DESC);
CREATE INDEX idx_notes_title ON notes(title);
```

### Flyway Migrations

Located in `src/main/resources/db/migration/`:

- **V1__create_users_table.sql** - Creates users table with indexes
- **V2__create_notes_table.sql** - Creates notes table with indexes and foreign key

Migrations run automatically on application startup.

### Data Persistence

- Database file: `./data/notesdb.mv.db`
- Data persists between application restarts
- To reset database: Delete `./data` directory and restart

## 🐳 Docker Setup

### Dockerfile

The project includes a multi-stage Dockerfile optimized for Spring Boot:
```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw clean package -DskipTests
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd -m -u 1001 appuser
COPY --from=builder --chown=appuser:appuser /app/target/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appuser /app/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appuser /app/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appuser /app/target/extracted/application/ ./
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

### Build and Run
```bash
# Build the image
docker build -t notes-api:latest .

# Run the container
docker run -d \
  -p 8082:8082 \
  -v $(pwd)/data:/app/data \
  -e JWT_SECRET="your-production-secret-key" \
  --name notes-api \
  notes-api:latest

# View logs
docker logs -f notes-api

# Stop container
docker stop notes-api

# Remove container
docker rm notes-api
```

**Note**: The `-v $(pwd)/data:/app/data` mounts a volume to persist the H2 database file.

## 📁 Project Structure
```
notes-api/
├── src/
│   ├── main/
│   │   ├── java/com/gideon/notes/
│   │   │   ├── config/
│   │   │   │   ├── CorsConfig.java
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   ├── RateLimitConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── WebMvcConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   └── NotesController.java
│   │   │   ├── dto/
│   │   │   │   ├── AuthDto.java
│   │   │   │   └── NoteDto.java
│   │   │   ├── entity/
│   │   │   │   ├── Note.java
│   │   │   │   └── User.java
│   │   │   ├── enums/
│   │   │   │   └── UserDomain.java
│   │   │   ├── exception/
│   │   │   │   ├── EntityNotFoundException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── RateLimitExceededException.java
│   │   │   │   └── VersionConflictException.java
│   │   │   ├── repository/
│   │   │   │   ├── NotesRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   ├── JwtService.java
│   │   │   │   ├── RateLimitInterceptor.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   ├── service/
│   │   │   │   ├── auth/
│   │   │   │   │   ├── AuthService.java
│   │   │   │   │   └── AuthServiceInt.java
│   │   │   │   ├── notes/
│   │   │   │   │   ├── NoteService.java
│   │   │   │   │   └── NoteServiceInt.java
│   │   │   │   └── ratelimit/
│   │   │   │       └── RateLimitService.java
│   │   │   └── NotesApplication.java
│   │   └── resources/
│   │       ├── db/migration/
│   │       │   ├── V1__create_users_table.sql
│   │       │   └── V2__create_notes_table.sql
│   │       └── application.yml
│   └── test/
│       └── java/com/gideon/notes/
│           └── integration/
│               ├── NotesIntegrationTest.java
│               └── RateLimitIntegrationTest.java
├── .gitignore
├── Dockerfile
├── pom.xml
└── README.md
```

## ⚙️ Configuration

### Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `PORT` | Application HTTP port | 8082 | No |
| `DB_USER` | H2 database username | gideon | No |
| `DB_PASS` | H2 database password | gideon | No |
| `JWT_SECRET` | Secret key for signing JWT tokens | (see application.yml) | **Yes for production** |
| `TOKEN_EXP` | Access token expiration (ms) | 3600000 (1 hour) | No |
| `REFRESH_EXP` | Refresh token expiration (ms) | 604800000 (7 days) | No |

### application.yml
```yaml
server:
  port: ${PORT:8082}

spring:
  datasource:
    url: jdbc:h2:file:./data/notesdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: ${DB_USER:gideon}
    password: ${DB_PASS:gideon}
    
  h2:
    console:
      enabled: true
      path: /h2-console
      
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

jwt:
  secret-key: ${JWT_SECRET:your-secret-key-change-in-production}
  token_exp: ${TOKEN_EXP:3600000}
  refresh_token_exp: ${REFRESH_EXP:604800000}

rate-limit:
  auth:
    capacity: 5
    duration: 1m
  api:
    capacity: 100
    duration: 1m
  notes-create:
    capacity: 20
    duration: 1m
  notes-update:
    capacity: 30
    duration: 1m

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /api-docs
    title: "Notes API Documentation"
```

## 🔒 Security

### Authentication Flow

1. User registers or logs in with email and password
2. Server validates credentials and returns JWT access token + refresh token
3. Client stores tokens (recommended: httpOnly cookies for refresh token, memory for access token)
4. Client includes access token in `Authorization: Bearer <token>` header for all API requests
5. When access token expires (1 hour), client uses refresh token to get new tokens

### Password Security

- All passwords are hashed using BCrypt with strength factor 10
- Passwords never stored in plain text
- Minimum password length: 8 characters (enforced via validation)
- Salt automatically generated per password

### CORS Configuration

Configured in `CorsConfig.java`:
- **Allowed Origins**: `http://localhost:3000` (development)
- **Allowed Methods**: GET, POST, PUT, DELETE, PATCH, OPTIONS
- **Allowed Headers**: All
- **Credentials**: Enabled

**Production**: Update allowed origins to your frontend domain.

### JWT Token Security

- Algorithm: HS256 (HMAC with SHA-256)
- Secret key: Minimum 32 characters (use environment variable in production)
- Claims: user ID, email, roles, issued at, expiration
- Access token: 1 hour validity
- Refresh token: 7 days validity

### Security Best Practices

✅ **Implemented:**
- Password hashing with BCrypt
- JWT token authentication
- Rate limiting on all endpoints
- CORS configuration
- Input validation
- Soft deletes for audit trail
- Optimistic locking

⚠️ **Recommended for Production:**
- Use HTTPS only
- Store JWT_SECRET in secure vault (e.g., AWS Secrets Manager)
- Implement refresh token rotation
- Add request logging and monitoring
- Enable SQL injection protection (JPA handles this)
- Set up firewall rules
- Implement account lockout after failed attempts

## 🐛 Troubleshooting

### Issue: Rate limit exceeded

**Error**: `Rate limit exceeded. Try again in X seconds`

**Solution**: This is expected behavior. Wait for the specified time or adjust rate limits in `application.yml` for development.

### Issue: Version conflict when updating notes

**Error**: `Note was modified by another user`

**Solution**: This is expected behavior with optimistic locking. Get the latest version of the note and retry the update with the correct `If-Match` header.

### Issue: Database file locked

**Error**: `Database may be already in use`

**Solution**: Ensure only one instance of the application is running. Stop any running instances and delete `./data/notesdb.lock` if it exists.

### Issue: Tests failing

**Solution**:
```bash
mvn clean test
```

If tests still fail, check:
- Java version is 21 or higher
- No other application is using port 8082
- All dependencies downloaded correctly
- Database file is not locked

### Issue: JWT token invalid

**Error**: `Invalid or expired token`

**Solution**:
- Check if token has expired (1 hour for access tokens)
- Use refresh token endpoint to get new tokens
- Ensure JWT_SECRET matches between requests

## 📝 Development Guidelines

### Making Changes

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Make your changes
3. Run tests: `mvn test`
4. Commit with meaningful message: `git commit -m "feat: add feature description"`
5. Push and create pull request

### Commit Message Convention

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `refactor:` Code refactoring
- `test:` Adding tests
- `chore:` Maintenance tasks

### Adding New Endpoints

1. Create/update DTO in `dto` package
2. Add method to Service interface and implementation
3. Add controller endpoint with proper annotations
4. Add integration tests
5. Update Swagger documentation
6. Consider rate limiting requirements

## 🚀 Deployment

### Production Checklist

- [ ] Set `JWT_SECRET` environment variable to a secure random string
- [ ] Disable H2 console (`spring.h2.console.enabled=false`)
- [ ] Set `spring.jpa.show-sql=false`
- [ ] Configure proper CORS origins
- [ ] Use HTTPS
- [ ] Set up database backups (if using production database)
- [ ] Configure logging to external service
- [ ] Set up monitoring and alerting
- [ ] Review rate limits for production traffic
- [ ] Enable security headers

### Environment-Specific Configuration

Create `application-prod.yml` for production settings:
```yaml
spring:
  h2:
    console:
      enabled: false
  jpa:
    show-sql: false

logging:
  level:
    root: INFO
    com.gideon.notes: INFO
```

Run with: `java -jar app.jar --spring.profiles.active=prod`

## 📊 Monitoring

### Health Check Endpoint
```bash
curl http://localhost:8082/actuator/health
```

### Metrics

Add Spring Boot Actuator for metrics:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Access at: `http://localhost:8082/actuator`

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

**Built with ❤️ using Spring Boot and Java 21**