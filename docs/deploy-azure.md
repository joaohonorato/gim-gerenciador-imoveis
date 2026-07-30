# Deploy no Azure + Supabase

Arquitetura: **frontend** no Azure Static Web Apps (plano Free), **backend** no Azure Container Apps (plano Consumption, imagem no GHCR), **banco** no Supabase (Postgres gerenciado, fora do Azure — Azure não tem tier always-free de Postgres gerenciado). Provisionamento dos recursos via `deploy/azure-setup.sh`; deploy contínuo via GitHub Actions (`.github/workflows/azure-static-web-apps.yml` e `azure-container-apps-backend.yml`), disparado por push na `main`.

## Ordem de setup

O build do frontend embute a URL pública do backend (build-time, não runtime — ver gotcha 1), então o backend precisa estar deployado e com URL pública antes do primeiro build do frontend que for pra valer. `deploy/azure-setup.sh` automatiza tudo isso — provisiona os recursos do Azure, resolve a ordem/dependência entre backend e frontend sozinho, e já publica os GitHub Secrets necessários via `gh secret set`.

1. **Supabase** (manual — precisa da sua conta, não dá pra scriptar sem um token pessoal do Supabase): criar projeto. Em *Project Settings → Database → Connection pooling*, pegar host/usuário do **pooler em modo Session** (porta `5432`, não a `6543` de modo Transaction — ver gotcha 3). Usuário vem no formato `postgres.<project-ref>`, não só `postgres`.
2. `az login` e `gh auth login` (se ainda não estiver autenticado — `gh` já costuma estar).
3. `cp deploy/.env.azure.example deploy/.env.azure` e preencher: nomes dos recursos Azure, `GITHUB_REPO` e os dados de conexão do Supabase do passo 1.
4. Rodar `./deploy/azure-setup.sh`. Ele cria resource group, Container Apps environment, o Container App (com um placeholder até o primeiro deploy real) já configurado com as env vars do Supabase, o Static Web App, e fecha o loop de CORS sozinho (sabe a URL do frontend assim que cria o recurso, não precisa esperar um deploy). No final, publica `AZURE_CREDENTIALS`, `AZURE_RESOURCE_GROUP`, `AZURE_CONTAINERAPPS_ENV`, `AZURE_STATIC_WEB_APPS_API_TOKEN` e `EXPO_PUBLIC_API_URL` como GitHub Secrets. É seguro rodar de novo (idempotente — pula o que já existe, reatualiza env vars e credenciais).
5. Dar push na `main` (ou disparar os workflows manualmente) — os dois workflows em `.github/workflows/azure-*.yml` cuidam do build e deploy real de backend e frontend a partir daí.

Testado de ponta a ponta contra uma subscription real (não só revisado) — backend e frontend estão no ar. Três bugs reais apareceram só ao rodar de verdade, todos corrigidos no script: resource providers não registrados (`Microsoft.OperationalInsights`, `Microsoft.Web` — comuns em subscription nova, o script não registra sozinho, tem que rodar `az provider register -n <nome> --wait` manualmente se acontecer), região sem capacidade (`eastus` deu `AKSCapacityHeavyUsage` — daí o default ter virado `eastus2`), e mangling de path/CRLF do git-bash na geração do `AZURE_CREDENTIALS` (dois bugs distintos, ver histórico de commits de `deploy/azure-setup.sh`).

## Domínio customizado (opcional)

`deploy/azure-custom-domain.sh` — mesmo padrão do `azure-setup.sh`, idempotente, roda depois dele. Preenche `DOMAIN_NAME`/`FRONTEND_SUBDOMAIN`/`BACKEND_SUBDOMAIN` em `deploy/.env.azure` (domínio precisa já estar registrado em algum lugar — Squarespace, Registro.br, etc.; o script não registra domínio, só cria a zona DNS no Azure e delega). Primeira rodada cria a zona + registros (CNAME de `app`/`api` pros recursos existentes, TXT `asuid.api` de verificação do Container Apps) e imprime os 4 nameservers pra configurar no registrador. Depois de trocar lá, roda de novo — ele checa se já propagou (`nslookup -type=NS`) e, se sim, vincula o domínio customizado no Static Web App e no Container App (com certificado gerenciado grátis), atualiza `CORS_ALLOWED_ORIGIN_1`/`APP_CONVITES_FRONTEND_BASE_URL` e republica `EXPO_PUBLIC_API_URL`. Se ainda não propagou, só imprime os nameservers de novo e sai sem erro — seguro rodar quantas vezes precisar.

