# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repo layout

Single git repo at the root (`https://github.com/joaohonorato/gim-gerenciador-imoveis`), covering backend, frontend and docs together — run git commands from the repo root. `frontend/` used to have its own nested `.git`; that history was folded into this repo and no longer exists.

- `backend/` — Micronaut 4 + Java 21 REST API
- `frontend/` — Expo 57 (React Native for Web) app
- `run.ps1` — single entry point to run backend/frontend/e2e together or individually (`./run.ps1 -Target backend|frontend|dev|e2e|all`); `all` (the default) always runs E2E too, which registers real accounts and can fire real outbound calls (e.g. Resend) — use `-Target dev` to just bring backend+frontend up and keep them running without E2E. See the `run` skill in `.claude/skills/` before improvising a launch sequence.
- `docker-compose.yml` — local Postgres for dev (`docker compose up -d postgres`); env vars come from `.env.local` at the root, auto-loaded by `run.ps1`.
- `docs/` — see [`docs/README.md`](docs/README.md) for the full index. Highlights:
  - `docs/especificacao-produto.md` — the product/architecture spec (data model, state machine, API surface, design system, legal constraints). Treat it as the source of truth for business rules; read it before implementing new domain behavior.
  - `docs/jornadas-e-backlog-tecnico.md` — engineering-facing journey mapping (4 personas incl. Admin/Suporte) and the prioritized backlog derived from it (per-persona flows, KPIs, sequencing, RACI).
  - `docs/jornadas-e-prioridades-negocio.md` — business-facing counterpart scoped to the 3 revenue-relevant personas (Investidor/Proprietário/Inquilino), with a backlog prioritized by business impact and ready-to-use implementation prompts for still-open items. Prefer this over the engineering doc when the task is "what should we build next and why," not "how do we sequence a sprint."
  - `docs/matriz-acesso-por-rota.md` — living route × persona access-control matrix for every REST controller. Update it whenever a route's authorization logic changes; consult it before adding a new `{id}`/`{imovelId}` route.
  - `docs/catalogo-erros-api.md` — living `code → frontend message` catalog for every API error. Consult it before adding a new error code.
  - `docs/gherkin/*.feature` — Gherkin scenarios per persona (pt-BR), tagged `@implementado`/`@pendente`, meant to become integration tests later. Check here before writing a new integration test for a persona flow.
  - `docs/deploy-azure.md`, `docs/provisionamento-e-custos.md`, `docs/caso-de-negocio.md` — deploy runbook, infra/cost breakdown, and the product's business case. Not needed for day-to-day coding; read before infra or business-facing questions.
  - `docs-antigos/` — superseded by `docs/`; historical only, do not read or edit.
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
npx expo start --web    # http://localhost:8081
```
Expo 57's web target runs on Metro's default port (`8081`), not the `19006` used by older Expo web (webpack) — confirmed by actually running it this session, not assumed from memory. The backend's CORS config (`application.yml`/`.env.local`) already allows both `CORS_ALLOWED_ORIGIN_1` (default `:19006`, kept for compatibility) and `CORS_ALLOWED_ORIGIN_2` (default `:8081`), so a normal `./run.ps1 -Target frontend` / `-Target all` needs no changes. If you start the backend directly with `./gradlew.bat run` instead of through `run.ps1`, remember `.env.local` is only auto-loaded by `run.ps1` — source it yourself (or export the vars) if you hit an unexplained CORS failure from the web frontend.

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

### Domain model shape (see `docs/especificacao-produto.md` §3–4 for full detail)
`PROPRIETARIO 1—N IMOVEL 1—N UNIDADE 1—N CONTRATO`; `INQUILINO` and `CONTRATO` are independent (a tenant can hold multiple contracts). Every `IMOVEL` auto-creates one `UNIDADE` with `padrao=true` at creation, and the owner can add more via `POST /imoveis/{id}/unidades` (single or batch — e.g. a terreno with several casas, each split into apartments) — `CONTRATO`/`CONVITE`/`CANDIDATURA`/`CONTA` are already keyed by `unidade_id`, not `imovel_id`, so multi-unit needed no data migration for those; `CHAMADO` did (`unidade_id` added, derived automatically from the opening tenant's contrato). `POST /imoveis/{imovelId}/convites` takes an optional `unidadeId` in the body — omitted resolves to the unidade padrão, so single-unit properties (still the common case) need no extra UI step. `CONTRATO` has exactly one `GARANTIA` (never combined types), and approving a candidato validates no overlapping signed contrato period exists on the same unidade before creating one.

### Frontend
```
frontend/app/              # Expo Router (file-based routing); route groups: (auth), (owner), (tenant), (contrato)
frontend/src/api/          # apiFetch client (client.ts) + AsyncStorage-backed session token (session.ts)
frontend/src/design/       # Button, Card, StatusBadge, tokens (design system, see spec §6)
frontend/e2e/              # Playwright golden-path spec + helpers
```
- `apiFetch<T>` in `src/api/client.ts` auto-attaches the bearer token from `session` unless called with `{ auth: false }`, and throws `ApiException` (status + typed `ApiError`) on non-2xx responses — catch `ApiException` rather than raw fetch errors.
- `EXPO_PUBLIC_API_URL` env var overrides the backend base URL (defaults to `http://localhost:8080`).
- Design tokens (colors, spacing, radius) live in `frontend/src/design/tokens.ts`: flat surfaces, light-gray borders, no shadows/gradients, no dark mode, no custom fonts (OS default per platform), no icon library — status indicated by color **and** the status name spelled out (`StatusBadge`: colored dot + label) so meaning never depends on color alone — see the spec §6 for the full current-state inventory before adding new UI states or colors.
- See the `expo-screen` skill before adding or restyling a screen — it has the route-group/component/API-client conventions in one place.

## Scope notes
The tenant (inquilino) side now has full UI parity with its backend coverage: `(tenant)/tenant/index.tsx` (home — contracts, invites, owner/imóvel info, invite acceptance, logout), `(tenant)/tenant/pagamentos.tsx` (own payments), `(tenant)/tenant/chamados.tsx` (open/track maintenance chamados — only for a tenant with an active contrato on the imóvel's unidade), `(tenant)/tenant/perfil.tsx` (profile/avatar), and `(contrato)/locacao/[token].tsx` / `(contrato)/[id]/revisar.tsx` for garantia/document submission and signing. Per `docs/jornadas-e-backlog-tecnico.md`, "jornada completa do inquilino no frontend" (rank 1) is done — check there before assuming otherwise. SMTP/invite delivery and real e-signature are still stubbed behind ports/interfaces (`AssinaturaProvider`, `ConviteLinkSender`/`ResendConviteLinkSender`) — see the README's "fora do escopo" table for what's stubbed vs. what needs real integration. Postgres is no longer stubbed: it's the real default datastore for local dev (see Commands above).
