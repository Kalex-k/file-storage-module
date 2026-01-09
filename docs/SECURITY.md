# Security and Access Control

## Role-Based Access Model

Files can be restricted to specific project participant roles:

```java
// Allowed roles can be specified during upload
POST /api/v1/projects/123/resources
  ?allowedRoles=MANAGER,DEVELOPER

// If roles not specified, all project participants have access
```

### Access Check on Download

1. User existence verification
2. Check for at least one allowed role
3. File status check (ACTIVE only)

### Deletion Rights

Files can be deleted by:
- **File creator** (`createdBy`)
- **Project manager** (role `MANAGER` or `OWNER`)

## File Validation

### File Size

- Maximum size: 500MB (configurable via `file-storage.max-file-size`)
- Check before upload to storage
- Error: `PAYLOAD_TOO_LARGE` (413)

### File Extensions

- Blocked: `exe`, `bat`, `cmd`, `sh`
- Configurable list via `file-storage.blocked-extensions` configuration
- Error: `IllegalArgumentException` (400)

### MIME Type

- Detection via Apache Tika (content analysis)
- Fallback to client `Content-Type` header
- Default type: `application/octet-stream`
- Protection against extension spoofing

## File Name Sanitization

File names are cleaned before saving:

- Remove special characters (regex: `[^a-zA-Z0-9.-]`)
- Replace with underscores
- Convert to lowercase
- Remove duplicate underscores

**Example:**
```
"My Document (2024).pdf" → "my_document_2024_.pdf"
```

## Storage Key Generation

Key format ensures uniqueness and structure:

```
project-{projectId}/{timestamp}-{uuid}-{sanitizedFileName}
```

**Example:**
```
project-123/1703123456789-a1b2c3d4-document.pdf
```

**Advantages:**
- Uniqueness via timestamp + UUID
- Grouping by projects
- Original name preservation (sanitized)

## Security Error Handling

### Exception Hierarchy

```
RuntimeException
├── EntityNotFoundException          (404) - entity not found
├── ResourceNotFoundException        (404) - file not found
├── StorageLimitExceededException    (507) - quota exceeded
├── IllegalArgumentException         (400) - invalid parameters
├── IllegalStateException            (400) - invalid state
└── AccessDeniedException            (403) - access denied
```

### Standardized Responses

All errors returned in unified format:

```json
{
  "errorCode": "ACCESS_DENIED",
  "message": "User 123 does not have permission to access resource 456 in project 789"
}
```

## Storage Quota Control

### Check Mechanism

1. **Pessimistic Locking** at project level
   - Prevents race conditions
   - Guarantees atomicity of check and update

2. **Automatic Recalculation**
   - After each upload/delete
   - Aggregation of active files only
   - Update project `storageSize` field

3. **Validation Before Upload**
   - Check current size + new file size
   - Compare with `maxStorageSize`
   - Error: `StorageLimitExceededException` (507)

### Quota Configuration

- Default: 2GB per project
- Configurable via `maxStorageSize` field in `project` table
- Can be set individually for each project

## Security Recommendations

1. **Use HTTPS** in production
2. **Validate all input data**
3. **Log file operations**
4. **Monitor** suspicious activity
5. **Regular dependency updates**
6. **Request size limits** at web server level
7. **Rate limiting** for API endpoints

## Future Improvements

- Server-side file encryption
- Antivirus scanning integration
- Detailed audit logging
- Two-factor authentication for critical operations
- OAuth2/JWT integration for production
