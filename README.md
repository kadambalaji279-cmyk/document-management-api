# Document Management API

A small REST API for registering users and managing documents with optional file
uploads. It is built with Spring Boot 3 and MySQL 8, uses Flyway for the database
schema, and is intentionally kept simple enough to explain line by line.

> **Backend only:** this project has no frontend. Use curl, Postman, or any HTTP
> client to interact with it.

---

## Features

- User registration with BCrypt password hashing
- Username/password login (demo authentication; no JWT)
- User lookup by id
- Document CRUD with an owner
- Pagination, sorting, and case-insensitive title search
- Multipart file upload/download (10 MB limit)
- Safe file names: UUID-prefixed storage, path traversal protection
- Bean Validation on every request DTO
- One consistent JSON error format
- Flyway migration plus a standalone `schema.sql`
- Unit tests with JUnit 5 and Mockito (no database required)
- Interactive Swagger UI / OpenAPI documentation
- Docker Compose file for MySQL, Postman collection, and Maven wrapper

---

## Tech Stack

| Component | Version / Notes |
|---|---|
| Java | 17+ |
| Spring Boot | 3.5.16 |
| Build | Maven (wrapper included) |
| Web | Spring Web (Spring MVC) |
| Persistence | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation (Hibernate Validator) |
| Database | MySQL 8, `mysql-connector-j` |
| Migrations | Flyway (`flyway-core`, `flyway-mysql`) |
| Passwords | `spring-security-crypto` only (`BCryptPasswordEncoder`) |
| API docs | springdoc-openapi / Swagger UI |
| Tests | JUnit 5 + Mockito |
| Boilerplate | No Lombok; plain getters and setters |

---

## Prerequisites

- **Java 17 or newer** (`java -version`)
- **MySQL 8** — either Docker or a local installation
- No separate Maven installation is needed; `mvnw` downloads Maven on first use
- Internet access on the first build (Maven dependencies and wrapper)

---

## Quick Start

### 1. Start MySQL

**Option A — Docker Compose (recommended if Docker is available):**

```bash
docker compose up -d
```

This starts MySQL 8 with:

- database: `docmanagement`
- username: `root`
- password: `root`
- port: `3306`

The application uses `createDatabaseIfNotExist=true`, so the database is created
automatically if it does not exist. Flyway creates the tables on first startup.

**Option B — Local MySQL 8:**

Start your MySQL server and make sure the credentials match the defaults below,
or override them with environment variables. If the `docmanagement` database does
not exist, the JDBC URL creates it automatically.

If you prefer to create it manually:

```sql
CREATE DATABASE docmanagement
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

> If MySQL is already using port 3306, stop it before running Docker Compose, or
> point `DB_URL` at the port where your MySQL instance is listening.

### 2. Configure environment variables (optional)

The defaults work for a local MySQL with `root` / `root`.

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/docmanagement?createDatabaseIfNotExist=true` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | `root` |
| `UPLOAD_DIR` | `./uploads` |
| `SERVER_PORT` | `8080` |

PowerShell:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="root"
# Optional:
$env:DB_URL="jdbc:mysql://localhost:3306/docmanagement?createDatabaseIfNotExist=true"
```

Bash / Git Bash:

```bash
export DB_USERNAME="root"
export DB_PASSWORD="root"
```

### 3. Run the application

Linux/macOS/Git Bash:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The API starts at **http://localhost:8080**.

On startup:

1. Flyway runs `V1__init.sql`.
2. Hibernate validates the entities against the schema (`ddl-auto=validate`).
3. The upload directory is created if it does not exist.

### 4. Run the tests

```bash
./mvnw clean test
```

Windows:

```powershell
.\mvnw.cmd clean test
```

The tests are service-layer unit tests only. They mock the repositories and file
system dependencies, so **no running database is needed**.

Test coverage:

- `UserServiceTest` — register success/duplicates, BCrypt hashing, login success,
  wrong password, unknown user, user not found
- `DocumentServiceTest` — create, owner not found, get, list/pagination, update,
  delete, upload metadata
- `FileStorageServiceTest` — valid upload in `@TempDir`, empty file rejection,
  path traversal rejection, file deletion

---

## API Overview

Base URL: `http://localhost:8080`

### Users

| Method | Endpoint | Description | Success |
|---|---|---|---|
| POST | `/api/users/register` | Register a user; password is hashed | `201 Created` |
| POST | `/api/users/login` | Verify username and password | `200 OK` |
| GET | `/api/users/{id}` | Get basic user information | `200 OK` |

### Documents

