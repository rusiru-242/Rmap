# Rmap Internal AI Agent Architecture (Phase 2 Target)

All internal AI calls route exclusively through a backend `LlmProvider` backed by the **Google Gemini API**. The Gemini API key is never exposed to desktop, admin, or web clients.

## Four Specialized Internal Agents

| Agent | Responsibility | Primary Inputs | Structured Outputs |
| :--- | :--- | :--- | :--- |
| **1. Discovery Agent** | Project understanding, untrusted document analysis, requirements extraction, business research, dataset profiling, missing-info detection, constraint identification | User prompt, parsed docs (with page citations), dataset profiles, academic rubrics | `DiscoveryReport`, candidate requirements, constraints, ambiguity/conflict flags |
| **2. Architect Agent** | Technology recommendations, system architecture, database schema, API contracts, UI screen inventory, security architecture, phase planning | Approved discovery outputs, hard constraints, technology catalog | `ArchitectureSpec`, `DatabaseSpec`, `ApiContractSpec`, `UiInventory`, `PhasePlan` |
| **3. Engineering Agent** | Master prompt generation, coding task preparation, external agent coordination (Antigravity, Claude Code, Codex, Copilot), repository analysis, error diagnosis & repair guidance | Approved `ProjectSpec`, local repo index, build/test error logs | `MasterPromptPack`, `AGENTS.md`/`CLAUDE.md` exports, `RepairPlan` |
| **4. Quality & Delivery Agent** | Requirement verification against real evidence, test planning, security review, academic rubric validation, deployment instructions, documentation & handover packs | `ProjectSpec`, verified repo/build/test evidence, rubric criteria | `VerificationReport`, `RubricAudit`, `DeploymentGuide`, `DocumentationPack` |

## Persistent AI Workflow Orchestrator

Every agent run must enforce:
- **Distinct responsibility** and strict JSON schema validation of inputs/outputs.
- **Controlled tool permissions** (read-only by default; no arbitrary shell execution).
- **Persistent workflow state** in MongoDB with execution history, retry, and cancellation support.
- **Usage & cost ledger** tracking prompt/completion tokens and estimated cost per user and project (surfaced in Admin Portal).
- **Human approval boundaries** before promoting draft specs, overwriting artifacts, or triggering external actions.
- **Prompt-injection defense**: uploaded documents, websites, and repository files are wrapped as untrusted data blocks, never system instructions.
