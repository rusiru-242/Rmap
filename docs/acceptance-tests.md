# Rmap Phase 1 Acceptance Tests

## Backend (`backend/`)
- **AT-BE-01**: Health endpoint (`GET /actuator/health` and `GET /api/v1/health`) returns `200 OK`.
- **AT-BE-02**: `POST /api/v1/auth/register` validates email/password, stores BCrypt hash, generates a 6-digit OTP in Redis, sends verification email via SMTP/Mailpit, and logs delivery.
- **AT-BE-03**: `POST /api/v1/auth/verify-email` verifies OTP, activates user, provisions a **72-hour free trial**, and issues JWT access + rotating refresh tokens.
- **AT-BE-04**: `POST /api/v1/auth/refresh` rotates refresh token; reusing an already-rotated token revokes the token family and returns `401`.
- **AT-BE-05**: `POST /api/v1/auth/forgot-password` and `POST /api/v1/auth/reset-password` verify OTP, update password hash, and revoke existing refresh sessions.
- **AT-BE-06**: `EntitlementService` computes `ACTIVE_TRIAL`, `ACTIVE_PAID`, or `READ_ONLY_EXPIRED`; write endpoints annotated with `@RequiresActiveEntitlement` return `403` when expired.
- **AT-BE-07**: `POST /api/v1/billing/webhooks/{provider}` rejects invalid or missing HMAC signatures (`401`/`400`) and handles duplicate event IDs idempotently.
- **AT-BE-08**: `/api/v1/admin/**` endpoints reject unauthenticated requests (`401`) and non-admin `USER` tokens (`403`), and succeed for `ADMIN` role (`200`).

## Desktop (`desktop/`)
- **AT-DK-01**: `flutter analyze` passes with zero errors.
- **AT-DK-02**: Theme switching supports Light, Dark, and System modes using centralized design tokens.
- **AT-DK-03**: Main shell renders collapsible sidebar, data-driven navigation groups, project selector, resizable right AI panel, command palette (`Ctrl+K`), and status bar.
- **AT-DK-04**: Local workspace path validator blocks directory traversal (`..`) and filters out sensitive files (`.env`, `*.pem`, `*.key`, credentials).
- **AT-DK-05**: `flutter build windows --debug` builds cleanly on Windows.

## Admin Portal (`admin/`)
- **AT-AD-01**: TypeScript strict check (`npm run typecheck`) and production build (`npm run build`) succeed with zero errors.
- **AT-AD-02**: Unit tests (`npm run test`) verify auth guard, API client error handling, and admin dashboard components.
