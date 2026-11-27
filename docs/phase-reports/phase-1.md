# Rmap Phase 1 Report — Core Platform and Infrastructure

- **Branch**: `phase-1/core-platform-infrastructure`
- **Status**: Verified (All automated backend, desktop release, admin lint/test/build, and live MongoDB persistence checks passing; missing local Redis/Mailpit/Docker and payment credentials documented as blockers)
- **Date**: 2026-11-27

---

## 1. Implemented Functionality (Checkpoints A–I)

### A. Monorepo, Documentation & Antigravity Rules
- Created modular monorepo layout: `backend/`, `desktop/`, `admin/`, `website/` (Phase 4 placeholder), `infra/`, `scripts/`, `docs/`, `fixtures/`, `.agents/`, `.github/`.
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
- Full workspace shell (`lib/features/shell/rmap_shell.dart`): collapsible sidebar (`Ctrl+B`), data-driven navigation registry (`RmapNavRegistry`) across all 8 groups dynamically filtered by active project mode, project mode selector, `Ctrl+K` Command Palette, resizable right AI assistant panel, and bottom status bar.

### E. Authentication & Email OTP
- Backend endpoints under `/api/v1/auth`: `register`, `resend-otp`, `verify-email`, `login`, `refresh`, `logout`, `forgot-password`, `reset-password`, plus `/api/v1/users/me` (`GET`, `PATCH`).
- BCrypt (cost 12) password hashing, Redis-backed SHA-256 hashed 6-digit OTPs with TTL, HS256 JWT access tokens (`JwtService`), and opaque rotating refresh tokens stored hashed in MongoDB with reuse detection revoking the entire token family.
- Desktop authentication UI (`lib/features/auth/auth_screen.dart`, `auth_controller.dart`) supporting Sign In, Register, Email OTP verification, Password Reset, and configurable backend URL.

### F. Trial, Subscription & Billing Foundation
- Automatic 72-hour free trial (`TRIAL_72H`) provisioned upon verification (`BillingService.provisionTrialIfMissing`).
- Backend-enforced `EntitlementService` computing `TRIALING`/`ACTIVE` (`FULL_ACCESS`) and `EXPIRED` (`READ_ONLY`) states, enforced via `@RequiresActiveEntitlement` + `EntitlementAspect` (`403 ENTITLEMENT_READ_ONLY`).
- Pluggable `PaymentProvider` interface with `UnconfiguredPaymentProvider` default (`503 PAYMENT_PROVIDER_NOT_CONFIGURED` on checkout; never fakes charges) and HMAC-SHA256 verified, idempotent webhook ingestion at `POST /api/v1/billing/webhooks/{provider}`.

### G. Secure Local Workspace Selection
- Desktop `LocalWorkspaceSecurity` (`lib/features/workspace/local_workspace_security.dart`) enforces canonical path boundaries (`resolveSafePath` blocking `..` traversal), excludes sensitive files (`.env`, `.env.*`, `*.pem`, `*.key`, `*.p12`, `*secret*`, `*credential*`, `service-account*.json`) while allowing `.env.example`, performs bounded read-only scans (`maxFiles=2500`, `maxDepth=12`) with stack detection, and syncs only hashed path metadata to `POST /api/v1/workspaces`.

### H. Private Admin Portal Foundation
- Separate React `19` + TypeScript (`strict: true`) + Vite `6` + ESLint application in `admin/` with strict backend-enforced `ADMIN` RBAC (`/api/v1/admin/**`).
- Views for Admin Login, System Health & Overview, Users (suspend/reactivate), Trials & Subscriptions (+24h trial extension), Verified Payments, Audit Logs, and Email Delivery Logs.

### I. CI/CD & Verification
- GitHub Actions workflow `.github/workflows/ci.yml` covering backend (`./mvnw -B test`), admin (`lint`, `typecheck`, `test`, `build`), and desktop (`flutter analyze`, `flutter test`, `flutter build windows --debug`) plus Windows script `scripts/verify-all.ps1`.

