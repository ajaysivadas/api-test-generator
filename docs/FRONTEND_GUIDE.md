# Frontend Guide

This document covers the React dashboard architecture, component reference, and development instructions.

---

## Overview

The frontend is a single-page React application built with Vite and styled with Tailwind CSS. It provides a SaaS-style dashboard for interacting with the API Automation Agent backend.

---

## Tech Stack

| Library | Version | Purpose |
|---|---|---|
| React | 18.2 | UI rendering |
| React Router DOM | 6.22 | Client-side routing |
| Vite | 5.1 | Dev server and bundler |
| Tailwind CSS | 3.4 | Utility-first CSS |
| Axios | 1.6 | HTTP requests |
| react-dropzone | 14.2 | Drag-and-drop file upload |
| react-syntax-highlighter | 15.5 | Code display (available for future use) |

---

## Directory Structure

```
frontend/
├── index.html                  # HTML entry point
├── package.json                # Dependencies and scripts
├── vite.config.js              # Vite configuration + API proxy
├── tailwind.config.js          # Tailwind CSS configuration
├── postcss.config.js           # PostCSS plugins
└── src/
    ├── main.jsx                # React root + BrowserRouter
    ├── App.jsx                 # Route definitions
    ├── index.css               # Tailwind directives + global styles
    ├── components/             # Reusable UI components
    │   ├── Layout.jsx
    │   ├── Sidebar.jsx
    │   ├── FileUpload.jsx
    │   ├── GenerationProgress.jsx
    │   ├── SchemaChanges.jsx
    │   ├── CodePreview.jsx
    │   └── DownloadButton.jsx
    ├── pages/                  # Route-level page components
    │   ├── Dashboard.jsx
    │   ├── Generate.jsx
    │   ├── Compare.jsx
    │   └── History.jsx
    └── services/
        └── api.js              # Axios client + API functions
```

---

## Development

### Start the dev server

```bash
cd frontend
npm install
npm run dev
```

Opens at **http://localhost:5173**. All `/api` requests are proxied to `http://localhost:8080` (configured in `vite.config.js`).

### Build for production

```bash
npm run build
```

Output goes to `frontend/dist/`. Serve with any static file server.

### Preview the production build

```bash
npm run preview
```

---

## Routing

Defined in `App.jsx`:

| Path | Page Component | Description |
|---|---|---|
| `/` | `Dashboard` | Upload and preview API specs |
| `/generate` | `Generate` | Generate and download test framework |
| `/compare` | `Compare` | Compare two API spec versions |
| `/history` | `History` | View past generation runs |

---

## Pages

### Dashboard (`pages/Dashboard.jsx`)

The landing page. Allows users to upload an API spec to preview its structure before generating.

**Features:**
- File upload via `FileUpload` component
- Summary cards showing title, version, endpoint count, schema count
- `CodePreview` component with tabbed view of endpoints, schemas, and raw JSON
- Quick-action buttons to navigate to Generate or Compare pages

**State:**
- `parseResult` - Parsed API spec data from the backend
- `loading` - Upload in progress
- `error` - Error message string

---

### Generate (`pages/Generate.jsx`)

Upload a spec and generate a complete test automation framework.

**Features:**
- File upload
- Configurable base package name (text input, default: `com.example.api`)
- Animated progress steps via `GenerationProgress`
- Success summary with POJO count, test count, endpoint coverage
- Download button via `DownloadButton`

**State:**
- `file` - Selected file object
- `basePackage` - Package name string
- `generating` - Generation in progress
- `currentStep` - Progress step key (`parsing`, `schemas`, `tests`, `framework`, `packaging`)
- `result` - Generation result from backend
- `error` - Error message string

---

### Compare (`pages/Compare.jsx`)

Upload two versions of an API spec to see differences.

**Features:**
- Side-by-side file upload (old version / new version)
- Comparison header showing old title -> new title
- `SchemaChanges` component with summary cards and detailed change list

**State:**
- `oldFile`, `newFile` - File objects
- `comparison` - Comparison result from backend
- `loading`, `error` - Loading/error states

---

### History (`pages/History.jsx`)

View all past generation runs.

**Features:**
- Auto-loads history on mount via `useEffect`
- Refresh button for manual reload
- Empty state illustration when no history exists
- Each entry shows spec title, format, package, timestamp
- Stat cards for endpoints, schemas, POJOs, test classes
- Download button per entry

**State:**
- `history` - Array of generation records
- `loading`, `error` - Loading/error states

---

## Components

### Layout (`components/Layout.jsx`)

Top-level wrapper providing the sidebar + content area layout.

**Props:**
| Prop | Type | Description |
|---|---|---|
| `children` | ReactNode | Page content rendered in the main area |

**Structure:** Flexbox container with `Sidebar` on the left, scrollable main content on the right.

---

### Sidebar (`components/Sidebar.jsx`)

Navigation sidebar with links to all pages.

**Features:**
- App branding ("API Agent - Test Automation")
- 4 navigation links with SVG icons
- Active state highlighting (indigo background)
- Version footer

Uses `NavLink` from React Router for active state detection.

---

### FileUpload (`components/FileUpload.jsx`)

Drag-and-drop file upload zone powered by `react-dropzone`.

