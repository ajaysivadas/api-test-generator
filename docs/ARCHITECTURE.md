# Architecture & Design

This document describes the system architecture, service design, data flow, and key design decisions of the API Test Automation Agent.

---

## System Architecture

```
┌───────────────────────────────────────────────────────────────────────┐
│                     React Frontend (Port 5173)                        │
│                                                                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │Dashboard │  │ Generate │  │ Compare  │  │ History  │             │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘             │
│       │              │              │              │                   │
│       └──────────────┴──────────────┴──────────────┘                  │
│                              │                                        │
│                     Axios HTTP Client                                 │
│                     (services/api.js)                                 │
└──────────────────────────────┬────────────────────────────────────────┘
                               │ REST API (Vite proxy → :8080)
                               ▼
┌───────────────────────────────────────────────────────────────────────┐
│                   Spring Boot Backend (Port 8080)                     │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │                    ApiContractController                        │  │
│  │  POST /api/parse  │ POST /api/generate │ POST /api/compare     │  │
│  │  GET /api/download/{id}  │  GET /api/history                   │  │
│  └──────────┬────────────────┬──────────────────┬─────────────────┘  │
│             │                │                  │                     │
│  ┌──────────▼──────────┐     │     ┌────────────▼───────────────┐    │
│  │   Parser Services   │     │     │ Schema Comparison Service  │    │
│  │                     │     │     │                            │    │
│  │ ┌─────────────────┐ │     │     │ - Endpoint diff            │    │
│  │ │ OpenAPI Parser   │ │     │     │ - Schema field diff        │    │
│  │ │ (Swagger Parser) │ │     │     │ - Type change detection    │    │
│  │ └─────────────────┘ │     │     │ - Breaking/non-breaking    │    │
│  │ ┌─────────────────┐ │     │     └────────────────────────────┘    │
│  │ │ Postman Parser   │ │     │                                      │
│  │ │ (Jackson)        │ │     │                                      │
│  │ └─────────────────┘ │     │                                      │
│  └──────────┬──────────┘     │                                      │
│             │                │                                      │
│             ▼                ▼                                       │
│  ┌─────────────────────────────────────┐                            │
│  │         Generator Service           │                            │
│  │                                     │                            │
│  │ ┌─────────────┐ ┌────────────────┐  │                            │
│  │ │ POJO Gen    │ │ Test Gen       │  │                            │
│  │ │ (pojo.ftl)  │ │(test-class.ftl)│  │                            │
│  │ └─────────────┘ └────────────────┘  │                            │
│  │ ┌─────────────────────────────────┐ │                            │
│  │ │ Framework Gen                   │ │                            │
│  │ │ (base-test, config, pom, testng)│ │                            │
│  │ └─────────────────────────────────┘ │                            │
│  └──────────────────┬──────────────────┘                            │
│                     │                                                │
│                     ▼                                                │
│  ┌──────────────────────────┐    ┌────────────────────┐             │
│  │ FreeMarker Template      │    │ Packaging Service   │             │
│  │ Engine                   │    │ (ZipOutputStream)   │             │
│  │                          │    │                     │             │
│  │ .ftl templates →         │    │ Directory → ZIP     │             │
│  │ .java / .xml / .properties│   │ for download        │             │
│  └──────────────────────────┘    └────────────────────┘             │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │ In-Memory History Store (ConcurrentHashMap)              │       │
│  │ generationId → { metadata, timestamps, counts }          │       │
│  └──────────────────────────────────────────────────────────┘       │
└──────────────────────────────────────────────────────────────────────┘
                     │
                     ▼
         ┌─────────────────────┐
         │  Temp Filesystem    │
         │  /tmp/api-agent/    │
         │  ├── {uuid1}/       │
         │  │   ├── pom.xml    │
         │  │   ├── testng.xml │
         │  │   └── src/...    │
         │  └── {uuid2}/       │
         └─────────────────────┘
```

---

## Data Flow

### Flow 1: Parse API Spec

```
User uploads file
       │
       ▼
Controller receives MultipartFile
       │
       ▼
Controller reads file content as UTF-8 string
       │
       ▼
Auto-detect format:
  ├── Postman? (has "info.schema" containing "postman") → PostmanParserService
  └── OpenAPI/Swagger? (contains "openapi" or "swagger") → OpenApiParserService
       │
       ▼
Parser returns ApiSpec (unified model)
       │
       ▼
Controller serializes ApiSpec to JSON response
```

### Flow 2: Generate Framework

```
User uploads file + provides basePackage
       │
       ▼
Controller parses file → ApiSpec (same as Flow 1)
       │
       ▼
GeneratorService.generate(spec, basePackage)
       │
       ├── Creates UUID-based output directory in temp folder
       ├── Creates Maven project directory structure
       │
       ├── PojoGeneratorService.generatePojos()
       │   └── For each schema: apply pojo.ftl → {ClassName}.java
       │
       ├── TestGeneratorService.generateTests()
       │   ├── Group endpoints by tag/path → test class names
       │   └── For each group: apply test-class.ftl → {Name}ApiTest.java
       │
       └── FrameworkGeneratorService
           ├── generateBaseTest()     → base-test.ftl → BaseTest.java
           ├── generateApiConfig()    → config.ftl → ApiConfig.java
           ├── generateTestUtils()    → inline template → TestUtils.java
           ├── generatePom()          → pom.ftl → pom.xml
           ├── generateTestNg()       → testng.ftl → testng.xml
           └── generateConfigProperties() → inline → config.properties
       │
       ▼
Returns generationId + counts
       │
       ▼
Controller stores entry in history map
```

