# File Storage Service Architecture

## General Architecture

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

## File Upload Data Flow

1. Client → POST `/api/v1/projects/{id}/resources`
2. ResourceController → request validation
3. FileStorageService → access rights check
4. FileStorageService → storage quota check (with pessimistic lock)
5. FileStorageService → file validation (size, extension)
6. FileStorageService → MIME type detection (Apache Tika)
7. FileStorageService → unique key generation
8. FileStorageService → upload to MinIO
9. FileStorageService → save metadata to PostgreSQL
10. FileStorageService → update project storage size
11. Response → ResourceResponse with file metadata

## Technology Selection

### MinIO - Object Storage

**Why MinIO?**

1. **S3-compatible API**
   - Full compatibility with Amazon S3 API
   - Easy migration to AWS S3 in the future
   - Built-in presigned URL support

2. **Simple Deployment**
   - Lightweight container
   - Minimal resource requirements
   - Simple configuration

3. **Performance**
   - High read/write speed
   - Optimized for large files
   - Multipart upload support

4. **Self-hosted Solution**
   - Full control over data
   - No dependency on external services
   - Reduced storage costs

**Alternatives Considered:**
- **AWS S3** - deferred due to external dependency and costs
- **Local File System** - not suitable for scaling and distributed systems
- **HDFS** - excessive for current requirements

### Apache Tika - MIME Type Detection

**Why Apache Tika?**

1. **Detection Accuracy**
   - Content analysis, not just extension
   - Support for 1000+ file formats
   - Protection against extension spoofing

2. **Reliability**
   - Time-tested Apache project
   - Active community support
   - Regular updates

3. **Performance**
   - Lightweight library
   - Result caching
   - Minimal performance impact

**Alternatives:**
- **Extension-based detection** - insecure, easily spoofed
- **HTTP header detection** - unreliable, client can forge

### PostgreSQL - Metadata Storage

**Why PostgreSQL?**

1. **ACID Guarantees**
   - Transactional operations
   - Data consistency
   - Reliability under concurrent access

2. **Performance**
   - Efficient indexes for search
   - Optimized queries for aggregation
   - Lock support

3. **Already Used in Project**
   - Unified database infrastructure
   - Simple integration
   - Reduced system complexity

### PESSIMISTIC Locking for Project

**Why PESSIMISTIC_WRITE instead of OPTIMISTIC?**

**With OPTIMISTIC_LOCK:**
- When OptimisticLockException occurs, file is already uploaded to MinIO
- Additional rollback logic required for MinIO upload
- Risk of "orphaned" files in storage on errors

**With PESSIMISTIC_WRITE:**
- Write lock occurs immediately on read
- Atomicity of quota check and update guaranteed
- Race condition eliminated during parallel uploads
- File uploaded to MinIO only after successful quota check

**Trade-off:**
- Slight throughput reduction under high concurrency
- Significant improvement in reliability and data consistency

## Main Components

### ResourceController

REST controller providing API for project file operations.

**Main Methods:**
- `POST /api/v1/projects/{projectId}/resources` - upload file
- `GET /api/v1/projects/{projectId}/resources/{resourceId}/download` - download file
- `GET /api/v1/projects/{projectId}/resources/{resourceId}/url` - get presigned URL
- `DELETE /api/v1/projects/{projectId}/resources/{resourceId}` - delete file
- `GET /api/v1/projects/{projectId}/resources` - project file list (with pagination)
- `POST /api/v1/projects/{projectId}/resources/bulk` - bulk file upload

### FileStorageService

Central service implementing file operation business logic.

**Key Functions:**

1. **File Validation**
   - Size check (max 500MB by default)
   - Extension check (block exe, bat, cmd, sh)
   - MIME type detection via Apache Tika

2. **Quota Management**
   - Check current project storage size
   - Validation before upload (with pessimistic lock)
   - Automatic update after operations

3. **Access Control**
   - User role verification
   - Read/delete permission validation
   - Role-based access model support

4. **Storage Key Generation**
   ```
   Format: project-{projectId}/{timestamp}-{uuid}-{sanitizedFileName}
   Example: project-123/1703123456789-a1b2c3d4-document.pdf
   ```

### MinioConfig

MinIO client configuration with automatic bucket creation on application startup.

**Features:**
- Conditional activation via `@ConditionalOnProperty`
- Connection error handling in test environment
- Automatic bucket creation if missing

## Data Models

### Resource

Main entity representing a file in the system.

**Fields:**
- `id: Long` - unique identifier
- `name: String` - original file name
- `key: String` - object storage key
- `contentType: String` - file MIME type
- `size: BigInteger` - file size in bytes
- `type: ResourceType` - file category
- `status: ResourceStatus` - status (ACTIVE, DELETED)
- `allowedRoles: List<UserRole>` - roles with file access
- `project: Project` - project relationship
- `createdBy/updatedBy: User` - author and last editor

**Resource Type Detection:**
Type is automatically determined based on file MIME type:
- `image/*` → IMAGE
- `video/*` → VIDEO
- `audio/*` → AUDIO
- `application/pdf` → PDF
- `application/msword` → MSWORD
- `application/vnd.ms-excel` → MSEXCEL
- `application/zip` → ZIP
- `text/*` → TEXT
- Unknown type → OTHER or NONE

### Project (Extension)

Added fields for storage management:

- `storageSize: BigInteger` - current storage size
- `maxStorageSize: BigInteger` - maximum size (default 2GB)

## Performance

### Optimizations

1. **Streaming Upload/Download**
   - Files not fully loaded into memory
   - Using StreamingResponseBody for download
   - Streaming transfer to MinIO

2. **Database Indexes**
   - Optimized queries for file search
   - Fast storage size aggregation

3. **Pessimistic Locking**
   - Minimal lock duration
   - Lock only during quota check

### Recommendations

- Use connection pooling for MinIO
- Configure metadata caching (if needed)
- Consider CDN for frequently requested files
- Monitor bucket size and configure lifecycle policies

## Future Improvements

### Potential Extensions

1. **File Versioning**
   - Change history preservation
   - Rollback to previous version capability

2. **Encryption**
   - Server-side file encryption
   - Client-side encryption support

3. **CDN Integration**
   - Popular file caching
   - Geographic distribution

4. **Asynchronous Processing**
   - Queues for large files
   - Background virus scanning
   - Automatic thumbnail generation

5. **Advanced Analytics**
   - Storage usage statistics
   - File type reports
   - Access monitoring
