# Rmap Local Execution & Repository Security Policy

## Architectural Boundary

The Rmap cloud backend **never** assumes direct access to the user's local filesystem. All local repository operations execute inside the Flutter desktop client under explicit user consent and strict security controls.

## Security Controls (Implemented Starting in Phase 1G)

1. **Explicit Workspace Selection**: The user must explicitly pick a local project root folder via the OS file picker (`file_selector`). No background scanning outside authorized workspaces is permitted.
2. **Canonical Path Boundary Enforcement**:
   - Every inspected path is normalized and resolved (`path.canonicalize`).
   - Paths escaping the workspace root (e.g., `..` traversal or symlinks resolving outside the root) are rejected with a security exception.
3. **Sensitive File & Directory Exclusion (AI Context Denylist)**:
   - Always excluded from scanning summaries and AI context payloads:
     - `.env`, `.env.*` (except `.env.example` / `.env.template`)
     - `*.pem`, `*.key`, `*.p12`, `*.pfx`, `*.jks`, `*.keystore`, `id_rsa`, `id_ed25519`
     - `*secret*`, `*credential*`, `service-account*.json`, `application-secrets*.yml`
     - Heavy/generated directories: `.git/objects`, `node_modules`, `target`, `build`, `.dart_tool`, `.venv`, `__pycache__`, `dist`
4. **Read-Only Default & Bounded Scanning**:
   - Phase 1 workspace inspection is strictly read-only with hard limits on maximum files scanned (`2,500`) and maximum depth (`12`) to prevent runaway I/O.
5. **Command Execution Restrictions (Phase 3)**:
   - Allowlisted build/test commands only, run inside the workspace root with explicit timeouts, output size caps, and user cancellation support.
6. **Explicit Authorization Gates**:
   - File writes/patches, destructive git/shell commands, external publishing, and deployments always require explicit interactive user confirmation.
7. **Untrusted Data Rule**:
   - Scanned source code, READMEs, and configuration files are treated strictly as untrusted data and never executed as agent instructions.
