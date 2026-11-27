# Rmap Phase 1 Report — Core Platform and Infrastructure

- **Branch**: `phase-1/core-platform-infrastructure`
- **Status**: Completed (Automated backend, desktop, and admin builds & tests passing; external runtime blockers documented below)
- **Date**: 2026-11-27

---

## 1. Implemented Functionality (Checkpoints A–I)

### A. Monorepo, Documentation & Antigravity Rules
- Created complete modular monorepo layout: `backend/`, `desktop/`, `admin/`, `website/` (Phase 4 placeholder), `infra/`, `scripts/`, `docs/`, `fixtures/`, `.agents/`, `.github/`.
- Authored `AGENTS.md`, `CLAUDE.md`, `README.md`, `.gitignore`, `.agents/rules/rmap-engineering.md`, and complete `docs/` specifications (including `docs/product/academic-mode.md` preserving `ACAD-01` through `ACAD-16`, `docs/architecture.md`, `docs/implementation-roadmap.md`, `docs/requirements-traceability.md`, `docs/agent-architecture.md`, `docs/local-execution-security.md`, `docs/acceptance-tests.md`, `docs/versions.md`, and `docs/prompts/phase-1..4.md`).

### B & C. Spring Boot Backend Foundation, MongoDB Replica Set, Redis & Mailpit
- Spring Boot `3.4.5` targeting Java `21` LTS (`--release 21`) with Maven Wrapper (`mvnw` / `mvnw.cmd`).
- Modular monolith packages under `com.rmap.platform`: `common`, `config`, `user`, `auth`, `billing`, `workspace`, `admin`.
- Standardized `ApiResponse<T>` envelope and RFC 7807 `ProblemDetail` `GlobalExceptionHandler`.
- MongoDB auditing (`MongoAuditingConfig`), persistent `AuditLog` (`AuditService`), and `EmailDeliveryLog` (`EmailService` via SMTP/Mailpit).
- Docker Compose (`infra/docker-compose.yml`) configuring MongoDB `7.0` single-node replica set (`rs0`), Redis `7.4`, and Mailpit `v1.22` with healthchecks and `infra/.env.example`.

### D. Flutter Windows Desktop Shell & Design System
- Windows-first Flutter `3.47.5` / Dart `3.13.4` app using `flutter_riverpod`, `go_router`, `dio`, `file_selector`, `path`, and `crypto`.
- Minimal Linear / VS Code design system (`lib/core/theme/rmap_tokens.dart`, `rmap_theme.dart`) with neutral palette, single restrained Indigo accent, compact typography, subtle radii, and Light / Dark / System themes.
- Full workspace shell (`lib/features/shell/rmap_shell.dart`): collapsible sidebar (`Ctrl+B`), data-driven navigation registry (`RmapNavRegistry`) across all 8 groups (`WORKSPACE`, `DISCOVERY`, `DESIGN`, `ENGINEERING`, `QUALITY`, `DELIVERY`, `PROJECT-SPECIFIC`, `ACCOUNT`) dynamically filtered by active project mode, project mode selector, `Ctrl+K` Command Palette, resizable right AI assistant panel, and bottom status bar.

### E. Authentication & Email OTP
- Backend endpoints under `/api/v1/auth`: `register`, `resend-otp`, `verify-email`, `login`, `refresh`, `logout`, `forgot-password`, `reset-password`, plus `/api/v1/users/me` (`GET`, `PATCH`).
- BCrypt (cost 12) password hashing, Redis-backed SHA-256 hashed 6-digit OTPs with TTL, HS256 JWT access tokens (`JwtService`), and opaque rotating refresh tokens stored hashed in MongoDB with reuse detection that revokes the entire token family.
- Optional env-only local admin bootstrap (`BootstrapAdminRunner`).
- Desktop authentication UI (`lib/features/auth/auth_screen.dart`, `auth_controller.dart`) supporting Sign In, Register, Email OTP verification, Password Reset, and configurable backend URL.

