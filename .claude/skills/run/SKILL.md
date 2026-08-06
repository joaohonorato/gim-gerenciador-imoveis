---
name: run
description: Launch or drive this project (backend, frontend, or E2E) — the app-specific recipe the generic "run" skill looks for. Use whenever asked to start the backend/frontend, run the golden-path E2E, or check a change works end to end in Gestão de Imóveis.
---

# Running Gestão de Imóveis

Single entry point: `run.ps1` at the repo root. Prefer it over improvising `gradlew`/`expo`/`docker` sequences — it handles Postgres, env loading, health-checks and teardown for you.

```powershell
./run.ps1 -Target backend                    # backend only, foreground, http://localhost:8080
./run.ps1 -Target frontend                   # frontend only, foreground, http://localhost:19006
./run.ps1 -Target dev                        # backend + frontend in background, stays up, NO e2e — for normal local dev/use
./run.ps1 -Target e2e                        # starts backend+frontend in background, runs Playwright, tears down
./run.ps1 -Target e2e -ReuseExisting         # runs Playwright against services you already started yourself
./run.ps1 -Target all                        # backend + frontend + e2e, then tears down
./run.ps1 -Target all -KeepServices          # same, but leaves backend/frontend running after
./run.ps1 -NoInstall                         # skip `npm install` even if node_modules is missing
./run.ps1 -BackendPort 8080 -FrontendPort 19006
```

**`dev` vs `all`:** `all` (and the default `-Target all` when no flag is given) always runs the E2E suite as part of bringing things up — that suite registers real accounts through the real backend and, if `RESEND_API_KEY` is set in `.env.local` (needed for real invite delivery during normal manual testing), it fires a real outbound call to Resend too. If you just want the app running to click around or test manually, use `-Target dev` instead — it starts backend+frontend and leaves them up (Ctrl+C to stop) without touching E2E or creating any test data.

`run.ps1` auto-loads `.env.local` (repo root) into the environment for backend, frontend, and `docker compose` — edit that file rather than exporting vars by hand. It also brings up the local Postgres container (`docker compose up -d postgres`) before starting the backend; Docker Desktop must be running or this fails with a clear message.

## Manual / partial runs

- **Backend only, no Docker orchestration:** `cd backend && docker compose -f ../docker-compose.yml up -d postgres && ./gradlew.bat run` — the `default` Micronaut profile points at Postgres by default now (see CLAUDE.md), not H2.
- **Frontend only:** `cd frontend && npm install && npx expo start --web`.
- **E2E only, services already up:** `cd frontend && npx playwright test --config e2e/playwright.config.ts` (set `E2E_DISABLE_WEBSERVER=1` if Playwright's own `webServer` shouldn't try to start anything).

## Known pitfall: port 8080 conflicts

Backend integration tests (`AuthFlowIT`, `GoldenPathIT`, `RecusarCandidatoIT` under `backend/src/test/java/br/com/imoveis/infrastructure/`) boot a real embedded Micronaut server on port 8080. If a backend instance is already running (via `run.ps1`, `gradlew run`, or a leftover process), `./gradlew.bat test` fails those three with `BindException` — that's a port conflict, not a real test failure. Check `netstat -ano | grep :8080` (or free the port) before trusting a red IT result.

## Golden-path E2E, what it proves

`frontend/e2e/golden-path.spec.ts` drives: owner onboarding invite → password creation → login (email/password) → cadastro de imóvel via UI → convite → candidatura com senha via API → aprovação → assinatura do proprietário via UI → assinatura do inquilino via API → verifies 12 `PENDENTE` pagamentos were generated. If you change anything in that chain (auth, imóvel creation, convite/candidatura, contrato signing, pagamento generation), re-run this spec before calling the change done.

## Test-support endpoints (E2E/manual debugging only)

Guarded by `app.test-support.enabled` (on by default in `test`/`default` profiles):
- `GET /test-support/access-invites/{email}` — latest access-invite token, for onboarding flows without real email delivery.
- `GET /test-support/magic-links/{email}` — legacy, only kept for the old magic-link flow.

Never rely on these being enabled in a real deployment — they exist purely to unblock E2E/manual testing without SMTP.