---

## 2. Reproducible Issues Found & Fixed During Phase 1 Verification

1. **Desktop `RmapShell` & `LocalWorkspaceScreen` `RenderFlex` overflows**: Detected by the expanded `RmapShell` widget test in `desktop/test/widget_test.dart`; fixed by wrapping header text columns in `Expanded` with `TextOverflow.ellipsis` in `local_workspace_screen.dart` and `rmap_shell.dart`.
2. **Admin Vitest DOM cleanup**: Added `cleanup()` in `afterEach` of `admin/src/App.test.tsx` so multi-test React Testing Library suites isolate DOM trees cleanly without requiring global test hooks.
3. **Admin ESLint setup**: Added `eslint`, `@eslint/js`, `typescript-eslint`, `admin/eslint.config.js`, and `npm run lint` (also wired into `.github/workflows/ci.yml` and `scripts/verify-all.ps1`).

---

## 3. Completed Requirement IDs

`PLAT-01`, `PLAT-02` (Compose config complete; native MongoDB verified locally; Docker/Redis/Mailpit blocked locally), `PLAT-03`, `AUTH-01`, `AUTH-02`, `AUTH-03`, `AUTH-04`, `BILL-01`, `BILL-02`, `BILL-03` (provider boundary & HMAC webhook verified; live checkout blocked pending credentials), `DESK-01`, `DESK-02`, `DESK-03`, `LOCL-01`, `LOCL-02`, `ADMN-01`, `ADMN-02`, `CI-01`.

---

## 4. Actual Verification Commands & Observed Results

### Automated Builds, Linting, Unit & Integration Tests

| Component | Command | Observed Result |
| :--- | :--- | :--- |
| Backend Verify & JAR | `cd backend && .\mvnw.cmd verify` | **PASS** — `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0` (`AuthServiceTest`: 3, `EntitlementAndBillingTest`: 3, `SecurityRbacWebMvcTest`: 2, `Phase1IntegrationWebMvcTest`: 4); packaged `backend\target\rmap-backend-0.1.0-SNAPSHOT.jar` (13.2s) |
| Desktop Analyze | `cd desktop && flutter analyze` | **PASS** — `No issues found! (ran in 3.8s)` |
| Desktop Tests | `cd desktop && flutter test` | **PASS** — `7/7` tests passed (path traversal, sensitive-file filter, read-only scan, `RmapNavRegistry`, `AuthScreen`, `RmapShell` + `Ctrl+K` Command Palette + `LocalWorkspaceScreen`, `SettingsScreen` Light/Dark/System theme toggle) |
| Desktop Windows Release | `cd desktop && flutter build windows --release` | **PASS** — `√ Built build\windows\x64\runner\Release\rmap_desktop.exe` (66.5s) |
| Admin Lint | `cd admin && npm run lint` | **PASS** — `eslint src` (0 errors, 0 warnings) |
| Admin Typecheck | `cd admin && npm run typecheck` | **PASS** — `tsc --noEmit` (0 errors) |
| Admin Tests | `cd admin && npm run test` | **PASS** — Vitest `3/3` passed (unauthenticated gate, non-ADMIN rejection, ADMIN System Health load) |
| Admin Production Build | `cd admin && npm run build` | **PASS** — Vite production bundle built in `1.05s` (`dist/`) |

### Live Backend Connectivity & Real MongoDB Persistence Smoke Test

Executed against the natively running local `MongoDB Server` service (`127.0.0.1:27017`, database `rmap_phase1_verify`) with the packaged Spring Boot JAR running on `PORT=8085` (port `8080` was occupied by an unrelated local PID `5768`):

```text
1. HEALTH: UP (phase=1)
2. LOGIN: user=verify-admin@rmap.local role=ADMIN entitlement=TRIALING/FULL_ACCESS trialHrs=71
3. PROFILE: verify-admin@rmap.local verified=True
4. ADMIN OVERVIEW: users=1 subs=1 audits=1 provider=unconfigured
5. WORKSPACE SAVED: id=6b090a5c527f7c530dfeb91a readOnly=True
6. REFRESH ROTATED: newAccessLen=242
7. LOGOUT: Signed out.
```

