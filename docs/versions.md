# Rmap Tooling & Dependency Versions

This document records the verified local development environment and selected stable dependency versions for Phase 1.

## 1. Local Environment Audit (Verified)

| Tool | Installed Version | Target / Notes | Status |
| :--- | :--- | :--- | :--- |
| OS | Microsoft Windows 11 (25H2, build 26200) | Windows-first desktop target | Available |
| Java (JDK) | Oracle JDK 22.0.2 (`C:\Program Files\Java\jdk-22`) | Backend targets Java 21 LTS via `--release 21` | Available |
| Maven | Not installed globally | Uses Maven Wrapper (`mvnw` / `mvnw.cmd`, Maven 3.9.9) | Handled via Wrapper |
| Flutter | 3.47.5 (stable channel) | Windows desktop build toolchain ready | Available |
| Dart | 3.13.4 (stable) | Bundled with Flutter | Available |
| Visual Studio | Visual Studio Community 2026 (18.12.0) | C++ Windows Desktop workload verified by `flutter doctor` | Available |
| Node.js | v24.14.0 | Admin portal & Website build runtime | Available |
| npm | 11.9.0 | Package manager for `admin/` and `website/` | Available |
| Git | 2.53.0.windows.2 | Version control | Available |
| GitHub CLI (`gh`) | 2.103.3 | Repository automation | Available |
| Docker / Docker Compose | **Not installed** | Required to run local `infra/docker-compose.yml` (MongoDB `rs0`, Redis, Mailpit) | **Missing Prerequisite** |

## 2. Selected Dependency Versions (Phase 1)

### Backend (`backend/pom.xml`)
- **Java Target**: 21 LTS (`maven.compiler.release=21`)
- **Spring Boot**: `3.4.5`
- **Spring Security**: Managed by Spring Boot 3.4.5
- **Spring Data MongoDB**: Managed by Spring Boot 3.4.5
- **Spring Data Redis**: Managed by Spring Boot 3.4.5
- **Spring Mail**: Managed by Spring Boot 3.4.5
- **Spring Boot Actuator**: Managed by Spring Boot 3.4.5
- **JJWT (`io.jsonwebtoken`)**: `0.12.6` (HMAC-SHA256 JWT access tokens)
- **SpringDoc OpenAPI**: `2.8.6` (API documentation)

### Infrastructure (`infra/docker-compose.yml`)
- **MongoDB**: `mongo:7.0` (Single-node replica set `rs0` for local transactions)
- **Redis**: `redis:7.4-alpine`
- **Mailpit**: `axllent/mailpit:v1.22`

### Desktop (`desktop/pubspec.yaml`)
- **Flutter SDK**: `>=3.40.0` (verified on `3.47.5`)
- **Dart SDK**: `>=3.10.0 <4.0.0` (verified on `3.13.4`)
- **flutter_riverpod**: `^2.6.1`
- **go_router**: `^14.8.1`
- **dio**: `^5.8.0`
- **flutter_secure_storage**: `^9.2.4`
- **file_selector**: `^1.0.3`
- **path**: `^1.9.1`
- **shared_preferences**: `^2.5.2`

### Private Admin Portal (`admin/package.json`)
- **React**: `^19.1.0`
- **React DOM**: `^19.1.0`
- **React Router DOM**: `^7.5.0`
- **TypeScript**: `~5.8.2`
- **Vite**: `^6.3.2`
- **Vitest**: `^3.1.2`