**Props:**
| Prop | Type | Default | Description |
|---|---|---|---|
| `onFileSelect` | Function | required | Callback receiving the selected file(s) |
| `label` | String | `"Upload API Spec"` | Display label text |
| `accept` | Object | JSON/YAML types | Accepted file MIME types |
| `multiple` | Boolean | `false` | Allow multiple file selection |

**States:**
- Default: Dashed border, upload icon, instruction text
- Drag active: Indigo border with highlight
- File selected: Green badge showing filename

---

### GenerationProgress (`components/GenerationProgress.jsx`)

Step-by-step progress indicator for framework generation.

**Props:**
| Prop | Type | Description |
|---|---|---|
| `currentStep` | String | Current step key (`parsing`, `schemas`, `tests`, `framework`, `packaging`) |
| `completed` | Boolean | Whether all steps are done |

**Steps:**
1. Parsing API Spec
2. Generating POJOs
3. Generating Tests
4. Building Framework
5. Packaging ZIP

Visual states: completed (green checkmark), current (pulsing blue dot), pending (gray number).

---

### SchemaChanges (`components/SchemaChanges.jsx`)

Displays the results of a schema comparison.

**Props:**
| Prop | Type | Description |
|---|---|---|
| `comparison` | Object | Comparison result with `changes`, `totalChanges`, `breakingChanges`, `nonBreakingChanges` |

**Sub-components:**
- `ChangeTypeBadge` - Color-coded badge for ADDED (green), REMOVED (red), MODIFIED (yellow), TYPE_CHANGED (purple)
- `SeverityBadge` - BREAKING (red pill) or NON-BREAKING (green pill)

**Layout:**
- 3 summary cards (total, breaking, non-breaking)
- Scrollable change list with old/new value diffs

---

### CodePreview (`components/CodePreview.jsx`)

Tabbed preview of parsed API spec data.

**Props:**
| Prop | Type | Description |
|---|---|---|
| `parseResult` | Object | Parsed spec data with `endpoints`, `schemas`, `endpointCount`, `schemaCount` |

**Tabs:**
- **Endpoints** - List with HTTP method badges and paths
- **Schemas** - Cards with field names, types, and required badges
- **Raw Data** - Pretty-printed JSON dump

**Sub-components:**
- `TabButton` - Tab selector with active underline
- `MethodBadge` - Color-coded HTTP method (GET=green, POST=blue, PUT=yellow, DELETE=red, PATCH=purple)

---

### DownloadButton (`components/DownloadButton.jsx`)

Button that triggers a framework ZIP download.

**Props:**
| Prop | Type | Description |
|---|---|---|
| `generationId` | String | UUID from the generation result |
| `disabled` | Boolean | Force disabled state |

**States:**
- Disabled: Gray background, not clickable
- Ready: Indigo button with download icon
- Downloading: Spinner animation with "Downloading..." text

Triggers a browser download by creating a temporary blob URL.

---

## API Service (`services/api.js`)

Centralized Axios client configured with base URL `/api` and 60-second timeout.

### Exported Functions

| Function | Parameters | Returns | Description |
|---|---|---|---|
| `parseSpec(file)` | File object | Parsed spec data | Upload and parse an API spec |
| `generateFramework(file, basePackage)` | File, package string | Generation result | Generate test framework |
| `compareSpecs(oldFile, newFile)` | Two file objects | Comparison result | Compare two specs |
| `downloadFramework(generationId)` | UUID string | (triggers download) | Download ZIP file |
| `getHistory()` | - | History array | Fetch generation history |

All functions handle multipart form data encoding automatically.

---

## Styling

The project uses **Tailwind CSS** with the default configuration. Key conventions:

- **Colors:** Indigo for primary actions, green for success, red for errors/breaking changes, gray for neutral
- **Rounded corners:** `rounded-xl` for cards, `rounded-lg` for buttons and inputs
- **Spacing:** `p-4`/`p-6`/`p-8` for padding, `gap-3`/`gap-4` for flex gaps
- **Typography:** `font-bold` for headings, `font-medium` for labels, `font-mono` for code
- **Custom scrollbar:** `.scrollbar-thin` class defined in `index.css`

No custom Tailwind theme extensions are used. All styling is utility-class based.

---

## Adding a New Page

1. Create `src/pages/NewPage.jsx`:
   ```jsx
   function NewPage() {
     return (
       <div className="max-w-5xl mx-auto space-y-8">
         <h2 className="text-2xl font-bold text-gray-900">New Page</h2>
       </div>
     )
   }
   export default NewPage
   ```

2. Add the route in `App.jsx`:
   ```jsx
   import NewPage from './pages/NewPage'
   // Inside <Routes>:
   <Route path="/new-page" element={<NewPage />} />
   ```

3. Add the nav item in `Sidebar.jsx`:
   ```jsx
   { path: '/new-page', label: 'New Page', icon: YourIcon }
   ```

---

## Adding a New API Call

1. Add the function in `services/api.js`:
   ```javascript
   export const newFunction = async (params) => {
     const response = await api.post('/new-endpoint', params)
     return response.data
   }
   ```

2. Import and use in any page/component:
   ```javascript
   import { newFunction } from '../services/api'
   ```
