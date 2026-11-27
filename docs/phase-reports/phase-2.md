# Rmap Phase 2 Report — Universal Project Intelligence and AI Agents

- **Branch**: `phase-2-universal-intelligence`
- **Status**: Completed & Verified (All automated backend, desktop release, admin regression, and live MongoDB persistence checks passing; live Google Gemini API calls blocked pending `GEMINI_API_KEY`)
- **Date**: 2026-11-27

---

## 1. Implemented Features

1. **Universal Project Management (`backend/project`, `desktop/projects`)**:
   - Full lifecycle CRUD: create, edit, duplicate, archive/restore, delete with exact name confirmation, search/mode/status filters, project switching, membership, invitations, activity history, local workspace association, optimistic locking (`@Version`), and strict object-level authorization (`ProjectAuthorizationService`).
   - Supports all 7 project modes (`INDIVIDUAL`, `CLIENT`, `TEAM`, `ACADEMIC`, `STARTUP`, `TESTING`, `MAINTENANCE`) and extensible combined/custom archetypes.
2. **Adaptive Project Creation Wizard (`desktop/lib/features/projects/projects_screens.dart`)**:
   - Supports combining multiple input methods (A–H), dynamically adapts visible fields (e.g., team size, budget) based on mode and archetypes, and supports custom unseeded archetypes.
3. **Real Google Gemini Integration (`backend/ai/GeminiLlmProvider.java`)**:
   - Backend-only `LlmProvider` using `GEMINI_API_KEY` and `GEMINI_MODEL` (default `gemini-2.5-flash`), structured JSON output (`responseMimeType: application/json`), secret redaction, timeout, bounded retries with backoff, rate-limit handling, token & cost tracking (`AiUsageRecord`), and daily token quota enforcement.
   - Returns truthful `503 GEMINI_NOT_CONFIGURED` when `GEMINI_API_KEY` is missing (never returns fake AI responses).
4. **Four Specialized Internal AI Agents & Persistent Workflow Orchestrator (`backend/ai/`)**:
   - Distinct agents: `DiscoveryAgent`, `ArchitectAgent`, `EngineeringAgent`, `QualityDeliveryAgent` with strict JSON schema validation and `<UNTRUSTED_DATA>` prompt-injection framing.
   - `WorkflowOrchestratorService`: conditional agent pipelines, human approval checkpoints (`AWAITING_APPROVAL` -> `approve`), failed-step recovery (`FAILED` -> `retry`, never marking failed runs as completed), cancellation, and token/cost accounting.
5. **Universal Document Intelligence (`backend/document/`)**:
   - Safe parsing for PDF (Apache PDFBox page-by-page), DOCX & XLSX (Apache POI sections/sheets), TXT/MD/CSV, Images, and ZIP archives.
   - Protects against filename path traversal, ZIP slip (`..`), archive bombs (entry count & uncompressed byte caps), sensitive file exposure inside ZIPs, and redacts secret-like tokens; deduplicates by SHA-256 and versions by filename.
6. **Versioned `ProjectSpec` & Architecture Engine (`backend/spec/ProjectSpec*.java`)**:
   - Authoritative single source of truth with stable IDs (`REQ-001`, `NFR-001`, `MUST-001`, `RUB-001`, `ROLE-001`, `COMP-001`, `PAGE-001`, `API-001`, `ENT-001`, `TEST-001`), optimistic locking, draft -> approved lifecycle (editing an approved spec forks a new draft version without overwriting history), version diff/impact analysis, and archetype-aware architecture generation (standalone ML notebooks omit DB/API and emit `ML_PIPELINE` diagrams).
7. **Data-Driven Technology Recommendation & Constraint Engine (`backend/spec/TechnologyCatalog*.java`)**:
   - Seeds 45 representative technologies across 13 categories in MongoDB, supports adding custom unseeded technologies via API, and enforces mandatory/forbidden project constraints as hard overrides.
8. **Academic Intelligence Engine (`backend/spec/Academic*.java`)**:
   - Preserves `ACAD-01`–`ACAD-16` with `AcademicAssessment`, rubric-to-requirement mapping, planned vs verified coverage separation, marking-scheme sum inconsistency detector, conflicting mandatory/forbidden tech detector, and academic integrity warnings (never guarantees marks or fabricates evidence).
