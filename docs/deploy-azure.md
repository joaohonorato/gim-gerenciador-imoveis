# Deploy no Azure + Supabase

Arquitetura: **app** (`frontend/`) no Azure Static Web Apps, **landing page** (`landing/`) em um *segundo* Static Web App, **backend** no Azure Container Apps (plano Consumption, imagem no GHCR), **banco** no Supabase (Postgres gerenciado, fora do Azure — Azure não tem tier always-free de Postgres gerenciado). Provisionamento via `deploy/azure-setup.sh`; deploy contínuo via GitHub Actions (`azure-static-web-apps.yml`, `azure-static-web-apps-landing.yml`, `azure-container-apps-backend.yml`), disparado por push na `main`.

**Por que dois Static Web Apps:** cada recurso Static Web Apps serve **um** conteúdo só, em todos os domínios customizados vinculados a ele — não dá pra ter `gim-imoveis.com` mostrando a landing e `app.gim-imoveis.com` mostrando o app de dentro do mesmo recurso. `gim-frontend` (app real, `frontend/`) fica só em `app.gim-imoveis.com`; `gim-landing` (`landing/`, HTML estático simples, sem build) fica no domínio raiz `gim-imoveis.com`.

## Ordem de setup

O build do frontend embute a URL pública do backend (build-time, não runtime — ver gotcha 1), então o backend precisa estar deployado e com URL pública antes do primeiro build do frontend que for pra valer. `deploy/azure-setup.sh` automatiza tudo isso — provisiona os recursos do Azure, resolve a ordem/dependência entre backend e frontend sozinho, e já publica os GitHub Secrets necessários via `gh secret set`.

1. **Supabase** (manual — precisa da sua conta, não dá pra scriptar sem um token pessoal do Supabase): criar projeto. Em *Project Settings → Database → Connection pooling*, pegar host/usuário do **pooler em modo Session** (porta `5432`, não a `6543` de modo Transaction — ver gotcha 3). Usuário vem no formato `postgres.<project-ref>`, não só `postgres`.
2. `az login` e `gh auth login` (se ainda não estiver autenticado — `gh` já costuma estar).
3. `cp deploy/.env.azure.example deploy/.env.azure` e preencher: nomes dos recursos Azure, `GITHUB_REPO` e os dados de conexão do Supabase do passo 1.
4. (Opcional, recomendado antes de rodar contra uma subscription com recursos reais) `./deploy/azure-setup.sh --what-if` — mostra só a parte declarativa (Container Apps environment + Storage Account, ver "Bicep" abaixo) sem aplicar nada.
5. Rodar `./deploy/azure-setup.sh`. Ele cria resource group, aplica o template Bicep (Container Apps environment + Storage Account com os 3 containers), cria o Container App (com um placeholder até o primeiro deploy real) já configurado com as env vars do Supabase, os dois Static Web Apps (app + landing), e fecha o loop de CORS sozinho (sabe a URL do frontend assim que cria o recurso, não precisa esperar um deploy). No final, publica `AZURE_CREDENTIALS`, `AZURE_RESOURCE_GROUP`, `AZURE_CONTAINERAPPS_ENV`, `AZURE_CONTAINER_APP_NAME`, `AZURE_STATIC_WEB_APPS_API_TOKEN`, `AZURE_LANDING_STATIC_WEB_APPS_API_TOKEN`, `EXPO_PUBLIC_API_URL` e `AZURE_STORAGE_CONNECTION_STRING` como GitHub Secrets. É seguro rodar de novo (idempotente — pula o que já existe, reatualiza env vars e credenciais).
6. Dar push na `main` (ou disparar os workflows manualmente) — os dois workflows em `.github/workflows/azure-*.yml` cuidam do build e deploy real de backend e frontend a partir daí.

Testado de ponta a ponta contra uma subscription real (não só revisado) — backend e frontend estão no ar. Três bugs reais apareceram só ao rodar de verdade, todos corrigidos no script: resource providers não registrados (`Microsoft.OperationalInsights`, `Microsoft.Web` — comuns em subscription nova, o script não registra sozinho, tem que rodar `az provider register -n <nome> --wait` manualmente se acontecer), região sem capacidade (`eastus` deu `AKSCapacityHeavyUsage` — daí o default ter virado `eastus2`), e mangling de path/CRLF do git-bash na geração do `AZURE_CREDENTIALS` (dois bugs distintos, ver histórico de commits de `deploy/azure-setup.sh`).

## Bicep (provisionamento parcialmente declarativo)

