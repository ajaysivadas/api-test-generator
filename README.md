# API Test Automation Agent

An AI-driven tool that converts API contracts (OpenAPI/Swagger/Postman) into a complete **REST Assured + TestNG** automation framework, packaged as a downloadable Maven project. Includes a React dashboard for uploading specs, previewing generated code, comparing schema versions, and downloading the output.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Usage Guide](#usage-guide)
- [API Reference](#api-reference)
- [Generated Output](#generated-output)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)

---

## Features

- **Multi-format parsing** - Supports OpenAPI 3.0, Swagger 2.0 (YAML/JSON), and Postman Collection v2.1 (JSON)
- **POJO generation** - Java model classes with Lombok `@Data`, `@Builder` and Jackson annotations
- **Test generation** - REST Assured test classes with positive and negative test cases per endpoint
- **Framework scaffolding** - Complete Maven project with BaseTest, TestNG XML, config properties, and utility classes
- **Schema comparison** - Diff two API spec versions to detect breaking vs non-breaking changes (endpoints, fields, types, required status)
- **ZIP packaging** - One-click download of the entire generated framework as a ZIP archive
- **React dashboard** - Drag-and-drop upload, endpoint/schema preview, diff visualization, generation progress, and download history

---

## Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Runtime |
| Spring Boot | 3.2.2 | Web framework |
| Swagger Parser | 2.1.20 | OpenAPI/Swagger parsing |
| FreeMarker | (via Spring Boot) | Code generation templates |
| Jackson | (via Spring Boot) | JSON/YAML processing |
| Apache Commons IO | 2.15.1 | File utilities |
| Lombok | 1.18.30 | Boilerplate reduction |
| Maven | 3.x | Build tool |

### Frontend
| Technology | Version | Purpose |
|---|---|---|
| React | 18.2 | UI framework |
| Vite | 5.1 | Build tool & dev server |
| Tailwind CSS | 3.4 | Styling |
| Axios | 1.6 | HTTP client |
| react-dropzone | 14.2 | File drag-and-drop |
| react-router-dom | 6.22 | Client-side routing |

### Generated Output
| Technology | Purpose |
|---|---|
| REST Assured 5.4 | API test assertions |
| TestNG 7.9 | Test runner |
| Lombok 1.18 | Model boilerplate |
| Jackson 2.16 | JSON serialization |
| Allure 2.25 | Test reporting |

---

## Project Structure

```
api-automation-agent/
├── README.md
├── docs/
│   ├── API_REFERENCE.md          # Detailed API endpoint documentation
│   ├── ARCHITECTURE.md           # System design and data flow
│   └── FRONTEND_GUIDE.md         # Frontend components and development guide
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/apiautomation/agent/
│       │   ├── AgentApplication.java
│       │   ├── config/
│       │   │   └── CorsConfig.java
│       │   ├── controller/
│       │   │   └── ApiContractController.java
│       │   ├── model/
│       │   │   ├── ApiSpec.java
│       │   │   ├── Endpoint.java
│       │   │   ├── SchemaDefinition.java
│       │   │   ├── FieldDefinition.java
│       │   │   ├── SchemaChange.java
│       │   │   ├── ChangeType.java
│       │   │   └── ComparisonResult.java
│       │   └── service/
│       │       ├── ParserService.java            (interface)
│       │       ├── OpenApiParserService.java
│       │       ├── PostmanParserService.java
│       │       ├── GeneratorService.java
│       │       ├── PojoGeneratorService.java
│       │       ├── TestGeneratorService.java
│       │       ├── FrameworkGeneratorService.java
│       │       ├── SchemaComparisonService.java
│       │       └── PackagingService.java
│       └── resources/
│           ├── application.properties
│           └── templates/
│               ├── pojo.ftl
│               ├── test-class.ftl
│               ├── base-test.ftl
│               ├── config.ftl
│               ├── pom.ftl
│               └── testng.ftl
└── frontend/
    ├── package.json
    ├── vite.config.js
    ├── tailwind.config.js
    ├── index.html
    └── src/
        ├── main.jsx
        ├── App.jsx
        ├── index.css
        ├── components/
        │   ├── Layout.jsx
        │   ├── Sidebar.jsx
        │   ├── FileUpload.jsx
        │   ├── GenerationProgress.jsx
        │   ├── SchemaChanges.jsx
        │   ├── CodePreview.jsx
        │   └── DownloadButton.jsx
        ├── pages/
        │   ├── Dashboard.jsx
        │   ├── Generate.jsx
        │   ├── Compare.jsx
        │   └── History.jsx
        └── services/
            └── api.js
```

---

## Prerequisites

- **Java 17+** (JDK)
- **Maven 3.6+**
- **Node.js 18+** and **npm 9+**

Verify installations:
```bash
java -version
mvn -version
node -v
npm -v
```

---

## Getting Started

### 1. Clone and navigate

```bash
cd api-automation-agent
```

### 2. Start the backend

```bash
cd backend
mvn spring-boot:run
```

The backend starts at **http://localhost:8080**.

### 3. Start the frontend

Open a new terminal:

```bash
cd frontend
npm install
npm run dev
```

The dashboard opens at **http://localhost:5173**.

The Vite dev server proxies all `/api` requests to the backend at port 8080.

### 4. Build for production (optional)

```bash
# Backend
cd backend
mvn clean package -DskipTests
java -jar target/api-automation-agent-1.0.0-SNAPSHOT.jar

# Frontend
cd frontend
npm run build
# Serve the dist/ folder with any static server
```

---

## Usage Guide

### Upload and Preview an API Spec

1. Open the dashboard at `http://localhost:5173`
2. On the **Dashboard** page, drag-and-drop or click to upload an OpenAPI/Swagger YAML/JSON file or a Postman Collection JSON file
3. The parsed spec is displayed with:
   - API title, version, format
   - Number of endpoints and schemas
   - Tabbed view of endpoints (with method badges), schemas (with field types), and raw JSON

### Generate a Test Framework

1. Navigate to the **Generate** page
2. Upload your API spec
3. Optionally change the **Base Package Name** (default: `com.example.api`)
4. Click **Generate Framework**
5. Watch the real-time progress indicator
6. Once complete, review the summary (POJO count, test class count, endpoint coverage)
7. Click **Download Framework** to get a ZIP file

### Compare Two API Versions

1. Navigate to the **Compare** page
2. Upload the **old** spec on the left
3. Upload the **new** spec on the right
4. Click **Compare Specs**
5. Review the results:
   - Summary cards: total, breaking, and non-breaking changes
   - Detailed change list with type badges (ADDED, REMOVED, MODIFIED, TYPE_CHANGED) and severity indicators

### View Generation History

1. Navigate to the **History** page
2. View all past generation runs with timestamps
3. Click **Download Framework** on any entry to re-download

---

## API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/parse` | Upload and parse an API spec file |
| `POST` | `/api/generate` | Generate a full automation framework |
| `POST` | `/api/compare` | Compare two API spec versions |
| `GET` | `/api/download/{id}` | Download a generated framework ZIP |
| `GET` | `/api/history` | Retrieve generation history |

See [docs/API_REFERENCE.md](docs/API_REFERENCE.md) for full request/response documentation.

---

## Generated Output

The downloaded ZIP contains a ready-to-use Maven project:

```
generated-framework/
├── pom.xml                              # Maven dependencies (REST Assured, TestNG, Lombok, Allure)
├── testng.xml                           # TestNG suite configuration
├── src/
│   ├── main/java/com/example/api/
│   │   ├── models/                      # Generated POJOs (@Data, @Builder, @JsonProperty)
│   │   │   ├── User.java
│   │   │   ├── Product.java
│   │   │   └── ...
│   │   └── config/
│   │       └── ApiConfig.java           # Configuration loader singleton
│   └── test/java/com/example/api/
│       ├── base/
│       │   └── BaseTest.java            # REST Assured setup, auth config, request spec
│       ├── tests/
│       │   ├── UserApiTest.java          # Positive + negative tests per endpoint
│       │   ├── ProductApiTest.java
│       │   └── ...
│       └── utils/
│           └── TestUtils.java           # JSON helpers, random data generators
└── src/test/resources/
    └── config.properties                # Base URL, auth, timeouts
```

### Running the generated tests

```bash
cd generated-framework
# Edit src/test/resources/config.properties with your API base URL
mvn clean test
```

---

## Configuration

### Backend (`application.properties`)

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | Server port |
| `spring.servlet.multipart.max-file-size` | `10MB` | Max upload file size |
| `spring.servlet.multipart.max-request-size` | `10MB` | Max request size |
| `agent.output.dir` | `${java.io.tmpdir}/api-automation-agent` | Directory for generated output |

### Frontend (`vite.config.js`)

| Setting | Default | Description |
|---|---|---|
| `server.port` | `5173` | Dev server port |
| `server.proxy./api.target` | `http://localhost:8080` | Backend proxy target |

---

## Troubleshooting

| Problem | Solution |
|---|---|
| Backend fails to start | Ensure Java 17+ is installed. Run `java -version` to verify. |
| Frontend `npm install` fails | Ensure Node.js 18+ is installed. Delete `node_modules` and `package-lock.json`, then retry. |
| Upload returns "Unsupported file format" | Ensure the file is a valid OpenAPI 3.0/Swagger 2.0 YAML/JSON or Postman Collection v2.1 JSON. |
| CORS errors in browser | The backend includes CORS config for `localhost:5173` and `localhost:3000`. If using a different port, update `CorsConfig.java`. |
| Download returns 404 | The generation output is stored in a temp directory. If the server restarted, previous generation IDs are lost. Re-generate. |
| Generated tests fail to compile | Ensure you're using Java 17+ and that Lombok is configured in your IDE (annotation processing enabled). |