9. **Dataset & ML Intelligence Engine (`backend/spec/Dataset*.java`)**:
   - Deterministic bounded CSV profiling: SHA-256 fingerprint, row/column counts, duplicate row count, per-column type inference, missing %, distinct count, numeric min/max/mean, data quality & target leakage risk warnings, and ML experiment planning without fabricating training accuracy.
10. **Project-Aware AI Chatbot (`backend/chat/`, `desktop/lib/features/chat/project_chat_panel.dart`)**:
    - Persistent per-project conversations (create, list/search, rename, delete), strict single-project authorized context retrieval (zero cross-project leakage), source references, suggested actions, and explicit user approval workflow before applying AI-proposed `ProjectSpec` changes.

---

## 2. API Endpoints Added (all under `/api/v1`)

- **Projects**: `POST/GET /projects`, `GET/PATCH/DELETE /projects/{id}`, `POST /projects/{id}/{duplicate,archive,restore,invitations,workspace}`
- **Documents**: `POST/GET /projects/{id}/documents`, `GET/DELETE /projects/{id}/documents/{docId}`
- **AI & Workflows**: `GET /ai/status`, `POST/GET /projects/{id}/ai/workflows`, `POST /projects/{id}/ai/workflows/{wfId}/{approve,retry,cancel}`, `GET /projects/{id}/ai/usage`
- **ProjectSpec & Architecture**: `GET /projects/{id}/specs`, `GET /projects/{id}/specs/latest`, `PUT /projects/{id}/specs/draft`, `POST /projects/{id}/specs/{v}/approve`, `GET /projects/{id}/specs/compare?from=&to=`, `POST /projects/{id}/specs/architecture/generate`
- **Technology Catalog**: `GET/POST /catalog/technologies`, `POST /catalog/technologies/validate`
- **Academic & Dataset**: `GET/PUT /projects/{id}/academic`, `GET /projects/{id}/academic/rubric-coverage`, `POST/GET /projects/{id}/datasets`
- **Project-Aware Chat**: `POST/GET /projects/{id}/chat/conversations`, `PATCH/DELETE /projects/{id}/chat/conversations/{cid}`, `POST /projects/{id}/chat/conversations/{cid}/messages`, `POST /projects/{id}/chat/conversations/{cid}/proposals/{pid}/approve`

---

## 3. MongoDB Collections Added

`projects`, `project_documents`, `project_specs`, `technology_catalog`, `academic_assessments`, `dataset_profiles`, `ai_workflows`, `ai_usage_records`, `chat_conversations`.

---

## 4. Actual Verification & Test Results

### Phase 2 Implementation Audit Findings & Fixes

During the full Phase 2 audit, the following reproducible gaps/issues were identified and fixed:
1. **Backend Optimistic Lock Error Mapping**: Added `OptimisticLockingFailureException` handler in `GlobalExceptionHandler` returning RFC 7807 `409 CONFLICT` (`OPTIMISTIC_LOCK_CONFLICT`) instead of `500`.
2. **Backend Catalog Entitlement Guard**: Added `@RequiresActiveEntitlement` to `POST /api/v1/catalog/technologies` so expired trials remain strictly read-only.
3. **Desktop Cross-Project Chat Isolation**: Fixed `ProjectChatPanel` so switching `activeProject` immediately resets and reloads conversations scoped strictly to the newly selected project, and added session switcher, **New Chat**, and **Delete Chat** controls.
4. **Desktop Dashboard, Design & Academic Completeness**: Updated `DashboardScreen` from Phase 1 placeholder text to active Phase 2 project context; upgraded `ArchitectureWorkspaceScreen` into a 4-tab workspace (`Architecture & Diagrams`, `Technology Stack` with live `/validate`, `Database & APIs`, `Roles & Permissions`); added interactive rubric criterion editing wired to `PUT /academic` in `AcademicStudioScreen`.

### Automated & Live Verification Matrix

