# Rmap Product Specification

## 1. Product Identity & Vision

- **Name**: Rmap
- **Category**: Commercial AI-Powered Software Engineering Workspace and Project Execution Platform
- **Objective**: Enable individuals, teams, agencies, startups, and students to transform any software project idea, client requirement, academic specification, dataset, or existing repository into a structured, implementable, tested, and deployable software solution.

Rmap is **not** a generic chatbot, static prompt generator, or academic-only template app. Its intelligence dynamically adapts to project requirements, constraints, and real repository state.

## 2. Supported Project Types & Modes

### Project Types (Extensible)
Web applications, mobile applications, desktop applications, full-stack applications, e-commerce platforms, enterprise systems, SaaS platforms, REST APIs, microservices, machine learning, deep learning, data science, agentic AI, IoT systems, automation tools, cybersecurity/testing projects, academic assignments, hackathons, existing project improvements, combined project types, and future custom types.

### Project Modes
- Individual Project
- Client Project
- Team Project
- Academic Project (see [academic-mode.md](academic-mode.md))
- Startup Project
- Testing & Maintenance Project

## 3. Core Platform Capabilities (23 Modular Engines)

| # | Engine | Primary Phase |
| :- | :--- | :--- |
| 01 | Universal Project Intelligence Engine | Phase 2 |
| 02 | Document Analysis Engine | Phase 2 |
| 03 | Dataset Intelligence Engine | Phase 2 |
| 04 | Technology Recommendation Engine | Phase 2 |
| 05 | Requirements Management Engine | Phase 2 |
| 06 | Constraint Validation Engine | Phase 2 |
| 07 | System Architecture Engine | Phase 2 |
| 08 | Database and API Design Engine | Phase 2 |
| 09 | UI/UX Design Engine | Phase 3 |
| 10 | Dynamic Phase Optimization Engine | Phase 3 |
| 11 | Master Prompt Generation Engine | Phase 3 |
| 12 | Coding Agent Orchestration Engine | Phase 3 |
| 13 | Repository Intelligence Engine | Phase 1 (foundation) / Phase 3 |
| 14 | Implementation Verification Engine | Phase 3 |
| 15 | Error Analysis and Repair Engine | Phase 3 |
| 16 | Project-Aware AI Assistant | Phase 2 |
| 17 | Task and Team Management Engine | Phase 3 |
| 18 | Documentation Engine | Phase 3 |
| 19 | Academic Assessment Engine | Phase 2 / Phase 3 |
| 20 | ML Experiment Tracking Engine | Phase 3 |
| 21 | Testing and Security Engine | Phase 3 |
| 22 | Deployment and Delivery Engine | Phase 3 |
| 23 | Maintenance and Change Management Engine | Phase 3 / Phase 4 |

## 4. Universal Project Specification (`ProjectSpec`)

A single versioned source of truth connecting every artifact (requirements, architecture, DB, API, UI, phases, tasks, prompts, verification evidence, documentation). Uses stable IDs, supports draft/review/approval lifecycle, version history, and change impact analysis, while omitting irrelevant sections for a given project type.

## 5. Commercial & Access Model

- **Authentication**: Email + password registration, Email OTP verification, JWT access tokens, rotating refresh tokens stored securely, session revocation, password reset via OTP.
- **Free Trial**: 72-hour full-feature free trial automatically provisioned upon account verification.
- **Subscriptions**: Configurable subscription tiers enforced exclusively by the backend entitlement engine.
- **Payment Boundary**: Pluggable `PaymentProvider` interface with webhook signature verification, idempotent event processing, and automatic paid access activation upon verified payment. No fake charges.
- **Expired Entitlement**: Automatic transition to read-only access preserving user data without destructive lockouts.

## 6. Surfaces

1. **Desktop Application (Windows-first Flutter)**: Primary engineering workspace with collapsible sidebar, main workspace, resizable right AI assistant panel, project selector, command palette, status bar, data-driven navigation registry, and secure local workspace access.
2. **Private Admin Portal (React/TS)**: Separate administrative application for user management, trials, subscriptions, payment history, revenue & accounting, AI usage/cost monitoring, email logs, audit logs, system health, and catalog/settings.
3. **Public Download Website (React/TS, Phase 4)**: Separate public site for product overview, Windows downloads, release notes, pricing, privacy, and terms.
