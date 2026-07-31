# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repo layout

Single git repo at the root (`https://github.com/joaohonorato/gim-gerenciador-imoveis`), covering backend, frontend and docs together — run git commands from the repo root. `frontend/` used to have its own nested `.git`; that history was folded into this repo and no longer exists.

- `backend/` — Micronaut 4 + Java 21 REST API
- `frontend/` — Expo 57 (React Native for Web) app
- `run.ps1` — single entry point to run backend/frontend/e2e together or individually (`./run.ps1 -Target backend|frontend|e2e|all`); see the `run` skill in `.claude/skills/` before improvising a launch sequence.
- `docker-compose.yml` — local Postgres for dev (`docker compose up -d postgres`); env vars come from `.env.local` at the root, auto-loaded by `run.ps1`.
- `docs/gerenciador-imoveis-initial-prompt.md` — the original product/architecture spec (data model, state machine, API surface, design system, legal constraints). Treat it as the source of truth for business rules; read it before implementing new domain behavior.
- `docs/journey-map.md`, `docs/plano-execucao-ajustes.md` — product/UX journey mapping and the prioritized backlog derived from it (per-persona flows, KPIs, sequencing). Consult these when a task touches UX flow, error messaging, or prioritization, not just when adding a screen.
- `.claude/skills/` — project-specific Claude Code skills (`backend-usecase`, `expo-screen`, `run`) encoding the recipes below as invocable workflows.

## Commands