| Suite / Check | Command | Observed Result |
| :--- | :--- | :--- |
| Backend Verify & Package | `cd backend && .\mvnw.cmd verify` | **PASS** — 95 Java source files compiled (`--release 21`), **`19/19` tests passed** (Phase 1 regression: 12; Phase 2 Scenarios A/C/E/G, security, AI schema/workflow recovery, cross-project RBAC, chat proposal approval: 7); packaged JAR |
| Live Phase 2 + MongoDB Audit | Packaged JAR (`PORT=8087`, `127.0.0.1:27017/rmap_phase2_audit`) | **PASS** — Verified live multipart document upload (`scenario-c-synthetic-academic-brief.md` -> `EXTRACTED`), multipart CSV profiling (`6×6`, `dups=1`, `leakageWarnings=2`), academic rubric audit (`inconsistencies=2`, `90.00` vs `100.00` marks), tech constraint validation (`valid=False`, `violations=2`), and truthful AI workflow failure without `GEMINI_API_KEY` (`status=FAILED`, never marked `COMPLETED`) |
| Desktop Analyze & Tests | `cd desktop && flutter analyze && flutter test` | **PASS** — 0 analyzer issues, **`8/8` tests passed** |
| Desktop Windows Release Build | `cd desktop && flutter build windows --release` | **PASS** — `√ Built build\windows\x64\runner\Release\rmap_desktop.exe` (29.6s) |
| Admin Portal Regression | `cd admin && npm run lint && npm run typecheck && npm run test && npm run build` | **PASS** — 0 ESLint/TS errors, `3/3` Vitest tests passed, Vite production build succeeded (866ms) |

---

## 5. Real Gemini Integration Status, Missing Credentials & Blocked Tests

- **Real Gemini API Status**: Implemented in `GeminiLlmProvider` (`GEMINI_API_KEY`, `GEMINI_MODEL`). Because `GEMINI_API_KEY` is **not set** on this machine, live end-to-end Google Gemini API generation tests are **BLOCKED** and `GET /api/v1/ai/status` truthfully reports `configured: false`.
- **Real Academic Source Fixtures**: Not present in the development repository (by design). Synthetic test fixtures in `fixtures/phase2/` verify deterministic parsing, rubric arithmetic auditing, and dataset profiling, and do **not** claim universal document understanding on unseen real PDFs.
- **Redis & Mailpit**: Remain external local infrastructure blockers (Docker Desktop not installed locally).

---

## 6. Manual Testing Instructions

1. Start MongoDB (already running on `127.0.0.1:27017`) and export `GEMINI_API_KEY` + `GEMINI_MODEL=gemini-2.5-flash` to enable live Gemini calls.
2. Start backend on a free port:
   ```powershell
   cd backend
   $env:PORT = "8086"
   $env:RMAP_MONGODB_URI = "mongodb://localhost:27017/rmap"
   $env:RMAP_BOOTSTRAP_ADMIN_EMAIL = "admin@rmap.local"
   $env:RMAP_BOOTSTRAP_ADMIN_PASSWORD = "ChangeMeAdmin123!"
   .\mvnw.cmd spring-boot:run
   ```
3. Launch `.\desktop\build\windows\x64\runner\Release\rmap_desktop.exe`, set Server URL to `http://localhost:8086`, sign in, and test **Create Project Wizard**, **My Projects**, **Requirements & Spec Approval**, **Documents**, **Architecture**, **Agent Manager**, **Academic Studio**, **ML Experiments (Dataset Workspace)**, and the **Project-Aware AI Assistant** side panel.

---

## 7. Phase 3 Handover Context

Phase 2 establishes the persisted `Project`, `ProjectSpec` (versioned & approved), `ProjectDocument`, `AcademicAssessment`, `DatasetProfile`, `WorkflowExecution`, and `LlmProvider` contracts required by Phase 3 (**Dynamic Phase Optimization, Master Prompt Generation, External Coding Agent Orchestration, Full Repository Intelligence, Evidence-Based Verification, Documentation Exports, and Deployment Center**). Do not start Phase 3 until Phase 2 is reviewed and approved.
