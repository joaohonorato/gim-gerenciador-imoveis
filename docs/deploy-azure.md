# Deploy no Azure + Supabase

Arquitetura: **frontend** no Azure Static Web Apps (plano Free), **backend** no Azure Container Apps (plano Consumption), **banco** no Supabase (Postgres gerenciado, fora do Azure — Azure não tem tier always-free de Postgres gerenciado). Deploy via GitHub Actions (`.github/workflows/azure-static-web-apps.yml` e `azure-container-apps-backend.yml`), disparado por push na `main`.

## Ordem de setup

O build do frontend embute a URL pública do backend (build-time, não runtime — ver gotcha 1), então o backend precisa estar deployado e com URL pública antes do primeiro build do frontend que for pra valer.

1. **Supabase**: criar projeto. Em *Project Settings → Database → Connection pooling*, pegar host/porta/usuário do **pooler em modo Session** (porta `5432`, não a `6543` de modo Transaction — ver gotcha 3). Usuário vem no formato `postgres.<project-ref>`, não só `postgres`.
2. **Azure Container Apps**: criar o Container Apps Environment + o Container App (`gim-backend`) via portal/CLI. Configurar as variáveis de ambiente do app (seção abaixo). Criar um Azure Container Registry (ACR) se for usar o workflow como está — ou trocar por outro registry (GHCR, Docker Hub) ajustando os secrets do workflow.
3. Gerar as credenciais de deploy: service principal (`AZURE_CREDENTIALS`, via `az ad sp create-for-rbac --sdk-auth`) e credenciais do ACR (`ACR_LOGIN_SERVER`/`ACR_USERNAME`/`ACR_PASSWORD`, via `az acr credential show`). Colocar como GitHub Secrets do repo.
4. Dar push na `main` (ou rodar o workflow manualmente) pra fazer o primeiro deploy do backend. Anotar a URL pública gerada pro Container App (Settings → Ingress).
5. **Azure Static Web Apps**: criar o recurso (pode ser feito direto pelo assistente do GitHub no portal, que já cria o workflow — nesse caso, usar o workflow deste repo no lugar do gerado automaticamente, ou mesclar os dois). Pegar o *deployment token* (Overview → Manage deployment token) e salvar como secret `AZURE_STATIC_WEB_APPS_API_TOKEN`. Salvar a URL do backend (passo 4) como secret `EXPO_PUBLIC_API_URL`.
6. Dar push pra deployar o frontend. Anotar a URL pública do Static Web App.
7. Voltar no Container App e setar `CORS_ALLOWED_ORIGIN_1` e `APP_CONVITES_FRONTEND_BASE_URL` com a URL do passo 6.

## Variáveis de ambiente do Container App (backend)

| Variável | Valor |
|---|---|
| `DB_HOST` | host do pooler Supabase (Session mode), ex: `aws-0-<region>.pooler.supabase.com` |
| `DB_PORT` | `5432` (Session pooler — ver gotcha 3) |
| `DB_NAME` | `postgres` (default do Supabase) |
| `DB_USER` | `postgres.<project-ref>` (formato do pooler, não só `postgres`) |
| `DB_PASSWORD` | senha do banco definida na criação do projeto Supabase |
| `DB_SSLMODE` | `require` (Supabase exige SSL; o default do app é `prefer`, que já funciona, mas `require` é mais explícito) |
| `HIBERNATE_DDL_AUTO` | `update` (sem migrations reais no MVP — mesmo trade-off documentado no runbook antigo do Railway) |
| `CORS_ALLOWED_ORIGIN_1` | URL pública do Static Web App |
| `APP_CONVITES_FRONTEND_BASE_URL` | mesma URL do Static Web App |
| `APP_TEST_SUPPORT_ENABLED` | `false` — **obrigatório em produção**, os endpoints de test-support expõem tokens sem autenticação |
| `RESEND_API_KEY` / `RESEND_FROM_EMAIL` | credenciais reais de envio, se for usar |

`APP_PORT` não precisa ser setado — Container Apps injeta `PORT` e o `targetPort: 8080` do workflow já casa com o `EXPOSE 8080`/default do `application.yml`.

## GitHub Secrets necessários

| Secret | Usado por | Onde conseguir |
|---|---|---|
| `AZURE_CREDENTIALS` | workflow do backend | `az ad sp create-for-rbac --sdk-auth` |
| `ACR_LOGIN_SERVER`, `ACR_USERNAME`, `ACR_PASSWORD` | workflow do backend | `az acr credential show --name <registry>` |
| `AZURE_RESOURCE_GROUP`, `AZURE_CONTAINERAPPS_ENV` | workflow do backend | nomes do resource group / environment criados no passo 2 |
| `AZURE_STATIC_WEB_APPS_API_TOKEN` | workflow do frontend | Static Web App → Overview → Manage deployment token |
| `EXPO_PUBLIC_API_URL` | workflow do frontend | URL pública do Container App (passo 4) |

## Gotchas

1. **`EXPO_PUBLIC_API_URL` é build-time.** Igual no runbook do Railway: o Expo embute isso no bundle JS durante `expo export`. Mudar depois exige novo build/deploy do frontend, não só reiniciar nada.
2. **CORS e `APP_TEST_SUPPORT_ENABLED=false`** — mesmos riscos de sempre, ver tabela acima.
3. **Pooler do Supabase: Session mode, não Transaction mode.** O backend usa Hibernate/JPA com HikariCP, que depende de prepared statements e às vezes de comandos `SET` por sessão — isso quebra ou fica instável no pooler em modo **Transaction** (porta `6543`), que não garante a mesma conexão física entre statements. O modo **Session** (porta `5432` via pooler) se comporta como uma conexão normal e é o caminho seguro pra Hibernate. Se mais pra frente quiser otimizar o número de conexões (Container Apps pode rodar múltiplas réplicas), dá pra migrar pro Transaction mode, mas exige desligar cache de prepared statement no driver (`prepareThreshold=0` na URL JDBC) e revisar o que usa transação/`SET` — não fazer isso sem necessidade.
4. **Limite de conexões do Supabase no free tier.** O plano free do Supabase tem um teto baixo de conexões diretas simultâneas (dezenas, não centenas) — é outro motivo pra usar o pooler em vez da conexão direta, e pra manter o pool do Hikari com um tamanho máximo razoável (default do Micronaut/HikariCP já é conservador, não precisa mexer pra esse volume de MVP).
5. **`serve -s` no frontend** ainda se aplica se algum dia trocar o Static Web Apps por um container próprio — não é o caso aqui, o Static Web Apps já lida com fallback de rota de SPA nativamente (`navigationFallback`), não tem 404 de refresh nas rotas do Expo Router.
6. **`backend/Dockerfile` usa a estrutura em camadas do Micronaut** (`build/docker/main/layers/`), não um `-all.jar` — ver comentário no próprio Dockerfile. Testado localmente com `docker build`/`docker run` antes de configurar o deploy; se o `build.gradle.kts` mudar a forma de empacotar, revalidar do mesmo jeito.
