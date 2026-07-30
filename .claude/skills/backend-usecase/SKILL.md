---
name: backend-usecase
description: Add or change backend business behavior in Gestão de Imóveis (Micronaut/Java hexagonal backend) — new use case, new state transition, new controller endpoint, new domain invariant. Use whenever a task touches backend/src/main/java/br/com/imoveis, before writing a class, to follow the existing architecture instead of improvising a new shape.
---

# Adding backend behavior (hexagonal architecture)

Layout: `backend/src/main/java/br/com/imoveis/{domain,application,infrastructure}`. `domain/` is plain Java (no framework annotations), `application/usecase/` orchestrates, `infrastructure/` wires it to Micronaut/JPA/HTTP. Never let `domain/` or `application/` import Micronaut/JPA types.

## 1. Domain first

- Construct aggregates via named factory methods, not public constructors: `Imovel.cadastrar(...)` for new instances, `Imovel.reconstituir(...)` for rehydrating from persistence. Follow the same split for any new aggregate.
- State transitions are methods **on the aggregate** (`unidade.reservar()`, `candidatura.aprovar()`), not setters called from a use case. They must enforce invariants themselves and throw `TransicaoInvalidaException` (invalid state transition) or `IllegalStateException` (other invariant violations) — don't push that validation into the use case layer.
- Check `docs/gerenciador-imoveis-initial-prompt.md` §3–4 before adding/changing an aggregate or transition — it's the source of truth for the data model and state machine, and lists invariants that are easy to silently break, e.g.:
  - A `CONTRATO` has exactly one `GARANTIA`, never combined types.
  - Two signed `CONTRATO`s on the same `UNIDADE` can never have overlapping `data_inicio`–`data_fim` — validated at candidato approval time, not left to the DB.
  - Every `IMOVEL` auto-creates one `UNIDADE` with `padrao=true`; the API/UI address `imovelId` and resolve to that unidade internally — don't expose `unidadeId` on existing endpoints just because it exists under the hood.
  - `IMOVEL.visibilidade` is always `privado` in the MVP (marketplace/public listing is future scope) — don't wire a public-search path against it yet.
  - LGPD: never let a use case return or leak data belonging to a different `proprietario` than the caller.

## 2. Use case

- One class per action in `application/usecase/`, `@Singleton @Transactional`, repository/gateway **ports** injected via constructor, single `execute(...)` method. No HTTP or JPA concerns in this layer.
- New ports go in `application/ports/` as interfaces; implement them in `infrastructure/persistence/` (JPA adapters, entities under `infrastructure/persistence/jpa/`) or `infrastructure/{auth,convite}/` for non-persistence gateways (see `ConviteLinkSender`/`ResendConviteLinkSender`, `AssinaturaProvider`/`StubAssinaturaProvider`, `PasswordHasher`/`Pbkdf2PasswordHasher` for the adapter-behind-a-port pattern). Domain aggregates are never JPA entities — the adapter translates between the two.
- Tenant isolation is enforced **inside the use case**, not by a filter: compare `proprietarioId` (or `inquilinoId`) on the loaded aggregate against `CurrentPrincipal.require(request)` before acting. Look at an existing use case (e.g. `AprovarCandidato`, `ContratosController`'s listing logic) for the pattern.

## 3. Controller + errors

- Thin controller in `infrastructure/rest/`, one per aggregate/resource (`ImoveisController`, `ContratosController`, `ChamadosController`, ...). It resolves the principal, calls the use case, maps to a DTO in `infrastructure/rest/dto/`.
- Use cases throw `NaoEncontradoException`, `ConflitoException`, `AutenticacaoInvalidaException`, `TransicaoInvalidaException` (or a new exception if none fits) — add the status mapping in `infrastructure/rest/GlobalErrorHandler` rather than catching/handling errors per-controller.

## 4. Tests

- Domain logic: plain unit test in `src/test/java/.../domain/...` (see `ContratoTest`, `UnidadeTest`).
- Use case logic: unit test against `Fake*Repository` in-memory fakes (`src/test/java/.../application/fakes/`) — add a new fake there if the use case needs a port that doesn't have one yet, following `FakeImovelRepository`/`FakeContratoRepository`/`FakeConviteRepository`.
- Cross-cutting/HTTP flows: integration test under `src/test/java/.../infrastructure/*IT.java` (`AuthFlowIT`, `GoldenPathIT`, `RecusarCandidatoIT`) — these boot a real embedded server on port 8080, so a backend already running elsewhere makes them fail with `BindException` (not a real bug; see the `run` skill).
- Run `./gradlew.bat test --tests "*domain*"` while iterating (fast, no server boot), full `./gradlew.bat test` before calling the change done.

## 5. Wiring a new screen to this endpoint

If the use case backs a new/changed UI flow, hand off to the `expo-screen` skill for the frontend half — it covers `apiFetch`/`ApiException` and where the screen file goes.