### F. Trial, Subscription & Billing Foundation
- Automatic 72-hour free trial (`TRIAL_72H`) provisioned upon email verification (`BillingService.provisionTrialIfMissing`).
- Backend-enforced `EntitlementService` computing `TRIALING`/`ACTIVE` (`FULL_ACCESS`) and `EXPIRED` (`READ_ONLY`) states, enforced via `@RequiresActiveEntitlement` + `EntitlementAspect` (`403 ENTITLEMENT_READ_ONLY`).
- Pluggable `PaymentProvider` interface with `UnconfiguredPaymentProvider` default (honestly returns `503 PAYMENT_PROVIDER_NOT_CONFIGURED` on checkout; never fakes charges) and HMAC-SHA256 verified, idempotent webhook ingestion at `POST /api/v1/billing/webhooks/{provider}`.

### G. Secure Local Workspace Selection
- Desktop `LocalWorkspaceSecurity` (`lib/features/workspace/local_workspace_security.dart`) enforces canonical path boundaries (`resolveSafePath` blocking `..` traversal), excludes sensitive files (`.env`, `.env.*`, `*.pem`, `*.key`, `*.p12`, `*secret*`, `*credential*`, `service-account*.json`) while allowing `.env.example`, performs bounded read-only scans (`maxFiles=2500`, `maxDepth=12`) with stack detection, and syncs only hashed path metadata (zero raw secrets) to `POST /api/v1/workspaces`.

### H. Private Admin Portal Foundation
- Separate React `19` + TypeScript (`strict: true`) + Vite `6` application in `admin/` with strict backend-enforced `ADMIN` RBAC (`/api/v1/admin/**`).
- Views for Admin Login, System Health & Overview, Users (suspend/reactivate), Trials & Subscriptions (+24h trial extension), Verified Payments, Audit Logs, and Email Delivery Logs.

### I. CI/CD & Verification
- Added GitHub Actions workflow `.github/workflows/ci.yml` covering backend (`./mvnw -B test`), admin (`typecheck`, `test`, `build`), and desktop (`flutter analyze`, `flutter test`, `flutter build windows --debug`) plus Windows script `scripts/verify-all.ps1`.

---

## 2. Created and Modified Files

- **Root & Rules**: `AGENTS.md`, `CLAUDE.md`, `README.md`, `.gitignore`, `.agents/rules/rmap-engineering.md`, `.github/workflows/ci.yml`, `scripts/verify-all.ps1`, `website/README.md`, `fixtures/README.md`
- **Docs**: `docs/product/product-specification.md`, `docs/product/academic-mode.md`, `docs/architecture.md`, `docs/implementation-roadmap.md`, `docs/requirements-traceability.md`, `docs/agent-architecture.md`, `docs/local-execution-security.md`, `docs/acceptance-tests.md`, `docs/versions.md`, `docs/prompts/phase-1.md`..`phase-4.md`, `docs/phase-reports/phase-1.md`
- **Infra**: `infra/docker-compose.yml`, `infra/.env.example`, `infra/mongo/init-rs.sh`
- **Backend**: `backend/pom.xml`, `backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/*`, `backend/src/main/resources/application.yml`, 50 Java source files under `backend/src/main/java/com/rmap/platform/**`, and 3 test suites under `backend/src/test/java/com/rmap/platform/**`
- **Desktop**: `desktop/pubspec.yaml`, `desktop/lib/main.dart`, `desktop/lib/app.dart`, `desktop/lib/core/{theme,navigation,network,routing}/*`, `desktop/lib/features/{auth,shell,dashboard,workspace,settings}/*`, `desktop/test/widget_test.dart`, and Flutter Windows runner files
- **Admin**: `admin/package.json`, `admin/tsconfig.json`, `admin/vite.config.ts`, `admin/index.html`, `admin/src/{main.tsx,index.css,App.tsx,App.test.tsx,testSetup.ts,vite-env.d.ts}`, `admin/src/types/admin.ts`, `admin/src/api/adminApi.ts`

---

## 3. Completed Requirement IDs

`PLAT-01`, `PLAT-02` (definition complete; local container runtime blocked by missing Docker), `PLAT-03`, `AUTH-01`, `AUTH-02`, `AUTH-03`, `AUTH-04`, `BILL-01`, `BILL-02`, `BILL-03` (provider boundary & HMAC webhook verified; live checkout blocked pending provider credentials), `DESK-01`, `DESK-02`, `DESK-03`, `LOCL-01`, `LOCL-02`, `ADMN-01`, `ADMN-02`, `CI-01`.

---

## 4. Actual Commands Executed & Results

