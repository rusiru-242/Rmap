# Phase 2 Execution Prompt — Universal Project Intelligence

## Prerequisites
Phase 1 reviewed, verified, and merged.

## Scope
- Project CRUD (all supported project types and modes, including combined types).
- Untrusted document ingestion pipeline (PDF, DOCX, TXT, MD, spreadsheets) with source-page citation tracking.
- Google Gemini `LlmProvider` integration (backend-only API key, token/cost ledger).
- Four specialized internal AI agents (Discovery, Architect, Engineering, Quality & Delivery) with persistent workflow orchestrator, human approval gates, retry/cancel, and schema validation.
- Versioned `ProjectSpec` single source of truth with change impact analysis.
- Technology catalog & Constraint Validation Engine.
- Academic Mode discovery intelligence (`ACAD-01`–`ACAD-05`, `ACAD-08`, `ACAD-13`–`ACAD-15`).
- Dataset Intelligence Engine (profiling, quality analysis, dataset fingerprints).
- Project-Aware AI Assistant with persistent per-project chat history and context retrieval.
