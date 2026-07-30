# Deploy no Azure + Supabase

Arquitetura: **frontend** no Azure Static Web Apps (plano Free), **backend** no Azure Container Apps (plano Consumption, imagem no GHCR), **banco** no Supabase (Postgres gerenciado, fora do Azure — Azure não tem tier always-free de Postgres gerenciado). Provisionamento dos recursos via `deploy/azure-setup.sh`; deploy contínuo via GitHub Actions (`.github/workflows/azure-static-web-apps.yml` e `azure-container-apps-backend.yml`), disparado por push na `main`.

## Ordem de setup

O build do frontend embute a URL pública do backend (build-time, não runtime — ver gotcha 1), então o backend precisa estar deployado e com URL pública antes do primeiro build do frontend que for pra valer. `deploy/azure-setup.sh` automatiza tudo isso — provisiona os recursos do Azure, resolve a ordem/dependência entre backend e frontend sozinho, e já publica os GitHub Secrets necessários via `gh secret set`.

1. **Supabase** (manual — precisa da sua conta, não dá pra scriptar sem um token pessoal do Supabase): criar projeto. Em *Project Settings → Database → Connection pooling*, pegar host/usuário do **pooler em modo Session** (porta `5432`, não a `6543` de modo Transaction — ver gotcha 3). Usuário vem no formato `postgres.<project-ref>`, não só `postgres`.
2. `az login` e `gh auth login` (se ainda não estiver autenticado — `gh` já costuma estar).
3. `cp deploy/.env.azure.example deploy/.env.azure` e preencher: nomes dos recursos Azure, `GITHUB_REPO` e os dados de conexão do Supabase do passo 1.
4. Rodar `./deploy/azure-setup.sh`. Ele cria resource group, Container Apps environment, o Container App (com um placeholder até o primeiro deploy real) já configurado com as env vars do Supabase, o Static Web App, e fecha o loop de CORS sozinho (sabe a URL do frontend assim que cria o recurso, não precisa esperar um deploy). No final, publica `AZURE_CREDENTIALS`, `AZURE_RESOURCE_GROUP`, `AZURE_CONTAINERAPPS_ENV`, `AZURE_STATIC_WEB_APPS_API_TOKEN` e `EXPO_PUBLIC_API_URL` como GitHub Secrets. É seguro rodar de novo (idempotente — pula o que já existe, reatualiza env vars e credenciais).
5. Dar push na `main` (ou disparar os workflows manualmente) — os dois workflows em `.github/workflows/azure-*.yml` cuidam do build e deploy real de backend e frontend a partir daí.

Script não testado por mim de ponta a ponta (não tenho `az` CLI neste ambiente) — revisado com cuidado e com checagens de idempotência, mas a primeira rodada real é sua; me avisa se algum passo específico falhar.

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

`APP_PORT` não precisa ser setado — Container Apps injeta `PORT` e o `targetPort: 8080` do workflow já casa com o `EXPOSE 8080`/default do `application.yml`. Todas as variáveis acima já são setadas por `deploy/azure-setup.sh` a partir de `deploy/.env.azure` — a tabela é só referência caso precise ajustar manualmente depois (`az containerapp update --set-env-vars ...`).

## GitHub Secrets necessários

Todos publicados automaticamente por `deploy/azure-setup.sh` — a tabela é só referência.

| Secret | Usado por | Origem |
|---|---|---|
| `AZURE_CREDENTIALS` | workflow do backend | service principal criado pelo script (`az ad sp create-for-rbac`) |
| `AZURE_RESOURCE_GROUP`, `AZURE_CONTAINERAPPS_ENV`, `AZURE_CONTAINER_APP_NAME` | workflow do backend | nomes definidos em `deploy/.env.azure` |
| `AZURE_STATIC_WEB_APPS_API_TOKEN` | workflow do frontend | `az staticwebapp secrets list` |
| `EXPO_PUBLIC_API_URL` | workflow do frontend | URL pública do Container App, resolvida pelo script |

Não tem secret de registry — o workflow do backend builda/empurra pro **GHCR** (`ghcr.io`, grátis) usando o `GITHUB_TOKEN` embutido do próprio workflow, não Azure Container Registry (que não tem tier grátis).

## Gotchas

1. **`EXPO_PUBLIC_API_URL` é build-time.** Igual no runbook do Railway: o Expo embute isso no bundle JS durante `expo export`. Mudar depois exige novo build/deploy do frontend, não só reiniciar nada.
2. **CORS e `APP_TEST_SUPPORT_ENABLED=false`** — mesmos riscos de sempre, ver tabela acima.
3. **Pooler do Supabase: Session mode, não Transaction mode.** O backend usa Hibernate/JPA com HikariCP, que depende de prepared statements e às vezes de comandos `SET` por sessão — isso quebra ou fica instável no pooler em modo **Transaction** (porta `6543`), que não garante a mesma conexão física entre statements. O modo **Session** (porta `5432` via pooler) se comporta como uma conexão normal e é o caminho seguro pra Hibernate. Se mais pra frente quiser otimizar o número de conexões (Container Apps pode rodar múltiplas réplicas), dá pra migrar pro Transaction mode, mas exige desligar cache de prepared statement no driver (`prepareThreshold=0` na URL JDBC) e revisar o que usa transação/`SET` — não fazer isso sem necessidade.
4. **Limite de conexões do Supabase no free tier.** O plano free do Supabase tem um teto baixo de conexões diretas simultâneas (dezenas, não centenas) — é outro motivo pra usar o pooler em vez da conexão direta, e pra manter o pool do Hikari com um tamanho máximo razoável (default do Micronaut/HikariCP já é conservador, não precisa mexer pra esse volume de MVP).
5. **`serve -s` no frontend** ainda se aplica se algum dia trocar o Static Web Apps por um container próprio — não é o caso aqui, o Static Web Apps já lida com fallback de rota de SPA nativamente (`navigationFallback`), não tem 404 de refresh nas rotas do Expo Router.
6. **GHCR privado por padrão.** O pacote `ghcr.io/<repo>/backend` fica privado no seu GitHub por default — o `azure/container-apps-deploy-action` já configura o Container App com as credenciais de pull na mesma chamada que builda/empurra a imagem (usa o mesmo `registryUsername`/`registryPassword` do `GITHUB_TOKEN`), então não precisa deixar o pacote público nem configurar pull separadamente. Se preferir, dá pra tornar o pacote público depois (Settings do pacote no GitHub) — não muda nada no workflow.
7. **`backend/Dockerfile` usa a estrutura em camadas do Micronaut** (`build/docker/main/layers/`), não um `-all.jar` — ver comentário no próprio Dockerfile. Testado localmente com `docker build`/`docker run` antes de configurar o deploy; se o `build.gradle.kts` mudar a forma de empacotar, revalidar do mesmo jeito.
