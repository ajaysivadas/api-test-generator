# API Reference

Base URL: `http://localhost:8080`

All endpoints are prefixed with `/api`.

---

## Endpoints Overview

| Method | Path | Content-Type | Description |
|--------|------|-------------|-------------|
| POST | `/api/parse` | `multipart/form-data` | Parse an API spec and return structured data |
| POST | `/api/generate` | `multipart/form-data` | Generate a REST Assured + TestNG framework |
| POST | `/api/compare` | `multipart/form-data` | Compare two API spec versions |
| GET | `/api/download/{id}` | - | Download a generated framework as ZIP |
| GET | `/api/history` | - | List all past generation runs |

---

## POST `/api/parse`

Upload an API specification file and receive a parsed breakdown of endpoints and schemas.

### Request

- **Content-Type:** `multipart/form-data`
- **Form Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | Yes | OpenAPI (YAML/JSON), Swagger (YAML/JSON), or Postman Collection (JSON) |

### cURL Example

```bash
curl -X POST http://localhost:8080/api/parse \
  -F "file=@petstore.yaml"
```

### Success Response (200)

```json
{
  "success": true,
  "title": "Petstore API",
  "version": "1.0.0",
  "sourceFormat": "OpenAPI 3.0",
  "endpointCount": 5,
  "schemaCount": 3,
  "endpoints": [
    {
      "path": "/pets",
      "method": "GET",
      "operationId": "listPets",
      "summary": "List all pets",
      "tag": "pets",
      "pathParameters": [],
      "queryParameters": [
        {
          "name": "limit",
          "type": "integer",
          "javaType": "Integer",
          "required": false
        }
      ],
      "requestBody": null,
      "responses": {
        "200": {
          "name": "Pet",
          "fields": []
        }
      },
      "contentType": "application/json",
      "securitySchemes": []
    }
  ],
  "schemas": [
    {
      "name": "Pet",
      "description": "A pet in the store",
      "fields": [
        {
          "name": "id",
          "type": "integer",
          "javaType": "Long",
          "format": "int64",
          "required": true,
          "isArray": false,
          "description": "Unique identifier",
          "example": "1"
        },
        {
          "name": "name",
          "type": "string",
          "javaType": "String",
          "format": null,
          "required": true,
          "isArray": false,
          "description": "Pet name",
          "example": "Fido"
        }
      ]
    }
  ]
}
```

### Error Response (400)

```json
{
  "success": false,
  "error": "Failed to parse OpenAPI spec: attribute openapi is missing"
}
```

---

## POST `/api/generate`

Upload an API spec to generate a full REST Assured + TestNG Maven project. Returns generation metadata including the `generationId` used for downloading.

### Request

- **Content-Type:** `multipart/form-data`
- **Form Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | API specification file |
| `basePackage` | String | No | `com.example.api` | Java package name for generated classes |

### cURL Example

```bash
curl -X POST http://localhost:8080/api/generate \
  -F "file=@petstore.yaml" \
  -F "basePackage=com.mycompany.api"
```

### Success Response (200)

```json
{
  "generationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "outputDir": "/tmp/api-automation-agent/a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "pojoCount": 3,
  "testCount": 2,
  "endpointCount": 5,
  "schemaCount": 3,
  "success": true
}
```

### Error Response (400)

```json
{
  "success": false,
  "error": "Unsupported file format. Please upload an OpenAPI (YAML/JSON) or Postman Collection (JSON) file."
}
```

### What Gets Generated

The generation creates a complete Maven project at the `outputDir` path:

| Path | Description |
|------|-------------|
| `pom.xml` | Maven POM with REST Assured, TestNG, Lombok, Allure dependencies |
| `testng.xml` | TestNG suite file referencing all generated test classes |
| `src/main/java/{package}/models/*.java` | POJO classes with `@Data`, `@Builder`, `@JsonProperty` |
| `src/main/java/{package}/config/ApiConfig.java` | Singleton configuration loader |
| `src/test/java/{package}/base/BaseTest.java` | Base class with REST Assured setup and auth handling |
| `src/test/java/{package}/tests/*ApiTest.java` | Test classes grouped by tag/path |
| `src/test/java/{package}/utils/TestUtils.java` | JSON deserialization and random data helpers |
| `src/test/resources/config.properties` | Base URL, auth, and timeout configuration |

---

## POST `/api/compare`

Upload two API spec files (old and new) to detect schema changes between versions.

### Request

- **Content-Type:** `multipart/form-data`
- **Form Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `oldFile` | File | Yes | Previous version of the API spec |
| `newFile` | File | Yes | New version of the API spec |

### cURL Example

```bash
curl -X POST http://localhost:8080/api/compare \
  -F "oldFile=@petstore-v1.yaml" \
  -F "newFile=@petstore-v2.yaml"
```

