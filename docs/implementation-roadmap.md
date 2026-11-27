# Rmap Consolidated Implementation Roadmap

Rmap is delivered in **four sequential phases** with strict internal checkpoints and phase verification gates.

## Phase 1: Core Platform and Infrastructure (Completed & Verified)

**Goal**: Establish the commercial monorepo, backend foundation, database/cache/SMTP infrastructure, Windows desktop workspace shell and design system, authentication with Email OTP, 72-hour trial and billing entitlement foundation, secure local workspace selection, private admin portal foundation, and CI verification.

**Checkpoints**:
- **1A**: Monorepo & Infrastructure (`infra/docker-compose.yml`, MongoDB `rs0`, Redis, Mailpit, docs, rules)
- **1B**: Spring Boot Backend Foundation (Java 21 target, Maven Wrapper, modular monolith, error contracts, health, OpenAPI)
- **1C**: MongoDB Replica Set, Redis & Mailpit Integration (auditing, OTP/rate-limit store, SMTP email service + delivery logs)
- **1D**: Flutter Windows Desktop & Design System (neutral tokens, themes, shell layout, command palette, data-driven nav registry)
- **1E**: Authentication & Email OTP (register, OTP verify/resend, login, logout, refresh rotation, password reset, profile, desktop UI)
- **1F**: Trial, Subscription & Billing Foundation (72h trial, plans, entitlement engine, read-only enforcement, `PaymentProvider` boundary, webhook verification)
- **1G**: Secure Local Workspace Selection (folder picker, canonical path guard, sensitive-file filter, bounded read-only scanner, backend metadata sync)
- **1H**: Private Admin Portal Foundation (React/TS/Vite, admin auth, health, users, trials/subscriptions, payments, audit & email logs)
- **1I**: Integration Tests, Build Verification & CI (`docs/phase-reports/phase-1.md`)

## Phase 2: Universal Project Intelligence (Completed on `phase-2-universal-intelligence`)

**Goal**: Implement project lifecycle management, untrusted document ingestion (PDF/DOCX/TXT/MD/spreadsheets), Google Gemini `LlmProvider`, the 4 specialized internal AI agents with persistent workflow orchestration, versioned `ProjectSpec`, technology catalog, requirements & constraint validation, system/DB/API architecture generation, Academic Intelligence (`ACAD-01`–`ACAD-16` discovery), Dataset Intelligence, and the Project-Aware AI Assistant.

## Phase 3: Engineering and Execution Workspace

**Goal**: Implement Dynamic Phase Optimization (Quick/Standard/Custom), Master Prompt Generation, External Coding Agent Orchestration (Antigravity, Claude Code, Codex, Copilot — Manual/Assisted/Authorized Automated), full Repository Intelligence, Task & Team Management, Evidence-Based Implementation Verification, Error Analysis & Repair Studio, UI/UX Design Workspace, Documentation Exports (MD/PDF/DOCX), Academic Deliverable Packs & Viva Studio, ML Experiment Tracking, Testing & Security Engine, and Deployment Center.

## Phase 4: Production Quality and Release

**Goal**: Complete Private Admin Portal (revenue/accounting exports, AI cost dashboards, catalog & settings management), hardening & security audits, end-to-end test suite, production deployment configs, Public Download Website (`website/`), Windows MSIX/EXE installer packaging, code signing, and secure auto-update infrastructure.
