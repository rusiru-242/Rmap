# Phase 1 Execution Prompt — Core Platform & Infrastructure

## Scope
Implement Phase 1 of Rmap on a dedicated Git branch (`phase-1/core-platform-infrastructure`):
- **A**: Monorepo skeleton, root docs, Antigravity rules, Docker Compose (MongoDB `rs0`, Redis, Mailpit).
- **B**: Spring Boot 3.4 backend foundation (Java 21 target, Maven Wrapper, modular monolith, error contracts, health, OpenAPI).
- **C**: MongoDB replica set configuration, Redis OTP/rate-limit store, SMTP/Mailpit email service with persistent delivery logs.
- **D**: Flutter Windows desktop shell, Linear/VS Code minimal design system, themes, command palette, data-driven navigation registry.
- **E**: Full authentication (register, email OTP, login, logout, refresh token rotation, password reset, profile) across backend and desktop.
- **F**: 72-hour trial, subscription plans, backend entitlement enforcement (read-only on expiry), `PaymentProvider` boundary, HMAC webhook verification.
- **G**: Secure local workspace selection on desktop (canonical path boundary, sensitive-file denylist, bounded read-only scan, backend metadata sync).
- **H**: Private React/TS/Vite Admin Portal foundation (admin auth, health, users, trials/subscriptions, payments, audit & email logs).
- **I**: Automated tests, build verification, GitHub Actions CI, and `docs/phase-reports/phase-1.md`.

## Exit Criteria
All automated tests and builds pass locally; external blockers (e.g., Docker not installed, live payment credentials) are documented transparently without faking verification.