### Success Response (200)

```json
{
  "success": true,
  "oldSpecTitle": "Petstore API",
  "newSpecTitle": "Petstore API",
  "totalChanges": 4,
  "breakingChanges": 1,
  "nonBreakingChanges": 3,
  "changes": [
    {
      "changeType": "ADDED",
      "category": "Endpoint",
      "path": "PATCH /pets/{petId}",
      "description": "New endpoint added: PATCH /pets/{petId}",
      "oldValue": null,
      "newValue": "PATCH /pets/{petId}",
      "severity": "NON-BREAKING"
    },
    {
      "changeType": "REMOVED",
      "category": "Schema Field",
      "path": "Pet.status",
      "description": "Field removed from Pet: status",
      "oldValue": "status (string)",
      "newValue": null,
      "severity": "BREAKING"
    },
    {
      "changeType": "TYPE_CHANGED",
      "category": "Schema Field",
      "path": "Pet.age",
      "description": "Type changed in Pet.age",
      "oldValue": "string",
      "newValue": "integer",
      "severity": "BREAKING"
    },
    {
      "changeType": "ADDED",
      "category": "Schema Field",
      "path": "Pet.breed",
      "description": "New field added to Pet: breed",
      "oldValue": null,
      "newValue": "breed (string)",
      "severity": "NON-BREAKING"
    }
  ]
}
```

### Change Types

| Change Type | Description |
|---|---|
| `ADDED` | New endpoint, parameter, schema, or field |
| `REMOVED` | Removed endpoint, parameter, schema, or field |
| `MODIFIED` | Changed property (e.g., required status) |
| `TYPE_CHANGED` | Field or parameter type changed |

### Severity Levels

| Severity | Meaning |
|---|---|
| `BREAKING` | Removal of endpoints/fields, type changes, adding required fields |
| `NON-BREAKING` | Adding optional fields, new endpoints, new schemas |

### Categories

| Category | What is compared |
|---|---|
| `Endpoint` | HTTP method + path combinations |
| `Parameter` | Path and query parameters |
| `Request Body` | Request body presence |
| `Schema` | Top-level schema definitions |
| `Schema Field` | Individual fields within schemas |

---

## GET `/api/download/{id}`

Download a previously generated framework as a ZIP file.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | String (UUID) | The `generationId` returned by `/api/generate` |

### cURL Example

```bash
curl -o framework.zip http://localhost:8080/api/download/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

### Success Response (200)

- **Content-Type:** `application/octet-stream`
- **Content-Disposition:** `attachment; filename=generated-framework.zip`
- **Body:** Binary ZIP data

### Error Response (404)

Returned when the generation ID is not found (e.g., after a server restart).

---

## GET `/api/history`

Retrieve a list of all generation runs from the current server session, ordered by most recent first.

### cURL Example

```bash
curl http://localhost:8080/api/history
```

### Success Response (200)

```json
[
  {
    "generationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "outputDir": "/tmp/api-automation-agent/a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "pojoCount": 3,
    "testCount": 2,
    "endpointCount": 5,
    "schemaCount": 3,
    "timestamp": "2026-02-23T14:30:00.123",
    "specTitle": "Petstore API",
    "sourceFormat": "OpenAPI 3.0",
    "basePackage": "com.example.api"
  }
]
```

Returns an empty array `[]` if no generations have been performed.

---

## Supported Input Formats

### OpenAPI 3.0

- File extensions: `.yaml`, `.yml`, `.json`
- Must contain the `openapi` field (e.g., `openapi: "3.0.0"`)
- Schemas are extracted from `components.schemas`
- Endpoints are extracted from `paths`

### Swagger 2.0

- File extensions: `.yaml`, `.yml`, `.json`
- Must contain the `swagger` field (e.g., `swagger: "2.0"`)
- Automatically converted to OpenAPI 3.0 internally by the Swagger Parser

### Postman Collection v2.1

- File extension: `.json`
- Must contain `info.schema` with a URL containing "postman"
- Folders map to endpoint tags
- Request bodies are analyzed to infer schemas from JSON examples
- Path variables with `:paramName` syntax are converted to `{paramName}`

---

## Error Handling

All endpoints return a consistent error format:

```json
{
  "success": false,
  "error": "Human-readable error message"
}
```

| HTTP Status | Meaning |
|---|---|
| 200 | Success |
| 400 | Bad request (invalid file, unsupported format, parse error) |
| 404 | Resource not found (invalid generation ID) |
| 413 | File too large (exceeds 10MB limit) |

---

## CORS Configuration

The backend allows cross-origin requests from:
- `http://localhost:5173` (Vite dev server)
- `http://localhost:3000` (alternative dev server)

To add additional origins, edit `backend/src/main/java/com/apiautomation/agent/config/CorsConfig.java`.