### Backend (run from `backend/`)
```bash
./gradlew.bat build                      # compile + test
./gradlew.bat run                        # start on http://localhost:8080
./gradlew.bat test                       # all tests (unit + integration)
./gradlew.bat test --tests "*domain*"    # domain-only unit tests (fast)
./gradlew.bat test --tests "br.com.imoveis.application.usecase.AprovarCandidatoTest"  # single test class
```
`infrastructure/*IT.java` (`AuthFlowIT`, `GoldenPathIT`, `RecusarCandidatoIT`) boot a real embedded server on port 8080 — they fail with `BindException` if a backend is already running on that port (e.g. via `run.ps1` or `gradlew run` in another terminal). That's a port conflict, not a regression; free the port or stop the other instance first.
The `default` profile (`application.yml`) now points at real Postgres by default (`localhost:5432/imoveis`, matching `docker-compose.yml`'s defaults) — run `docker compose up -d postgres` (or `./run.ps1 -Target backend`, which does it for you) before `./gradlew.bat run`. Only the `test` profile (`application-test.yml`) uses H2 in-memory, so `./gradlew.bat test` never needs Postgres running. DB connection vars (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, ...) are read from the environment / `.env.local` — see `README.md` for the full list.

**Schema is owned by Flyway** (`backend/src/main/resources/db/migration/`), not by Hibernate — `hibernate.hbm2ddl.auto` is `validate` for every Postgres-backed profile (checks entities against the real schema at boot, never alters it). This exists specifically so local/prod/future dev/hml stay identical: point a fresh empty Postgres at the app and Flyway runs every migration for real; point it at an already-provisioned one (no `flyway_schema_history` table yet) and it gets auto-baselined at `V1` instead of re-running it. **Any schema change (new column, new enum value, new/changed constraint, new table) needs a new `Vn__description.sql` file** — editing a JPA entity alone is not enough and will fail `validate` at boot, by design (this is what let `ConviteStatus.REVOGADO` reach the app layer while production's `convites_status_check` constraint still rejected it, an outage `validate` will now catch immediately instead of on the first affected write). The H2 `test` profile is exempt (Flyway disabled there, `hbm2ddl.auto: update`, fresh ephemeral schema every run) since there's no drift to catch on a database that doesn't persist between runs.

### Frontend (run from `frontend/`)
```bash
npm install
npx expo start --web    # http://localhost:19006
```
Read `frontend/AGENTS.md` before writing Expo code — Expo 57 changed significantly from prior versions; check the versioned docs it points to rather than assuming older-Expo APIs.

### E2E (run from `frontend/`, requires backend + frontend running or let Playwright's `webServer` start them)
```bash
npx playwright test --config e2e/playwright.config.ts
```
The golden-path spec drives: owner onboarding invite → password creation → login with email/password → cadastro de imóvel via UI → convite → candidatura com senha via API → aprovação → assinatura do proprietário via UI → assinatura do inquilino via API → verifica 12 pagamentos `PENDENTE` gerados.

## Architecture

### Backend: hexagonal / clean architecture
```
backend/src/main/java/br/com/imoveis/
├── domain/           # aggregates, value objects, enums — plain Java, zero framework annotations
├── application/      # single-purpose use cases (one class = one action) + ports (repository/gateway interfaces)
└── infrastructure/   # Micronaut wiring: JPA/H2 adapters, REST controllers, auth filter
```
- **Domain objects are constructed via named factory methods**, not public constructors (e.g. `Imovel.cadastrar(...)` for new instances, `Imovel.reconstituir(...)` for rehydrating from persistence). State transitions are methods on the aggregate (e.g. `unidade.reservar()`, `candidatura.aprovar()`) that enforce invariants and throw `TransicaoInvalidaException`/`IllegalStateException` on invalid transitions.
- **Use cases** live in `application/usecase/`, are `@Singleton @Transactional`, take repository ports in the constructor, and expose a single `execute(...)` method. They orchestrate multiple aggregates/repositories but contain no framework or HTTP concerns.
- **Ports vs adapters**: `application/ports/` defines repository interfaces; `infrastructure/persistence/` implements them against JPA entities (`infrastructure/persistence/jpa/`) — domain aggregates are never JPA entities themselves, adapters translate between the two.
- **Errors**: use cases throw domain/application exceptions (`NaoEncontradoException`, `ConflitoException`, `AutenticacaoInvalidaException`, `TransicaoInvalidaException`); `infrastructure/rest/GlobalErrorHandler` maps these globally to HTTP status + `ErrorResponse` JSON. Add new exception → status mappings there rather than handling errors per-controller.

### Auth
Login now uses email + password, but onboarding still starts from invite links. `TokenAuthenticationFilter` (a global Micronaut `HttpServerFilter`) reads the `Bearer` token from every request, resolves it via `SessaoRepository`, and stashes a `Principal` (owner or tenant account) as a request attribute if the session hasn't expired. Controllers call `CurrentPrincipal.require(request)` to get the authenticated principal or fail with `AutenticacaoInvalidaException`. There's no separate authorization layer — tenant isolation (a proprietário only sees their own imóveis) is enforced inside use cases by comparing `proprietarioId` on the loaded aggregate against the caller's principal, not by a filter.

`/auth/convites/proprietarios` creates owner onboarding invites. `/auth/convites/{token}/aceitar` finalizes the account with email/password. `/test-support/access-invites/{email}` (guarded by `app.test-support.enabled`, on by default in `test`/`default` profiles) exposes the latest access-invite token for E2E. `/test-support/magic-links/{email}` remains only as transitional legacy support.

### Domain model shape (see `docs/gerenciador-imoveis-initial-prompt.md` §3–4 for full detail)
`PROPRIETARIO 1—N IMOVEL 1—N UNIDADE 1—N CONTRATO`; `INQUILINO` and `CONTRATO` are independent (a tenant can hold multiple contracts). Every `IMOVEL` auto-creates one `UNIDADE` with `padrao=true` at creation — the API and UI operate on `imovelId` and resolve internally to the unidade padrão, keeping the door open for splitting a property into multiple rentable units later without an API redesign. `CONTRATO` has exactly one `GARANTIA` (never combined types), and approving a candidato validates no overlapping signed contrato period exists on the same unidade before creating one.

### Frontend
```
frontend/app/              # Expo Router (file-based routing); route groups: (auth), (owner), (tenant), (contrato)
frontend/src/api/          # apiFetch client (client.ts) + AsyncStorage-backed session token (session.ts)
frontend/src/design/       # Button, Card, StatusBadge, tokens (Bauhaus-style design system, see spec §6)
frontend/e2e/              # Playwright golden-path spec + helpers
```
- `apiFetch<T>` in `src/api/client.ts` auto-attaches the bearer token from `session` unless called with `{ auth: false }`, and throws `ApiException` (status + typed `ApiError`) on non-2xx responses — catch `ApiException` rather than raw fetch errors.
- `EXPO_PUBLIC_API_URL` env var overrides the backend base URL (defaults to `http://localhost:8080`).
- Design tokens (colors, fonts, spacing, radius) follow a Bauhaus aesthetic: flat surfaces, solid black borders, no shadows/gradients, status indicated by color **and** geometric shape (circle/square/triangle) for colorblind-safe reading — see the spec §6 before adding new UI states or colors.
- See the `expo-screen` skill before adding or restyling a screen — it has the route-group/component/API-client conventions in one place.

## Scope notes
The tenant (inquilino) side has real backend coverage for every flow and a real UI: `(tenant)/tenant/index.tsx` is a functional inquilino home (contracts, invites, owner/imóvel info, invite acceptance, logout). It's newer and thinner than the owner side — contract *signing* by the inquilino is still exercised via API in E2E (`AssinarContratoPorConvite` / `(contrato)/locacao/[token].tsx` covers the token-based sign screen, but broader tenant screens like document/garantia submission or chamados are still API/E2E-only, not full UI). Per `docs/plano-execucao-ajustes.md`, "jornada completa do inquilino no frontend" is the top-priority backlog item — check there before assuming a tenant flow is UI-complete. SMTP/invite delivery and real e-signature are still stubbed behind ports/interfaces (`AssinaturaProvider`, `ConviteLinkSender`/`ResendConviteLinkSender`) — see the README's "fora do escopo" table for what's stubbed vs. what needs real integration. Postgres is no longer stubbed: it's the real default datastore for local dev (see Commands above).