---

## 5. External Dependencies & Blockers

1. **Docker Desktop Not Installed**: `docker` / `docker compose` is not installed on this Windows workstation, so `infra/docker-compose.yml` could not be started locally.
2. **Redis (`6379`) and Mailpit (`1025`/`8025`) Not Installed Locally**: While MongoDB is running natively (`127.0.0.1:27017`), neither Redis nor Mailpit is installed/running on this machine. Therefore, live end-to-end Email OTP registration (`POST /api/v1/auth/register` -> Redis `rmap:otp:*` -> Mailpit SMTP) could not be executed against live Redis/Mailpit servers; OTP generation, consumption, password reset, and SMTP delivery logic are verified via automated unit & WebMvc tests (`AuthServiceTest`, `Phase1IntegrationWebMvcTest`).
3. **Live Payment Provider Credentials Not Configured**: Checkout (`POST /api/v1/billing/checkout`) returns `503 PAYMENT_PROVIDER_NOT_CONFIGURED` via `UnconfiguredPaymentProvider` until a provider is selected after eligibility review and `RMAP_PAYMENT_PROVIDER` + `RMAP_PAYMENT_WEBHOOK_SECRET` are provided.
4. **Local Port `8080` Occupied**: An existing non-Rmap process (PID `5768`) is listening on port `8080` on this workstation; pass `PORT=8085` (or another free port) when running Rmap backend locally unless port `8080` is freed.

---

## 6. Exact Commands & Manual Steps to Run Rmap Locally

### 1. Start Infrastructure (requires Docker Desktop)
```powershell
Copy-Item infra\.env.example infra\.env
docker compose -f infra/docker-compose.yml up -d
```
- MongoDB Replica Set (`rs0`): `mongodb://localhost:27017/rmap?replicaSet=rs0`
- Redis: `localhost:6379`
- Mailpit SMTP: `localhost:1025` | Web UI: `http://localhost:8025`

### 2. Start Backend
```powershell
cd backend
$env:PORT = "8085"
$env:RMAP_MONGODB_URI = "mongodb://localhost:27017/rmap"
$env:RMAP_BOOTSTRAP_ADMIN_EMAIL = "admin@rmap.local"
$env:RMAP_BOOTSTRAP_ADMIN_PASSWORD = "ChangeMeAdmin123!"
.\mvnw.cmd spring-boot:run
```
- Health check: `http://localhost:8085/api/v1/health`
- Swagger UI: `http://localhost:8085/swagger-ui.html`

### 3. Run Desktop Application (Windows)
```powershell
cd desktop
flutter run -d windows
```
Or launch the compiled release binary directly:
```powershell
.\desktop\build\windows\x64\runner\Release\rmap_desktop.exe
```
- Click **Server URL** on the Sign In screen and set `http://localhost:8085` (if running on port `8085`).
- Register an account, retrieve the 6-digit OTP from Mailpit (`http://localhost:8025`), verify email to activate the 72-hour trial, test `Ctrl+K` Command Palette, `Ctrl+B` sidebar collapse, theme switching in **Settings**, and folder selection in **Import Project / Repository Explorer**.

### 4. Run Private Admin Portal
```powershell
cd admin
$env:VITE_API_BASE_URL = "http://localhost:8085"
npm run dev
```
- Open `http://localhost:5174`, sign in with the bootstrapped `ADMIN` account, and inspect **System Health**, **Users**, **Trials & Subscriptions**, **Payments**, **Audit Logs**, and **Email Delivery Logs**.

---

## 7. Next Phase Instructions

Phase 1 verification is complete. Do not start Phase 2 until Phase 1 is reviewed and approved. When ready, follow [docs/prompts/phase-2.md](../prompts/phase-2.md).
