# Deployment

## Requirements

- Java 17+
- PostgreSQL 12+
- MinIO Server (or compatible S3 storage)
- Spring Boot 3.x

## Docker Compose

### Option 1: Dependencies Only

Start PostgreSQL and MinIO for local development:

```bash
docker-compose up -d
```

**Services:**
- PostgreSQL: `localhost:5432`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`

### Option 2: Full Stack

Run entire application in Docker:

```bash
docker-compose -f docker-compose.full.yml up -d
```

**Services:**
- PostgreSQL: `localhost:5432`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`
- Application: `http://localhost:8080`

## Starting MinIO

### Docker Compose

```yaml
minio:
  image: minio/minio:latest
  ports:
    - "9000:9000"
    - "9001:9001"
  environment:
    MINIO_ROOT_USER: minioadmin
    MINIO_ROOT_PASSWORD: minioadmin
  command: server /data --console-address ":9001"
```

### Docker (standalone)

```bash
docker run -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

## Bucket Initialization

Bucket is created automatically on first application startup via `MinioConfig`.

**Verification:**
1. Open MinIO Console: `http://localhost:9001`
2. Login with credentials: `minioadmin` / `minioadmin`
3. Verify that `filestorage` bucket is created

## Database Migrations

Migrations are executed automatically via Liquibase on application startup.

**Verification:**
```sql
SELECT * FROM databasechangelog ORDER BY dateexecuted DESC;
```

## Configuration

### Environment Variables

```bash
# Database
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/filestorage

# MinIO
export MINIO_ENDPOINT=http://localhost:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
export MINIO_BUCKET=filestorage

# File Storage
export FILE_STORAGE_MAX_FILE_SIZE=500000000
export FILE_STORAGE_BLOCKED_EXTENSIONS=exe,bat,cmd,sh
```

### application.yaml

Main settings:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/filestorage
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}

minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin}
  bucket-name: ${MINIO_BUCKET:filestorage}

file-storage:
  max-file-size: ${FILE_STORAGE_MAX_FILE_SIZE:500000000}
  blocked-extensions: ${FILE_STORAGE_BLOCKED_EXTENSIONS:exe,bat,cmd,sh}
```

## Build and Run

### Local Development

```bash
# Build
./gradlew build

# Run
./gradlew bootRun
```

### Production

```bash
# Build JAR
./gradlew bootJar

# Run
java -jar build/libs/filestorage-service.jar
```

### Docker

```bash
# Build image
docker build -t filestorage-service .

# Run
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/filestorage \
  -e MINIO_ENDPOINT=http://minio:9000 \
  filestorage-service
```

## Monitoring

### Health Checks

```bash
# Check application health
curl http://localhost:8080/actuator/health
```

### Metrics

```bash
# View metrics
curl http://localhost:8080/actuator/metrics
```

### Recommended Monitoring

- Project storage sizes
- Number of uploaded files
- Upload/download errors
- Quota usage
- API response time

## Recommendations for Production Environment

1. Use HTTPS for all connections
2. Configure connection pooling for PostgreSQL and MinIO
3. Use secrets management for passwords and keys
4. Configure logging to centralized system
5. Configure monitoring and alerting
6. Regular backups of database and MinIO
7. Configure lifecycle policies for MinIO (old file deletion)
8. Use CDN for frequently requested files

## Troubleshooting

### MinIO Connection Issues

1. Check that MinIO is running: `docker ps`
2. Check endpoint in configuration
3. Check credentials
4. Check network between containers

### Migrations Not Executing

1. Check database connection
2. Check application logs
3. Check `databasechangelog` table

### Quota Exceeded

1. Check `max_storage_size` in `project` table
2. Check current `storage_size`
3. Delete unused files
