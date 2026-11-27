# CLAUDE.md — Claude Code Instructions for Rmap

Read and follow `AGENTS.md` in the repository root as the authoritative set of engineering rules, architecture boundaries, and verification commands for **Rmap**.

## Quick Reference

- **Product**: Rmap — Commercial AI-Powered Software Engineering Workspace and Project Execution Platform.
- **Monorepo**: `backend/` (Java 21 / Spring Boot / Maven), `desktop/` (Flutter Windows / Riverpod / go_router), `admin/` (React / TypeScript / Vite), `website/` (Phase 4), `infra/` (Docker Compose: MongoDB replica set, Redis, Mailpit), `docs/`.
- **Key Docs**:
  - `docs/product/product-specification.md`
  - `docs/product/academic-mode.md` (ACAD-01 through ACAD-16)
  - `docs/architecture.md`
  - `docs/implementation-roadmap.md`
  - `docs/requirements-traceability.md`
  - `docs/local-execution-security.md`
  - `docs/versions.md`

## Claude-Specific Working Agreements

- Always run relevant unit/integration tests and builds after editing code (`mvnw.cmd test`, `flutter analyze && flutter test`, `npm run build`).
- Never hardcode secrets, never fake AI responses or payment verification, and never bypass security checks.
- Keep commits atomic using Conventional Commits.
- Do not advance to the next roadmap phase until the current phase report is verified and approved.
