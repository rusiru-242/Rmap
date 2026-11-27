$ErrorActionPreference = "Stop"

Write-Host "=== 1. Backend Verification (Maven Wrapper) ===" -ForegroundColor Cyan
Push-Location "$PSScriptRoot\..\backend"
.\mvnw.cmd test
Pop-Location

Write-Host "=== 2. Desktop Verification (Flutter) ===" -ForegroundColor Cyan
Push-Location "$PSScriptRoot\..\desktop"
flutter analyze
flutter test
flutter build windows --debug
Pop-Location

Write-Host "=== 3. Admin Portal Verification (React/TS/Vite) ===" -ForegroundColor Cyan
Push-Location "$PSScriptRoot\..\admin"
npm run typecheck
npm run test
npm run build
Pop-Location

Write-Host "All Phase 1 verification checks passed." -ForegroundColor Green
