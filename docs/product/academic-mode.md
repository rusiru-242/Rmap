# Rmap Academic Mode Specification (`ACAD-01` – `ACAD-16`)

Academic Mode is one supported project mode within Rmap — never a hardcoded limitation on the platform. It must dynamically analyze previously unseen academic briefs and rubrics at runtime without assuming any specific university, module, or technology stack.

## Core Principles

- Uploaded briefs, marking rubrics, and handouts are **untrusted runtime user inputs**.
- Every extracted requirement must reference its **source document and page number** (where applicable).
- Mandatory academic constraints **override** conflicting AI recommendations.
- Inconsistencies (e.g., mark totals not matching rubric weights) must be flagged for **user review**, never silently invented.
- Rmap must **never fabricate academic evidence** or ghostwrite content explicitly required to be written in a student's own words.

## Requirements Catalog (`ACAD-01` through `ACAD-16`)

| ID | Name | Requirement |
| :--- | :--- | :--- |
| **ACAD-01** | Dynamic Assignment Brief Analysis | Parse unseen assignment specifications (PDF, DOCX, TXT, MD) to extract objectives, scope, deliverables, and constraints with document/page citations. |
| **ACAD-02** | Marking Rubric & Weight Extraction | Extract rubric criteria, mark allocations, performance bands (e.g., Distinction/Merit/Pass), and flag arithmetic or structural inconsistencies for user review. |
| **ACAD-03** | Mandatory & Forbidden Technology Constraints | Identify required technologies, frameworks, languages, and forbidden tools/practices; enforce them as hard overrides in the Technology Recommendation and Constraint Validation engines. |
| **ACAD-04** | Required Component Inventory | Enumerate mandatory software components, artifacts, diagrams, or datasets required for submission and link them to `ProjectSpec` IDs. |
| **ACAD-05** | Individual vs. Group Ownership | Separate individual contribution requirements from shared team deliverables, tracking per-member ownership across tasks and phases. |
| **ACAD-06** | Per-Member Engineering Prompts | Generate coding agent instructions and master prompts scoped to an individual student's assigned components while preserving shared project contracts. |
| **ACAD-07** | Contribution & Evidence Tracking | Track real repository commits, files, tasks, and verification evidence per team member without fabricating contributions. |
| **ACAD-08** | Deadline & Milestone Alignment | Map assignment interim checkpoints, demo dates, viva dates, and final submission deadlines into the Dynamic Phase Planner. |
| **ACAD-09** | Report & Documentation Structure Mapping | Extract required report sections, word/page limits, formatting rules, and citation expectations into structured outlines and checklists (without ghostwriting student-authored prose). |
| **ACAD-10** | Demo Requirements Planning | Identify live/recorded demo requirements, scenarios, and test data needed to demonstrate rubric criteria. |
| **ACAD-11** | Viva Preparation Studio | Generate technical viva questions, architecture defense guides, and code walkthrough prompts grounded in the student's actual implemented repository. |
| **ACAD-12** | Submission Format Validation | Validate required archive formats, folder structures, naming conventions, README/setup instructions, and exclusion of build artifacts or secrets. |
| **ACAD-13** | AI-Use Policy & Restrictions Compliance | Extract institutional AI-use rules from the brief, enforce boundaries, and warn when a requested action conflicts with stated academic integrity rules. |
| **ACAD-14** | Transparent AI Usage Log | Maintain an exportable, timestamped AI Usage Log recording prompts, tools, models, and human-verified edits for academic disclosure. |
| **ACAD-15** | Originality & Integrity Warnings | Surface clear warnings against plagiarism, unverified AI claims, fabricated evaluation metrics, or ghostwritten reflections. |
| **ACAD-16** | Academic Deliverable Pack Export | Export a structured Academic Deliverable Pack (traceability matrix, rubric checklist, AI usage log, architecture/API/DB docs, and submission verification checklist) in Markdown, PDF, and DOCX. |