`deploy/bicep/main.bicep` + `deploy/bicep/modules/` definem, de forma declarativa, os únicos recursos onde um `az deployment group what-if` real contra a subscription de produção confirmou que reaplicar é um no-op seguro (só diffs em propriedades computadas pela Azure — default batendo com default): o **Container Apps environment** (+ Log Analytics workspace) e o **Storage Account** (+ os 3 containers de avatares/fotos/documentos). `deploy/azure-setup.sh` chama `az deployment group create` com esse template antes de tocar em qualquer outra coisa.

**Deliberadamente fora do Bicep**, e por quê (ver o comentário de cabeçalho de `main.bicep` para o detalhe técnico completo):

- **O Container App do backend.** A credencial de pull do GHCR fica guardada como um *secret* do próprio Container App (`configuration.secrets` + `registries[].passwordSecretRef`), e `az containerapp show` nunca retorna o valor de um secret — não tem como ler o valor atual e repassar de volta por um parâmetro Bicep com segurança. Reaplicar sem esse valor arriscaria repetir a gotcha 6 (credencial de pull sumindo, réplicas travadas em 0). O Container App continua 100% no caminho imperativo (`az containerapp create`/`update`), do jeito que sempre esteve.
- **Os dois Static Web Apps.** Um `what-if` real contra eles mostrou diffs em `deploymentAuthPolicy`, `provider`, `repositoryUrl`, `branch` — propriedades que plausivelmente controlam como o deploy baseado em token (usado pelos workflows) se autentica. Não dava pra confirmar com certeza se reaplicar de fato zera essas propriedades ou se é só o `what-if` reportando "não está no template" sem efeito real — e não é hipótese pra testar contra recursos servindo tráfego real. Ficam no caminho imperativo (`az staticwebapp create`), como antes.
- **A zona DNS / domínio customizado.** Já hospeda registros de e-mail do Resend (DKIM/SPF/DMARC) sem relação com o Container App, e o fluxo de vínculo do domínio raiz é assíncrono/multi-rodada (token de validação, polling de propagação — ver seção "Domínio customizado" abaixo) — não é uma boa forma de "recurso declarativo". `deploy/azure-custom-domain.sh` não mudou.

Validação antes de qualquer mudança real: `az bicep build --file deploy/bicep/main.bicep` (só sintaxe, não toca Azure) e `az deployment group what-if` (o `--what-if` do `azure-setup.sh` acima, somente leitura, mostra exatamente o que mudaria).

## Domínio customizado (opcional)

`deploy/azure-custom-domain.sh` — mesmo padrão do `azure-setup.sh`, idempotente, roda depois dele. Preenche `DOMAIN_NAME`/`FRONTEND_SUBDOMAIN`/`BACKEND_SUBDOMAIN` em `deploy/.env.azure` (domínio precisa já estar registrado em algum lugar — Squarespace, Registro.br, etc.; o script não registra domínio, só cria a zona DNS no Azure e delega). Primeira rodada cria a zona + registros (CNAME de `app`/`api` pros recursos existentes, TXT `asuid.api` de verificação do Container Apps, alias A record de `@` pro `gim-landing`) e imprime os 4 nameservers pra configurar no registrador. Depois de trocar lá, roda de novo — ele checa se já propagou (`nslookup -type=NS`) e, se sim, vincula `app`/`api` (fluxo CNAME, geralmente resolve numa rodada) e o domínio raiz na landing (fluxo TXT/apex — assíncrono, geralmente leva 2-3 rodadas do script: uma pra pedir o token de validação, outra pra criar o registro TXT depois que a Azure gera o token, uma última pra confirmar `Ready`). Se ainda não propagou nada, só imprime os nameservers de novo e sai sem erro — seguro rodar quantas vezes precisar. Se o domínio raiz ainda estiver vinculado ao `gim-frontend` de uma rodada anterior a existir a landing separada, o script desvincula de lá antes de vincular na landing.

Depois que vincular, dispara o workflow do frontend novamente (`gh workflow run azure-static-web-apps.yml`) — `EXPO_PUBLIC_API_URL` mudou e é build-time.

A parte de vínculo de domínio (`az staticwebapp hostname set` / `az containerapp hostname bind`) não foi testada ainda enquanto este doc era escrito — depende de propagação de DNS que leva tempo real. Mesmo aviso do restante do script: revisado com cuidado, mas confirme o resultado.

## Storage de arquivos (avatares, fotos, documentos)

Definido em `deploy/bicep/modules/storage.bicep` (ver seção "Bicep" acima) — Storage Account (nome em `AZURE_STORAGE_ACCOUNT_NAME` de `deploy/.env.azure`, ex. `gimimoveisstorage`) com 3 containers: `avatares` e `fotos-imoveis` (acesso público a nível de blob — URL direta, sem SAS) e `documentos` (privado — só acessível via URL assinada de curta duração, gerada sob demanda pelo backend em `GET /arquivos/{id}/url`). `deploy/azure-setup.sh` injeta a connection string (saída do deployment Bicep) como env var `AZURE_STORAGE_CONNECTION_STRING` no Container App e publica o mesmo valor como GitHub Secret (usado só localmente via `.env.local`, não pelo workflow — o backend lê a env var diretamente do Container App em produção).