### Flow 3: Compare Specs

```
User uploads oldFile + newFile
       │
       ▼
Controller parses both files → oldSpec, newSpec
       │
       ▼
SchemaComparisonService.compare(oldSpec, newSpec)
       │
       ├── compareEndpoints()
       │   ├── Map endpoints by "METHOD /path" key
       │   ├── Detect added endpoints (in new, not in old)
       │   ├── Detect removed endpoints (in old, not in new)
       │   └── For shared endpoints: compareEndpointDetails()
       │       ├── Compare path parameters
       │       ├── Compare query parameters
       │       └── Compare request body existence
       │
       └── compareSchemas()
           ├── Map schemas by name
           ├── Detect added/removed schemas
           └── For shared schemas: compareSchemaFields()
               ├── Detect added/removed fields
               ├── Detect type changes
               └── Detect required status changes
       │
       ▼
Returns ComparisonResult with change list + severity counts
```

### Flow 4: Download ZIP

```
User clicks download with generationId
       │
       ▼
PackagingService.packageAsZip(generationId)
       │
       ├── Locate directory: /tmp/api-agent/{generationId}
       ├── Walk file tree
       └── Write all files into ZipOutputStream with "generated-framework/" prefix
       │
       ▼
Return byte[] as application/octet-stream response
```

---

## Internal Models

### Unified API Representation

The parser services convert any supported format into a unified internal model:

```
ApiSpec
├── title: String
├── version: String
├── description: String
├── basePath: String
├── sourceFormat: String ("OpenAPI 3.0" | "Swagger 2.0" | "Postman Collection v2.1")
├── endpoints: List<Endpoint>
│   ├── path: String ("/users/{id}")
│   ├── method: String ("GET", "POST", etc.)
│   ├── operationId: String
│   ├── summary: String
│   ├── tag: String (used for test class grouping)
│   ├── pathParameters: List<FieldDefinition>
│   ├── queryParameters: List<FieldDefinition>
│   ├── requestBody: SchemaDefinition (nullable)
│   ├── responses: Map<statusCode, SchemaDefinition>
│   ├── contentType: String
│   └── securitySchemes: List<String>
└── schemas: List<SchemaDefinition>
    ├── name: String
    ├── description: String
    └── fields: List<FieldDefinition>
        ├── name: String
        ├── type: String (OpenAPI type: "string", "integer", "array<Pet>")
        ├── javaType: String (mapped: "String", "Integer", "List<Pet>")
        ├── format: String ("int64", "date-time", "uuid")
        ├── required: boolean
        ├── isArray: boolean
        ├── refSchema: String (referenced schema name)
        ├── description: String
        └── example: String
```

### Schema Change Model

```
ComparisonResult
├── oldSpecTitle: String
├── newSpecTitle: String
├── changes: List<SchemaChange>
│   ├── changeType: ChangeType (ADDED | REMOVED | MODIFIED | TYPE_CHANGED)
│   ├── category: String ("Endpoint" | "Parameter" | "Request Body" | "Schema" | "Schema Field")
│   ├── path: String (location of the change)
│   ├── description: String (human-readable)
│   ├── oldValue: String
│   ├── newValue: String
│   └── severity: String ("BREAKING" | "NON-BREAKING")
├── totalChanges: int
├── breakingChanges: int
└── nonBreakingChanges: int
```

---

## Service Layer Design

### Parser Services

Interface-based design with two implementations:

```
ParserService (interface)
├── canParse(content, filename): boolean    // Format detection
└── parse(content, filename): ApiSpec       // Parse to unified model

Implementations:
├── OpenApiParserService    — uses Swagger Parser library (io.swagger.parser.v3)
└── PostmanParserService    — uses Jackson for manual JSON traversal
```

**Format detection order:**
1. Check PostmanParserService first (looks for `info.schema` containing "postman")
2. Fall back to OpenApiParserService (looks for `openapi` or `swagger` keywords)

**OpenAPI type-to-Java mapping:**

| OpenAPI Type | Format | Java Type |
|---|---|---|
| `string` | - | `String` |
| `string` | `date` | `LocalDate` |
| `string` | `date-time` | `LocalDateTime` |
| `string` | `uuid` | `UUID` |
| `integer` | - | `Integer` |
| `integer` | `int64` | `Long` |
| `number` | - | `Double` |
| `number` | `float` | `Float` |
| `boolean` | - | `Boolean` |
| `object` | - | `Object` |
| `array` of T | - | `List<T>` |

### Generator Services

Orchestrated by `GeneratorService`:

| Service | Responsibility | Template(s) |
|---|---|---|
| `PojoGeneratorService` | Generate Java model classes | `pojo.ftl` |
| `TestGeneratorService` | Generate REST Assured test classes | `test-class.ftl` |
| `FrameworkGeneratorService` | Generate infrastructure files | `base-test.ftl`, `config.ftl`, `pom.ftl`, `testng.ftl` |

**Test class grouping:** Endpoints are grouped by their `tag` (from OpenAPI tags or Postman folders). Each group produces one test class. If no tag exists, the first non-parameterized path segment is used.

**Test method naming:** Uses `operationId` if available. Otherwise, constructs from method + path (e.g., `GET /users/{id}` becomes `getUsersId`).

### Schema Comparison Service

Stateless comparison between two `ApiSpec` instances. Comparison is keyed by:
- **Endpoints:** `METHOD /path` (e.g., `GET /users`)
- **Schemas:** Schema name
- **Fields:** Field name within a schema

**Breaking change rules:**

| Change | Severity |
|---|---|
| Endpoint removed | BREAKING |
| Endpoint added | NON-BREAKING |
| Required field removed | BREAKING |
| Optional field removed | BREAKING |
| Required field added | BREAKING |
| Optional field added | NON-BREAKING |
| Field type changed | BREAKING |
| Required status: optional → required | BREAKING |
| Required status: required → optional | NON-BREAKING |
| Schema removed | BREAKING |
| Schema added | NON-BREAKING |
| Request body added/removed | BREAKING |

### Packaging Service

Walks the generated output directory and creates a ZIP archive with:
- Root folder: `generated-framework/`
- All files with their relative paths preserved
- Forward-slash path separators for cross-platform compatibility

---

## FreeMarker Templates

Located in `backend/src/main/resources/templates/`.

### `pojo.ftl` — Java Model Class

**Input model:**
| Variable | Type | Description |
|---|---|---|
| `packageName` | String | e.g., `com.example.api.models` |
| `className` | String | PascalCase class name |
| `description` | String | Class-level Javadoc |
| `fields` | List | Field objects with `name`, `javaType`, `required`, `description`, `jsonName` |
| `imports` | Set | Additional imports (e.g., `java.util.List`) |

**Output:** `@Data @Builder @NoArgsConstructor @AllArgsConstructor` class with `@JsonProperty` on each field.

### `test-class.ftl` — REST Assured Test Class

**Input model:**
| Variable | Type | Description |
|---|---|---|
| `packageName` | String | e.g., `com.example.api.tests` |
| `basePackage` | String | Root package for imports |
| `className` | String | Test class name (e.g., `UserApiTest`) |
| `endpoints` | List | Endpoint objects with method, path, params, status code |

**Output:** Two tests per endpoint — positive (expected status code) and negative (invalid request).

### `base-test.ftl` — BaseTest Configuration

**Output:** `BaseTest` class with `@BeforeSuite` config loading and `@BeforeClass` REST Assured setup, including Bearer and Basic auth support.

### `config.ftl` — ApiConfig Singleton

**Output:** Thread-safe singleton that loads `config.properties` with getters for common settings.

### `pom.ftl` — Maven POM

**Output:** Complete POM with REST Assured, TestNG, Jackson, Lombok, Allure, and Maven Surefire plugin configuration.

### `testng.ftl` — TestNG Suite XML

**Output:** Suite configuration with parallel class execution, referencing all generated test classes.

---

## Data Storage

The current implementation uses:
- **Temp filesystem** for generated output (`/tmp/api-automation-agent/{uuid}/`)
- **In-memory `ConcurrentHashMap`** for generation history

Both are ephemeral — data is lost on server restart. For production use, consider adding:
- A database (H2/PostgreSQL) for history persistence
- Object storage (S3/MinIO) for generated artifacts

---

## Security Considerations

- **File upload validation:** Accepts only JSON/YAML content. Maximum file size is 10MB (configured in `application.properties`).
- **CORS:** Restricted to `localhost:5173` and `localhost:3000`. Update for production deployments.
- **No authentication:** The agent is designed for internal/local use. Add Spring Security for production.
- **Temp file cleanup:** The `PackagingService` provides a `cleanup()` method. Not currently called automatically — consider a scheduled task for production.

---

## Extending the System

### Adding a new parser (e.g., RAML)

1. Create `RamlParserService implements ParserService`
2. Implement `canParse()` to detect RAML content
3. Implement `parse()` to return an `ApiSpec`
4. Add `@Service` annotation — Spring auto-discovers it
5. Inject into `ApiContractController` and add to the detection chain in `parseContent()`

### Adding a new template

1. Create `new-template.ftl` in `src/main/resources/templates/`
2. Add a generation method in the appropriate service
3. Call it from `GeneratorService.generate()`

### Adding a new output language (e.g., Python/pytest)

1. Create new FreeMarker templates for Python test files
2. Create a new generator service (e.g., `PythonTestGeneratorService`)
3. Add a `language` parameter to the `/api/generate` endpoint
4. Route to the appropriate generator based on the parameter