Depois que vincular, dispara o workflow do frontend novamente (`gh workflow run azure-static-web-apps.yml`) — `EXPO_PUBLIC_API_URL` mudou e é build-time.

A parte de vínculo de domínio (`az staticwebapp hostname set` / `az containerapp hostname bind`) não foi testada ainda enquanto este doc era escrito — depende de propagação de DNS que leva tempo real. Mesmo aviso do restante do script: revisado com cuidado, mas confirme o resultado.

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
| `GHCR_PAT` | workflow do backend | **manual, não publicado pelo script** — PAT clássico (github.com/settings/tokens/new) com escopo `write:packages` (inclui read), sem expiração curta. Ver gotcha 6, é obrigatório, não opcional. |

Registry é **GHCR** (`ghcr.io`, grátis), não Azure Container Registry (sem tier grátis) — mas precisa do `GHCR_PAT` acima, não dá pra usar só o `GITHUB_TOKEN` do workflow.

## Gotchas

1. **`EXPO_PUBLIC_API_URL` é build-time.** Igual no runbook do Railway: o Expo embute isso no bundle JS durante `expo export`. Mudar depois exige novo build/deploy do frontend, não só reiniciar nada.
2. **CORS e `APP_TEST_SUPPORT_ENABLED=false`** — mesmos riscos de sempre, ver tabela acima.
3. **Pooler do Supabase: Session mode, não Transaction mode.** O backend usa Hibernate/JPA com HikariCP, que depende de prepared statements e às vezes de comandos `SET` por sessão — isso quebra ou fica instável no pooler em modo **Transaction** (porta `6543`), que não garante a mesma conexão física entre statements. O modo **Session** (porta `5432` via pooler) se comporta como uma conexão normal e é o caminho seguro pra Hibernate. Se mais pra frente quiser otimizar o número de conexões (Container Apps pode rodar múltiplas réplicas), dá pra migrar pro Transaction mode, mas exige desligar cache de prepared statement no driver (`prepareThreshold=0` na URL JDBC) e revisar o que usa transação/`SET` — não fazer isso sem necessidade.
4. **Limite de conexões do Supabase no free tier.** O plano free do Supabase tem um teto baixo de conexões diretas simultâneas (dezenas, não centenas) — é outro motivo pra usar o pooler em vez da conexão direta, e pra manter o pool do Hikari com um tamanho máximo razoável (default do Micronaut/HikariCP já é conservador, não precisa mexer pra esse volume de MVP).
5. **`serve -s` no frontend** ainda se aplica se algum dia trocar o Static Web Apps por um container próprio — não é o caso aqui, o Static Web Apps já lida com fallback de rota de SPA nativamente (`navigationFallback`), não tem 404 de refresh nas rotas do Expo Router.
6. **`GITHUB_TOKEN` não serve como credencial de pull do Container App — precisa de um PAT durável.** Descoberto em produção, não em teoria: o primeiro deploy funcionou (o pull acontece dentro do próprio job, com o token ainda válido), mas depois disso o app ficou com 0 réplicas e nenhum restart/scale-from-zero conseguia subir (`az containerapp logs show` só dizia "Could not find a replica for this app" — sem erro explícito de auth). Causa: `azure/container-apps-deploy-action` guarda o `registryPassword` recebido como a credencial *permanente* de pull do Container App, não só pra esse job — e `GITHUB_TOKEN` expira pouco depois do job terminar. Fix: gerar um PAT clássico com escopo `write:packages` (github.com/settings/tokens/new, sem expiração curta), salvar como secret `GHCR_PAT`, e usar esse no workflow em vez do `GITHUB_TOKEN`. Alternativa mais simples se não precisar manter a imagem privada: tornar o pacote GHCR público (Package settings → Change visibility) — nesse caso pull anônimo funciona e nem precisa de credencial nenhuma, mas essa mudança de visibilidade não é algo que a automação faz sozinha (ação sensível o suficiente pra pedir confirmação explícita).
7. **`backend/Dockerfile` usa a estrutura em camadas do Micronaut** (`build/docker/main/layers/`), não um `-all.jar` — ver comentário no próprio Dockerfile. Testado localmente com `docker build`/`docker run` antes de configurar o deploy; se o `build.gradle.kts` mudar a forma de empacotar, revalidar do mesmo jeito.