| Method | Endpoint | Description | Success |
|---|---|---|---|
| POST | `/api/documents` | Create a document | `201 Created` |
| GET | `/api/documents/{id}` | Get one document | `200 OK` |
| GET | `/api/documents` | List documents with pagination/sorting/search | `200 OK` |
| PUT | `/api/documents/{id}` | Update title, description, owner | `200 OK` |
| DELETE | `/api/documents/{id}` | Delete document and its file | `204 No Content` |
| POST | `/api/documents/{id}/upload` | Upload/replace a file (`file` part) | `200 OK` |
| GET | `/api/documents/{id}/download` | Download the stored file | `200 OK` |

**List query parameters**

| Parameter | Default | Example |
|---|---|---|
| `page` | `0` | `page=1` |
| `size` | `10` | `size=5` |
| `sort` | `createdAt,desc` | `sort=title,asc` |
| `title` | none | `title=report` (case-insensitive contains) |

Passwords are never included in any response. There is no JWT or session; login
only confirms that the supplied credentials are valid.

---

## Interactive API Documentation (Swagger UI)

Start the application and open:

- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

The UI groups endpoints under **Users** and **Documents**. Click any endpoint,
choose **Try it out**, fill in the JSON/query/file values, and execute the request
from the browser. No authentication token is needed.

For uploads, select a local file in the `file` field. For requests that use an
`id`, create a user and document first (or use the sample ids from your database).

---

## Sample Requests and Responses

The commands below use bash-style quoting. On Windows PowerShell, JSON quotes are
handled differently; use Git Bash/WSL, or put the JSON body in a file and use
`--data-binary "@body.json"` as shown at the end of this section.

### 1. Register

```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"password123"}'
```

`201 Created`

```json
{
  "id": 1,
  "username": "alice",
  "email": "alice@example.com",
  "role": "USER",
  "createdAt": "2026-10-26T15:45:57"
}
```

### 2. Login

```bash
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'
```

`200 OK`

```json
{
  "id": 1,
  "username": "alice",
  "email": "alice@example.com",
  "role": "USER",
  "createdAt": "2026-10-26T15:45:57"
}
```

### 3. Get a user

```bash
curl http://localhost:8080/api/users/1
```

`200 OK` — same user shape as above.

### 4. Create a document

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{"title":"Annual Report","description":"2026 annual report","ownerId":1}'
```

`201 Created`

```json
{
  "id": 1,
  "title": "Annual Report",
  "description": "2026 annual report",
  "fileName": null,
  "contentType": null,
  "fileSize": null,
  "ownerId": 1,
  "ownerUsername": "alice",
  "createdAt": "2026-10-26T15:46:00",
  "updatedAt": "2026-10-26T15:46:00"
}
```

### 5. Get a document

```bash
curl http://localhost:8080/api/documents/1
```

`200 OK` — same document shape.

### 6. List documents

```bash
curl "http://localhost:8080/api/documents?page=0&size=2&sort=title,asc"
```

`200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "title": "Annual Report",
      "description": "2026 annual report",
      "fileName": null,
      "contentType": null,
      "fileSize": null,
      "ownerId": 1,
      "ownerUsername": "alice",
      "createdAt": "2026-10-26T15:46:00",
      "updatedAt": "2026-10-26T15:46:00"
    },
    {
      "id": 2,
      "title": "Meeting Notes",
      "description": "Team meeting notes",
      "fileName": null,
      "contentType": null,
      "fileSize": null,
      "ownerId": 1,
      "ownerUsername": "alice",
      "createdAt": "2026-10-26T15:46:00",
      "updatedAt": "2026-10-26T15:46:00"
    }
  ],
  "page": 0,
  "size": 2,
  "totalElements": 3,
  "totalPages": 2
}
```

Search example:

```bash
curl "http://localhost:8080/api/documents?title=REPORT&page=0&size=10"
```

### 7. Update a document

```bash
curl -X PUT http://localhost:8080/api/documents/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Annual Report 2026","description":"Updated annual report","ownerId":1}'
```

`200 OK`

```json
{
  "id": 1,
  "title": "Annual Report 2026",
  "description": "Updated annual report",
  "fileName": null,
  "contentType": null,
  "fileSize": null,
  "ownerId": 1,
  "ownerUsername": "alice",
  "createdAt": "2026-10-26T15:46:00",
  "updatedAt": "2026-10-26T15:46:04"
}
```

### 8. Upload a file

```bash
curl -X POST http://localhost:8080/api/documents/1/upload \
  -F "file=@./sample.txt"
