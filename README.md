
# Gestão de Imóveis — MVP

SaaS multi-tenant para gestão de imóveis: backend Micronaut 4 + Java 21, frontend Expo 57 (Web), E2E Playwright.

Autenticação atual: login por e-mail e senha, com onboarding iniciado por convite.

## Pré-requisitos

- Java 21
- Node 24 + npm 11

## Script único (backend/frontend/e2e)

Na raiz do projeto, use o `run.ps1` para rodar tudo junto ou cada parte separada.

```powershell
# tudo (sobe backend + frontend, executa e2e e encerra os servicos)
./run.ps1 -Target all

# somente backend (foreground)
./run.ps1 -Target backend

# somente frontend (foreground)
./run.ps1 -Target frontend

# somente e2e (usa backend/frontend ja rodando)
./run.ps1 -Target e2e -ReuseExisting

# somente e2e (script sobe backend/frontend em background)
./run.ps1 -Target e2e

# manter backend/frontend ligados apos rodar e2e/all
./run.ps1 -Target all -KeepServices

# personalizar portas
./run.ps1 -Target all -BackendPort 8080 -FrontendPort 19006
```

Opcoes:
- `-Target`: `backend`, `frontend`, `e2e`, `all`
- `-ReuseExisting`: no alvo `e2e`, nao sobe servicos
- `-KeepServices`: no alvo `e2e`/`all`, nao encerra backend/frontend no fim
- `-NoInstall`: nao executa `npm install` quando `node_modules` estiver ausente
- `-BackendPort` / `-FrontendPort`: ajusta portas para health-check e frontend

## Backend

```bash
cd backend
./gradlew.bat build        # compila + testa (36 testes)
./gradlew.bat run          # sobe em http://localhost:8080
```

### Testes unitários
```bash
./gradlew.bat test --tests "*domain*"   # apenas domínio (rápido)
./gradlew.bat test                       # todos (unit + integration)
```

### Banco de dados
O backend agora usa **PostgreSQL por padrão** no desenvolvimento local.

As variaveis de ambiente locais ficam em `.env.local` (na raiz). O `run.ps1` carrega esse arquivo automaticamente para backend/frontend e para o `docker compose`.

Suba o banco com Docker na raiz do projeto:

```bash
docker compose up -d postgres
```

Parar/remover container e rede:

```bash
docker compose down
```

Remover tambem o volume de dados:

```bash
docker compose down -v
```

Defaults locais:
- host: `localhost`
- port: `5432`
- database: `imoveis`
- user: `imoveis`
- password: `imoveis`

Variáveis de ambiente suportadas no backend:
- `APP_NAME`
- `APP_PORT`
- `CORS_ENABLED`
- `CORS_ALLOWED_ORIGIN_1`
- `CORS_ALLOWED_ORIGIN_2`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `DB_DRIVER_CLASS_NAME`
- `DB_SCHEMA_GENERATE`
- `DB_DIALECT`
- `HIBERNATE_DDL_AUTO`
- `HIBERNATE_SHOW_SQL`
- `HIBERNATE_FORMAT_SQL`
- `FLYWAY_ENABLED`
- `APP_AUTH_SESSION_TOKEN_TTL_HOURS`
- `APP_AUTH_MAGIC_LINK_TTL_DAYS`
- `APP_CONVITES_FRONTEND_BASE_URL`
- `RESEND_API_KEY`
- `RESEND_FROM_EMAIL`
- `APP_TEST_SUPPORT_ENABLED`

Exemplo rapido de uso:

```powershell
# 1) edite .env.local conforme necessario

# 2) sobe banco + backend (script ja carrega .env.local)
./run.ps1 -Target backend
```

Observacao: os testes continuam usando `application-test.yml` com **H2 in-memory**, sem dependencia do Postgres.

O schema do Postgres (local, prod, e futuros dev/hml) e provisionado por Flyway (`backend/src/main/resources/db/migration/`), nao por `hibernate.hbm2ddl.auto` (que fica em `validate`, so checagem). Banco vazio → Flyway roda as migrations de verdade; banco ja existente sem `flyway_schema_history` → e baselineado na V1 automaticamente. Qualquer mudanca de schema precisa de um `Vn__descricao.sql` novo, nao so mudar a entidade JPA.

## Frontend

```bash
cd frontend
npm install
npx expo start --web    # http://localhost:19006
```

## E2E (Playwright)

Requer backend e frontend já rodando (ou deixa o Playwright subir via `webServer`):

```bash
cd frontend
npx playwright test --config e2e/playwright.config.ts
```

O teste golden-path cobre:
1. Convite de onboarding do proprietário, definição de senha e login por e-mail/senha
2. Cadastro de imóvel via UI
3. Convite → candidatura com senha → aprovação via API direta
4. Assinatura do proprietário via UI
5. Assinatura do inquilino via API
6. Verificação de 12 pagamentos `PENDENTE` gerados

### Fluxo de autenticação

- Um administrador da plataforma envia um convite de onboarding para o proprietário.
- O proprietário acessa o link do convite, confirma o e-mail e cria a própria senha.
- Depois disso, os acessos seguintes usam `/auth/login` com e-mail e senha.
- No fluxo de locação, o proprietário ou administrador envia o convite do inquilino/usuário, e o cadastro inicial também já define a senha da conta.

### Endpoints principais de auth

- `POST /auth/convites/proprietarios` cria convite de onboarding do proprietário.
- `GET /auth/convites/{token}` consulta os dados do convite público.
- `POST /auth/convites/{token}/aceitar` conclui o cadastro com e-mail e senha.
- `POST /auth/login` autentica com e-mail e senha.
- `GET /auth/me` retorna o usuário autenticado.

### Endpoints de suporte para testes

- `GET /test-support/access-invites/{email}` expõe o último token de convite de acesso para E2E.
- `GET /test-support/magic-links/{email}` continua existindo apenas como legado técnico enquanto o fluxo antigo não for removido por completo.

## Arquitetura

```
backend/src/main/java/br/com/imoveis/
├── domain/           # Agregados, value objects, enums — Java puro, zero anotações de framework
├── application/      # Use cases single-purpose + ports (interfaces)
└── infrastructure/   # Micronaut, JPA/H2, REST controllers, auth filter

frontend/
├── app/              # Expo Router (file-based routing)
│   ├── (auth)/login.tsx
│   ├── (owner)/imoveis/
│   └── (contrato)/[id]/revisar.tsx
├── src/api/          # cliente HTTP tipado + sessão AsyncStorage
└── src/design/       # Button, Card, StatusBadge, tokens

frontend/e2e/         # Playwright — golden-path.spec.ts
```

## O que não está neste MVP (costuras prontas para extensão)

| Fora do escopo | Cotura pronta |
|---|---|
| SMTP real | interfaces de envio de convite e stubs de teste |
| Assinatura eletrônica | `AssinaturaProvider` interface + stub |
| Postgres | `application-prod.yml` — só troca o driver |
| iOS/Android | Expo SDK — `expo run:ios/android` adiciona |
| Scheduler de alertas | Políticas de vencimento no domínio, scheduler separado |
| Telas do inquilino | Backend 100% coberto; UI do golden path só proprietário |
