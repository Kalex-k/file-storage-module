# File Storage Service

Independent file storage service based on MinIO and PostgreSQL with access control, quotas, and bulk operations support.

## Overview

Full-featured service for file management with role-based access control, storage size limits, and bulk operations support.

### Key Features

- File upload with size and type validation
- File download with access control
- Presigned URL generation for direct file access
- File deletion with permission checks
- Bulk file upload
- Project file list pagination
- Project-level storage quota control
- MIME type detection (Apache Tika)
- File type classification
- Blocked file extensions

## Architecture

```
┌─────────────────┐
│ ResourceController │  ← REST API Layer
└────────┬──────────┘
         │
         ▼
┌─────────────────┐
│ FileStorageService │  ← Business Logic Layer
└────────┬──────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐  ┌──────────┐
│ MinIO  │  │PostgreSQL│  ← Storage Layer
└────────┘  └──────────┘
```

### Application Layers

1. **Controller Layer** - HTTP request handling, validation, response formation
2. **Service Layer** - business logic, access control, quota management
3. **Repository Layer** - data access abstraction
4. **Storage Layer** - MinIO for files, PostgreSQL for metadata

For more details: [ARCHITECTURE.md](docs/ARCHITECTURE.md)

## Technologies

- **Java 17** + **Spring Boot 3.0.6**
- **PostgreSQL** - metadata and transactions
- **MinIO** - S3-compatible object storage
- **Apache Tika** - MIME type detection by content
- **Liquibase** - database migration management

### Key Architectural Decisions

**MinIO** - S3-compatible API, simple deployment, self-hosted solution  
**Apache Tika** - accurate MIME type detection by content, protection against extension spoofing  
**PostgreSQL** - ACID guarantees, efficient indexes, pessimistic locking for quotas  
**Pessimistic Locking** - prevents race conditions during parallel uploads

For more details: [ARCHITECTURE.md](docs/ARCHITECTURE.md)

## Quick Start

### Docker Compose (recommended)

```bash
# Start PostgreSQL and MinIO
docker-compose up -d

# Check status
docker-compose ps
```

After startup:
- **PostgreSQL**: `localhost:5432` (user: `postgres`, password: `postgres`)
- **MinIO API**: `http://localhost:9000`
- **MinIO Console**: `http://localhost:9001` (user: `minioadmin`, password: `minioadmin`)

### Running the Application

```bash
# Using Gradle
./gradlew bootRun

# Or build JAR
./gradlew build
java -jar build/libs/filestorage-service.jar
```

The application will be available at `http://localhost:8080`

For more details: [DEPLOYMENT.md](docs/DEPLOYMENT.md)

## API Endpoints

| Method | Endpoint | Description |
|-------|----------|-------------|
| POST | `/api/v1/projects/{projectId}/resources` | Upload file |
| GET | `/api/v1/projects/{projectId}/resources/{resourceId}/download` | Download file |
| GET | `/api/v1/projects/{projectId}/resources/{resourceId}/url` | Get presigned URL |
| DELETE | `/api/v1/projects/{projectId}/resources/{resourceId}` | Delete file |
| GET | `/api/v1/projects/{projectId}/resources` | List files (with pagination) |
| POST | `/api/v1/projects/{projectId}/resources/bulk` | Bulk upload |

### Required Headers

All requests require header: `x-user-id: {userId}`

### Upload Example

```bash
curl -X POST http://localhost:8080/api/v1/projects/1/resources \
  -H "x-user-id: 1" \
  -F "file=@document.pdf" \
  -F "allowedRoles=MANAGER,DEVELOPER"
```

## Security

### Role-Based Access Model

- Files can be restricted to specific roles during upload
- Access check on download (project membership + role)
- Deletion available only to file creator or project manager

### File Validation

- Maximum size: 500MB (configurable)
- Blocked extensions: `exe`, `bat`, `cmd`, `sh`
- MIME type detection via Apache Tika (content analysis)
- File name sanitization

For more details: [SECURITY.md](docs/SECURITY.md)

## Configuration

Main settings in `application.yaml`:

```yaml
file-storage:
  max-file-size: 500000000  # 500MB
  blocked-extensions: exe,bat,cmd,sh
  presigned-url-expiry-seconds: 3600
  bulk-upload-max-files: 10

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: filestorage
```

All values can be overridden via environment variables.

## Additional Documentation

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) - architecture and design decisions
- [SECURITY.md](docs/SECURITY.md) - security and access control
- [DEPLOYMENT.md](docs/DEPLOYMENT.md) - deployment and infrastructure

## Development

### Requirements

- Java 17+
- PostgreSQL 12+
- MinIO (via docker-compose)

### Build

```bash
./gradlew build
```

### Run Tests

```bash
./gradlew test
```

## License

This project is created to demonstrate file storage functionality.

---

**Version:** 1.0