```

`200 OK`

```json
{
  "id": 1,
  "title": "Annual Report 2026",
  "description": "Updated annual report",
  "fileName": "sample.txt",
  "contentType": "text/plain",
  "fileSize": 40,
  "ownerId": 1,
  "ownerUsername": "alice",
  "createdAt": "2026-10-26T15:46:00",
  "updatedAt": "2026-10-26T15:46:05"
}
```

Uploading a second file to the same document replaces the file and removes the
old file from disk.

### 9. Download a file

```bash
curl -OJ http://localhost:8080/api/documents/1/download
```

`200 OK` with binary content and headers similar to:

```text
Content-Type: text/plain
Content-Length: 40
Content-Disposition: attachment; filename*=UTF-8''sample.txt
```

The original file name is used in the attachment header; the physical file on
disk has a UUID-based name.

### 10. Delete a document

```bash
curl -i -X DELETE http://localhost:8080/api/documents/1
```

`204 No Content` — the database row and the uploaded file are removed.

---

## Error Format

Every error uses the same JSON shape:

```json
{
  "timestamp": "2026-10-26T15:45:58.7509382",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/users/register",
  "fieldErrors": {
    "username": "Username is required",
    "email": "Email must be valid",
    "password": "Password must be between 8 and 72 characters"
  }
}
```

`fieldErrors` is always present; it is an empty object when the error is not a
field validation error.

### Error cases

| Status | Situation |
|---|---|
| `400 Bad Request` | Bean Validation failure, empty file, path traversal name, malformed JSON, invalid id/sort |
| `401 Unauthorized` | Wrong username or password |
| `404 Not Found` | User/document not found, unknown URL |
| `405 Method Not Allowed` | Unsupported HTTP method |
| `409 Conflict` | Duplicate username/email or database constraint |
| `413 Payload Too Large` | Upload exceeds 10 MB |
| `415 Unsupported Media Type` | Unsupported request content type |
| `500 Internal Server Error` | Unexpected server error (details are logged, not returned) |

Examples:

```json
{
  "timestamp": "2026-10-26T15:45:58.063263",
  "status": 409,
  "error": "Conflict",
  "message": "Username already exists: alice",
  "path": "/api/users/register",
  "fieldErrors": {}
}
```

```json
{
  "timestamp": "2026-10-26T15:46:02.0572406",
  "status": 404,
  "error": "Not Found",
  "message": "Document not found with id: 999",
  "path": "/api/documents/999",
  "fieldErrors": {}
}
```

Empty file:

```json
{
  "timestamp": "2026-10-26T15:46:05.7864699",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot store an empty file",
  "path": "/api/documents/2/upload",
  "fieldErrors": {}
}
```

Oversized file:

```json
{
  "timestamp": "2026-10-26T15:46:06.6499466",
  "status": 413,
  "error": "Payload Too Large",
  "message": "File size exceeds the maximum allowed size of 10 MB",
  "path": "/api/documents/2/upload",
  "fieldErrors": {}
}
```

### Sending JSON on Windows PowerShell

PowerShell can strip the quotes from an inline JSON argument. The reliable option
is to keep the body in a file:

```powershell
curl.exe -X POST http://localhost:8080/api/users/register `
  -H "Content-Type: application/json" `
  --data-binary "@register.json"
```

---

## Project Structure

```text
document-management-api/
├── .mvn/wrapper/maven-wrapper.properties
├── mvnw
├── mvnw.cmd
├── pom.xml
├── docker-compose.yml
├── schema.sql
├── postman_collection.json
├── README.md
└── src/
    ├── main/
    │   ├── java/com/ikaansh/docmanagement/
    │   │   ├── DocumentManagementApiApplication.java
    │   │   ├── config/
    │   │   │   ├── PasswordConfig.java
    │   │   │   └── OpenApiConfig.java
    │   │   ├── controller/
    │   │   │   ├── UserController.java
    │   │   │   └── DocumentController.java
    │   │   ├── dto/
    │   │   │   ├── RegisterRequest.java
    │   │   │   ├── LoginRequest.java
    │   │   │   ├── UserResponse.java
    │   │   │   ├── DocumentRequest.java
    │   │   │   ├── DocumentResponse.java
    │   │   │   ├── PageResponse.java
    │   │   │   ├── ApiErrorResponse.java
    │   │   │   └── FileDownload.java
    │   │   ├── entity/
    │   │   │   ├── User.java
    │   │   │   └── Document.java
    │   │   ├── exception/
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   ├── DuplicateResourceException.java
    │   │   │   ├── InvalidCredentialsException.java
    │   │   │   └── FileStorageException.java
    │   │   ├── repository/
    │   │   │   ├── UserRepository.java
    │   │   │   └── DocumentRepository.java
    │   │   └── service/
    │   │       ├── UserService.java
    │   │       ├── DocumentService.java
    │   │       ├── FileStorageService.java
    │   │       └── StoredFile.java
    │   └── resources/
    │       ├── application.properties
    │       └── db/migration/V1__init.sql
    └── test/java/com/ikaansh/docmanagement/service/
        ├── UserServiceTest.java
        ├── DocumentServiceTest.java
        └── FileStorageServiceTest.java
```

---

## Database Schema

Flyway migration: `src/main/resources/db/migration/V1__init.sql`

