# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repo layout

This root directory is **not** a git repository. `frontend/` is its own nested git repo (`frontend/.git`) — git commands must be run with `frontend` as the working directory and only version frontend files. There is no version control over `backend/` or root-level files.

- `backend/` — Micronaut 4 + Java 21 REST API
- `frontend/` — Expo 57 (React Native for Web) app, own git repo
- `gerenciador-imoveis-initial-prompt.md` — the original product/architecture spec (data model, state machine, API surface, design system, legal constraints). Treat it as the source of truth for business rules; read it before implementing new domain behavior.

## Commands

### Backend (run from `backend/`)
```bash
./gradlew.bat build                      # compile + test
./gradlew.bat run                        # start on http://localhost:8080
./gradlew.bat test                       # all tests (unit + integration)
./gradlew.bat test --tests "*domain*"    # domain-only unit tests (fast)
./gradlew.bat test --tests "br.com.imoveis.application.usecase.AprovarCandidatoTest"  # single test class
```
Uses H2 in-memory by default (profiles `default`/`test`, `MODE=PostgreSQL`). Switching to Postgres only requires activating the `prod` profile in `application.yml` — no code changes.

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

### Domain model shape (see `gerenciador-imoveis-initial-prompt.md` §3–4 for full detail)
`PROPRIETARIO 1—N IMOVEL 1—N UNIDADE 1—N CONTRATO`; `INQUILINO` and `CONTRATO` are independent (a tenant can hold multiple contracts). Every `IMOVEL` auto-creates one `UNIDADE` with `padrao=true` at creation — the API and UI operate on `imovelId` and resolve internally to the unidade padrão, keeping the door open for splitting a property into multiple rentable units later without an API redesign. `CONTRATO` has exactly one `GARANTIA` (never combined types), and approving a candidato validates no overlapping signed contrato period exists on the same unidade before creating one.

### Frontend
```
frontend/app/              # Expo Router (file-based routing); route groups: (auth), (owner), (contrato)
frontend/src/api/          # apiFetch client (client.ts) + AsyncStorage-backed session token (session.ts)
frontend/src/design/       # Button, Card, StatusBadge, tokens (Bauhaus-style design system, see spec §6)
frontend/e2e/              # Playwright golden-path spec + helpers
```
- `apiFetch<T>` in `src/api/client.ts` auto-attaches the bearer token from `session` unless called with `{ auth: false }`, and throws `ApiException` (status + typed `ApiError`) on non-2xx responses — catch `ApiException` rather than raw fetch errors.
- `EXPO_PUBLIC_API_URL` env var overrides the backend base URL (defaults to `http://localhost:8080`).
- Design tokens (colors, fonts, spacing, radius) follow a Bauhaus aesthetic: flat surfaces, solid black borders, no shadows/gradients, status indicated by color **and** geometric shape (circle/square/triangle) for colorblind-safe reading — see the spec §6 before adding new UI states or colors.

## Scope notes
Backend covers the tenant (inquilino) side of every flow; there is deliberately no tenant-facing UI yet beyond what's needed for the owner golden path (contract signing happens via API for the inquilino in E2E, not through a screen). SMTP/invite delivery, real e-signature, and Postgres are all stubbed behind ports/interfaces (`AssinaturaProvider`, persistence adapters, invitation support endpoints) — see the README's "fora do escopo" table for what's stubbed vs. what needs real integration.
