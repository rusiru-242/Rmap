# Rmap Requirements Traceability Matrix

| Req ID | Area | Requirement Summary | Phase | Target Module(s) | Status |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **PLAT-01** | Monorepo | Modular monorepo (`backend`, `desktop`, `admin`, `website`, `infra`, `docs`, `.github`) | 1 | Repository root | Completed |
| **PLAT-02** | Infra | Docker Compose for MongoDB replica set (`rs0`), Redis, Mailpit SMTP | 1 | `infra/` | Completed (runtime blocked: Docker not installed) |
| **PLAT-03** | Backend | Spring Boot 3.4 (Java 21), modular monolith, Actuator health, RFC 7807 errors | 1 | `backend/` | Completed |
| **AUTH-01** | Auth | User registration with hashed password and Email OTP verification | 1 | `backend/auth`, `desktop/auth` | Completed |
| **AUTH-02** | Auth | Login, logout, JWT access tokens, rotating refresh tokens with reuse detection | 1 | `backend/auth`, `desktop/auth` | Completed |
| **AUTH-03** | Auth | Password reset via Email OTP and secure session revocation | 1 | `backend/auth`, `desktop/auth` | Completed |
| **AUTH-04** | User | User profile retrieval and update | 1 | `backend/user`, `desktop/settings` | Completed |
| **BILL-01** | Trial | Automatic 72-hour free trial upon verified registration | 1 | `backend/billing` | Completed |
| **BILL-02** | Entitlement | Backend-enforced entitlements (`ACTIVE_TRIAL`, `ACTIVE_PAID`, `READ_ONLY_EXPIRED`) | 1 | `backend/billing` | Completed |
| **BILL-03** | Billing | Pluggable `PaymentProvider` interface & HMAC-verified webhook ingestion (no fake charges) | 1 | `backend/billing` | Completed (live provider blocked pending credentials) |
| **DESK-01** | Desktop UI | Linear/VS Code minimal design system, neutral tokens, light/dark/system themes | 1 | `desktop/core/theme` | Completed |
| **DESK-02** | Desktop UI | Collapsible sidebar, workspace, resizable AI panel, project selector, `Ctrl+K` command palette, status bar | 1 | `desktop/features/shell` | Completed |
| **DESK-03** | Desktop UI | Data-driven navigation registry across all 8 workspace groups | 1 | `desktop/core/navigation` | Completed |
| **LOCL-01** | Local Repo | Controlled local workspace folder selection with canonical path boundary enforcement | 1 | `desktop/features/workspace` | Completed |
| **LOCL-02** | Local Repo | Sensitive-file exclusion (`.env`, keys, certs, credentials) & bounded read-only scan | 1 | `desktop/features/workspace`, `backend/workspace` | Completed |
| **ADMN-01** | Admin Portal | Separate React/TS private admin app with backend-enforced `ADMIN` role | 1 | `admin/`, `backend/admin` | Completed |
| **ADMN-02** | Admin Portal | User, trial, subscription, payment history, email log, audit log, and health views | 1 | `admin/`, `backend/admin` | Completed |
| **CI-01** | CI/CD | GitHub Actions CI verifying backend, desktop, and admin builds & tests | 1 | `.github/workflows/` | Completed |
| **AI-01..04** | AI Agents | Google Gemini `LlmProvider` and 4 internal agents + persistent orchestrator | 2 | `backend/ai` | Scheduled |
| **SPEC-01** | ProjectSpec | Versioned Universal Project Specification with traceability | 2 | `backend/project` | Scheduled |
| **ACAD-01..16** | Academic | Dynamic academic brief/rubric intelligence, constraints, logs, viva, deliverable packs | 2–3 | `backend/academic`, `desktop/` | Scheduled |
| **EXEC-01..10** | Execution | Dynamic Phase Planner, Master Prompts, External Agents, Verification, Deployment | 3 | `backend/`, `desktop/` | Scheduled |
| **REL-01..04** | Release | Public Download Website, Windows installer, signing, auto-update | 4 | `website/`, `scripts/` | Scheduled |