The same SQL is exported at the repository root as `schema.sql` for submission.

### `users`

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT` | Primary key, auto-increment |
| `username` | `VARCHAR(50)` | Unique, required |
| `email` | `VARCHAR(100)` | Unique, required |
| `password` | `VARCHAR(255)` | BCrypt hash, required |
| `role` | `VARCHAR(20)` | Default `USER` |
| `created_at` | `TIMESTAMP` | Required |
| `updated_at` | `TIMESTAMP` | Required |

### `documents`

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT` | Primary key, auto-increment |
| `title` | `VARCHAR(150)` | Required |
| `description` | `VARCHAR(500)` | Optional |
| `file_name` | `VARCHAR(255)` | Original (sanitized) name, optional |
| `file_path` | `VARCHAR(500)` | Internal storage path, optional |
| `content_type` | `VARCHAR(100)` | Optional |
| `file_size` | `BIGINT` | Bytes, optional |
| `owner_id` | `BIGINT` | Required FK to `users.id` |
| `created_at` | `TIMESTAMP` | Required |
| `updated_at` | `TIMESTAMP` | Required |

Relationship: one user owns many documents. `Document.owner` is a lazy
`@ManyToOne`; `owner_id` is indexed. There is no user-delete endpoint, so no
cascade is needed.

Timestamps are set by `@PrePersist` / `@PreUpdate` in the entities. The SQL
defaults are a safety net. The schema uses second-precision `TIMESTAMP`s, so the
entity callbacks also truncate to whole seconds; the value returned by the API
matches the value stored in MySQL.

`ddl-auto=validate` means Hibernate never changes the schema. To change the
database later, add a new migration (`V2__...sql`) instead of editing V1.

---

## File Storage

- Upload directory: `app.upload-dir` / `UPLOAD_DIR` (default `./uploads`)
- Maximum file size: **10 MB** (`spring.servlet.multipart.max-file-size`)
- Original file names are sanitized; path traversal (`..`) is rejected
- Files are stored as `UUID + safe extension`, so two uploads cannot overwrite
  each other
- `file_name`, `file_path`, `content_type`, and `file_size` are stored on the
  document row
- Uploading a replacement deletes the previous file
- Deleting a document also deletes its file
- On Windows, deletion retries briefly because antivirus scanners can lock a
  newly created file
- File paths are not exposed through the JSON API

The download endpoint streams the stored file with its original name in the
`Content-Disposition` header. The `file_path` column is internal and is never
returned by a controller.

---

## Authentication and Security Notes

- Only `spring-security-crypto` is included, solely for
  `BCryptPasswordEncoder`. There is **no Spring Security filter chain**, no JWT,
  and no session.
- `POST /api/users/login` verifies the credentials and returns basic user info.
  It does not grant a token or protect later requests.
- This is an internship/demo API, not production authentication. In a real
  system, add token-based authentication and per-owner authorization.
- Passwords are never returned in responses.
- Duplicate username/email are checked in the service and enforced by unique
  database constraints.
- Search and repository access use JPA parameters, not string-concatenated SQL.

---

## Postman Collection

Import `postman_collection.json` into Postman.

1. Confirm the `baseUrl` collection variable is `http://localhost:8080`.
2. Run **Users → Register** first. It sets the `userId` variable.
3. Run **Documents → Create Document** next. It sets the `documentId` variable.
4. The other requests reuse those variables.
5. For **Upload File**, select a file manually if Postman cannot resolve the
   placeholder `README.md` path.
6. A fresh registration is required; running Register twice returns `409` by
   design. Use a different username/email or reset the database before re-running.

The collection includes happy-path requests for every endpoint and sample error
requests for `400`, `401`, `404`, `409`, and `415`.

---

## Verification

The project was verified with:

- `.\mvnw.cmd clean test` — **23 unit tests, 0 failures**
- A full end-to-end curl run against **MySQL 8.0.45** — **94 assertions, 0 failures**
- Flyway applied `V1` successfully and Hibernate schema validation passed
- All endpoints, pagination/sorting/search, upload/download, replacement cleanup,
  delete cleanup, validation, duplicate/401/404/405/413/415 errors, and BCrypt
  storage in MySQL were checked
- The downloaded file hash matched the uploaded file byte-for-byte
- `docker compose config` validated the Compose file (the verification machine
  used a local MySQL 8 installation instead of the Docker daemon)

---

## Useful Commands

```bash
# Run the app
./mvnw spring-boot:run

# Run tests
./mvnw clean test

# Build the jar
./mvnw clean package

# Run the built jar
java -jar target/document-management-api-0.0.1-SNAPSHOT.jar

# Start/stop MySQL with Docker
docker compose up -d
docker compose down

# Remove the database volume as well
docker compose down -v
```

Windows equivalents use `.\mvnw.cmd` instead of `./mvnw`.