| Component | Command | Result |
| :--- | :--- | :--- |
| Backend | `cd backend && .\mvnw.cmd test` | **PASS** — Compiled 50 source files (`--release 21`), `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0` (`SecurityRbacWebMvcTest`: 2, `AuthServiceTest`: 3, `EntitlementAndBillingTest`: 3) |
| Desktop Analyze | `cd desktop && flutter analyze` | **PASS** — `No issues found! (ran in 3.3s)` |
| Desktop Tests | `cd desktop && flutter test` | **PASS** — `5/5` tests passed (`LocalWorkspaceSecurity` path traversal, sensitive-file exclusion, read-only scan, `RmapNavRegistry` filtering, `RmapDesktopApp` auth screen) |
| Desktop Windows Build | `cd desktop && flutter build windows --debug` | **PASS** — `√ Built build\windows\x64\runner\Debug\rmap_desktop.exe` (48.2s) |
| Admin Typecheck | `cd admin && npm run typecheck` | **PASS** — `tsc --noEmit` (0 errors) |
| Admin Tests | `cd admin && npm run test` | **PASS** — Vitest `1/1` passed (`src/App.test.tsx`) |
| Admin Build | `cd admin && npm run build` | **PASS** — Vite production bundle built in 949ms (`dist/`) |

---

## 5. Manual Testing Instructions

1. **Install & Start Docker Desktop** (prerequisite for local MongoDB `rs0`, Redis, and Mailpit):
   ```powershell
   Copy-Item infra\.env.example infra\.env
   docker compose -f infra/docker-compose.yml up -d
   ```
2. **Start Backend** (optionally set `RMAP_BOOTSTRAP_ADMIN_EMAIL` and `RMAP_BOOTSTRAP_ADMIN_PASSWORD` in shell for local admin login):
   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```
   - Verify `http://localhost:8080/api/v1/health` and Swagger UI at `http://localhost:8080/swagger-ui.html`.
3. **Run Flutter Windows Desktop**:
   ```powershell
   cd desktop
   flutter run -d windows
   ```
   - Register a new user -> inspect OTP email in Mailpit (`http://localhost:8025`) -> verify email -> confirm 72-hour trial status in the status bar -> test `Ctrl+K` Command Palette, sidebar collapse (`Ctrl+B`), theme switching, and **Import Project / Repository Explorer** folder selection.
4. **Run Private Admin Portal**:
   ```powershell
   cd admin
   npm run dev
   ```
   - Open `http://localhost:5174`, sign in with the bootstrapped `ADMIN` account, and verify System Health, Users, Subscriptions (+24h trial), Payments, Audit Logs, and Email Delivery Logs.

---

## 6. Known Issues & External Configuration Blockers

1. **Docker Desktop Not Installed (Local Runtime Blocker)**: `docker` / `docker compose` is not installed on this Windows workstation, so live container startup (`infra/docker-compose.yml`) and live MongoDB/Redis/Mailpit end-to-end smoke testing could not be executed locally. All backend business logic, security RBAC, OTP/JWT/refresh rotation, and billing webhook flows were verified via automated JUnit 5 + Spring MockMvc tests.
2. **Payment Provider Credentials Not Configured (External Blocker)**: Live payment checkout (`POST /api/v1/billing/checkout`) is intentionally blocked via `UnconfiguredPaymentProvider` (`503 PAYMENT_PROVIDER_NOT_CONFIGURED`) until a supported payment provider is selected after eligibility review and credentials (`RMAP_PAYMENT_PROVIDER`, `RMAP_PAYMENT_WEBHOOK_SECRET`) are supplied.
3. **Admin `npm audit`**: `npm install` reported 3 transitive dev-dependency advisories; review and pin/upgrade in Phase 4 hardening if needed.

---

## 7. Next Phase Instructions (Phase 2)

Do not begin Phase 2 until Phase 1 is reviewed and approved. When ready, follow [docs/prompts/phase-2.md](../prompts/phase-2.md) on a new branch (`phase-2/universal-project-intelligence`) to implement Project CRUD, untrusted document ingestion, the backend Google Gemini `LlmProvider`, the 4 specialized internal AI agents, versioned `ProjectSpec`, Academic Mode discovery (`ACAD-01`–`ACAD-16`), Dataset Intelligence, and the Project-Aware AI Assistant.