## Schema do banco (Flyway)

Diferente de quando este doc foi escrito, o schema **não é mais gerenciado por `hibernate.hbm2ddl.auto=update`** — é dono do Flyway (`backend/src/main/resources/db/migration/`). Em produção, `HIBERNATE_DDL_AUTO` deve ficar em `validate` (o default do `application.yml` já é esse) — o Flyway aplica as migrations no boot; o Hibernate só confere se as entidades batem com o schema resultante, nunca altera nada. Um banco Supabase vazio roda todas as migrations `Vn__*.sql` do zero; um banco já provisionado antes da adoção do Flyway (sem a tabela `flyway_schema_history`) é baselineado automaticamente na V1 (`baseline-on-migrate: true`, `baseline-version: 1`) e só aplica migrations *depois* dela. **Qualquer mudança de schema em produção precisa de uma migration nova**, não só mudar a entidade JPA — ver `README.md` para o fluxo completo.

## Variáveis de ambiente do Container App (backend)

| Variável | Valor |
|---|---|
| `DB_HOST` | host do pooler Supabase (Session mode), ex: `aws-0-<region>.pooler.supabase.com` |
| `DB_PORT` | `5432` (Session pooler — ver gotcha 3) |
| `DB_NAME` | `postgres` (default do Supabase) |
| `DB_USER` | `postgres.<project-ref>` (formato do pooler, não só `postgres`) |
| `DB_PASSWORD` | senha do banco definida na criação do projeto Supabase |
| `DB_SSLMODE` | `require` (Supabase exige SSL; o default do app é `prefer`, que já funciona, mas `require` é mais explícito) |
| `HIBERNATE_DDL_AUTO` | `validate` (default do `application.yml`) — schema é gerenciado por Flyway, não editar isso pra `update`/`create` em produção (ver seção "Schema do banco" acima) |
| `AZURE_STORAGE_CONNECTION_STRING` | connection string do Storage Account (`deploy/bicep/modules/storage.bicep`) — habilita avatar/foto/documento; em branco, os endpoints de upload ficam indisponíveis (skip gracioso, mesmo padrão do `RESEND_API_KEY`) |
| `CORS_ALLOWED_ORIGIN_1` | URL pública do Static Web App |
| `APP_CONVITES_FRONTEND_BASE_URL` | mesma URL do Static Web App |
| `APP_TEST_SUPPORT_ENABLED` | `false` — **obrigatório em produção**, os endpoints de test-support expõem tokens sem autenticação |
| `RESEND_API_KEY` / `RESEND_FROM_EMAIL` | credenciais reais de envio, se for usar |

`APP_PORT` não precisa ser setado — Container Apps injeta `PORT` e o `targetPort: 8080` do workflow já casa com o `EXPOSE 8080`/default do `application.yml`. Todas as variáveis acima já são setadas por `deploy/azure-setup.sh` a partir de `deploy/.env.azure` — a tabela é só referência caso precise ajustar manualmente depois (`az containerapp update --set-env-vars ...`).

## GitHub Secrets necessários

Todos publicados automaticamente por `deploy/azure-setup.sh` (parte Bicep + parte imperativa, ver seção "Bicep" acima) — a tabela é só referência.

| Secret | Usado por | Origem |
|---|---|---|
| `AZURE_CREDENTIALS` | workflow do backend | service principal criado pelo script (`az ad sp create-for-rbac`) |
| `AZURE_RESOURCE_GROUP`, `AZURE_CONTAINERAPPS_ENV`, `AZURE_CONTAINER_APP_NAME` | workflow do backend | nomes definidos em `deploy/.env.azure` |
| `AZURE_STATIC_WEB_APPS_API_TOKEN` | workflow do frontend (app) | `az staticwebapp secrets list -n $AZURE_STATIC_WEB_APP_NAME` |
| `AZURE_LANDING_STATIC_WEB_APPS_API_TOKEN` | workflow da landing | `az staticwebapp secrets list -n $AZURE_LANDING_APP_NAME` |
| `EXPO_PUBLIC_API_URL` | workflow do frontend | URL pública do Container App, resolvida pelo script |
| `AZURE_STORAGE_CONNECTION_STRING` | não usado por workflow (só referência local) | saída (`output`) do deployment Bicep, publicado pra conveniência de quem for configurar `.env.local` — o valor que importa em produção é a env var do Container App, setada pelo mesmo script |
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
