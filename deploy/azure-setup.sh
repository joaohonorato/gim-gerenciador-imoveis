#!/usr/bin/env bash
# One-time (but safely re-runnable) provisioning for the Azure + Supabase
# deploy described in docs/deploy-azure.md.
#
# The Container Apps environment (+ its Log Analytics workspace) and the
# Storage Account are declared in deploy/bicep/ (main.bicep + modules/) and
# applied via `az deployment group create` below — a real `az deployment
# group what-if` against the live production resource group confirmed
# applying them is a no-op beyond Azure-computed defaults. Everything else
# (the backend Container App, both Static Web Apps) stays on the same
# imperative `az ... create`/`update` path as before this migration — see
# main.bicep's header comment for the specific, verified reasons each one
# was kept out.
#
# What it does NOT do: create the Supabase project (needs your account —
# do that first at supabase.com and fill deploy/.env.azure with the
# connection-pooler details), `az login` / `gh auth login` (interactive,
# run those yourself before this script), or the custom domain (see
# deploy/azure-custom-domain.sh).
#
# Usage:
#   cp deploy/.env.azure.example deploy/.env.azure   # fill it in
#   az login
#   ./deploy/azure-setup.sh              # applies for real
#   ./deploy/azure-setup.sh --what-if    # read-only preview of the Bicep part, no changes
set -euo pipefail

# git-bash/MSYS rewrites leading-slash args (like --scopes /subscriptions/...)
# into Windows paths unless this is set — without it, az ad sp create-for-rbac
# gets a mangled scope and fails with MissingSubscription.
export MSYS_NO_PATHCONV=1

WHAT_IF=false
if [ "${1:-}" = "--what-if" ]; then
  WHAT_IF=true
fi

# `set -e` only catches non-zero exit codes — an `az ... --query X -o tsv`
# that succeeds but resolves to null/empty (bad --query path, unexpected API
# shape) prints nothing and keeps going. That's especially dangerous right
# before a `gh secret set`: an empty secret degrades production silently
# instead of failing loudly. Every value that feeds a secret or a live
# env var gets checked with this before use.
require_nonempty() {
  if [ -z "$2" ]; then
    echo "ERRO: $1 veio vazio — aborta antes de publicar/aplicar algo quebrado." >&2
    exit 1
  fi
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env.azure"
# MSYS_NO_PATHCONV=1 (needed below so `az ad sp create-for-rbac --scopes
# /subscriptions/...` isn't mangled into a Windows path) also stops git-bash
# from converting *real* filesystem paths for az.exe — so these two, which
# az.exe does need as Windows paths, are converted explicitly instead.
BICEP_TEMPLATE="$(cygpath -w "$SCRIPT_DIR/bicep/main.bicep" 2>/dev/null || echo "$SCRIPT_DIR/bicep/main.bicep")"
BICEP_PARAMETERS="$(cygpath -w "$SCRIPT_DIR/bicep/main.parameters.json" 2>/dev/null || echo "$SCRIPT_DIR/bicep/main.parameters.json")"

if ! command -v az >/dev/null 2>&1; then
  echo "Azure CLI (az) não encontrado. Instale: https://learn.microsoft.com/cli/azure/install-azure-cli" >&2
  exit 1
fi
if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI (gh) não encontrado. Instale: https://cli.github.com/" >&2
  exit 1
fi
if [ ! -f "$ENV_FILE" ]; then
  echo "Faltando $ENV_FILE — copie de deploy/.env.azure.example e preencha antes de rodar." >&2
  exit 1
fi

# shellcheck disable=SC1090
set -a; source "$ENV_FILE"; set +a

for var in AZURE_LOCATION AZURE_RESOURCE_GROUP AZURE_CONTAINERAPPS_ENV AZURE_CONTAINER_APP_NAME \
           AZURE_STATIC_WEB_APP_NAME AZURE_LANDING_APP_NAME AZURE_SP_NAME AZURE_STORAGE_ACCOUNT_NAME \
           GITHUB_REPO SUPABASE_DB_HOST SUPABASE_DB_USER SUPABASE_DB_PASSWORD SUPABASE_DB_NAME; do
  if [ -z "${!var:-}" ]; then
    echo "Variável obrigatória vazia em $ENV_FILE: $var" >&2
    exit 1
  fi
done

if ! az account show >/dev/null 2>&1; then
  echo "Não autenticado no Azure CLI. Rode 'az login' primeiro." >&2
  exit 1
fi
if ! gh auth status >/dev/null 2>&1; then
  echo "Não autenticado no GitHub CLI. Rode 'gh auth login' primeiro." >&2
  exit 1
fi

az bicep install --only-show-errors >/dev/null 2>&1 || az bicep upgrade --only-show-errors >/dev/null 2>&1 || true

# az.cmd on Windows emits CRLF; command substitution only strips the
# trailing \n, so every `-o tsv` capture below is piped through `tr -d
# '\r'` — an embedded \r inside AZURE_CREDENTIALS_JSON's tenantId is
# exactly what broke `azure/login@v2` with "Bad control character".
SUBSCRIPTION_ID="$(az account show --query id -o tsv | tr -d '\r')"
echo "==> Subscription ativa: $SUBSCRIPTION_ID"

echo "==> Resource group ($AZURE_RESOURCE_GROUP)..."
az group create --name "$AZURE_RESOURCE_GROUP" --location "$AZURE_LOCATION" --output none

echo "==> Container Apps extension..."
az extension add --name containerapp --upgrade --only-show-errors --output none 2>/dev/null || true

# Existing Log Analytics workspace name (auto-created the first time
# `az containerapp env create` ran, before this migrated to Bicep) — adopt
# it instead of creating a duplicate. Falls back to the name already baked
# into main.parameters.json when the environment doesn't exist yet.
LOG_ANALYTICS_PARAM=()
if az containerapp env show --name "$AZURE_CONTAINERAPPS_ENV" --resource-group "$AZURE_RESOURCE_GROUP" >/dev/null 2>&1; then
  LOG_ANALYTICS_CUSTOMER_ID="$(az containerapp env show --name "$AZURE_CONTAINERAPPS_ENV" --resource-group "$AZURE_RESOURCE_GROUP" \
    --query "properties.appLogsConfiguration.logAnalyticsConfiguration.customerId" -o tsv | tr -d '\r')"
  EXISTING_WORKSPACE="$(az monitor log-analytics workspace list --resource-group "$AZURE_RESOURCE_GROUP" \
    --query "[?customerId=='$LOG_ANALYTICS_CUSTOMER_ID'].name | [0]" -o tsv | tr -d '\r')"
  if [ -n "$EXISTING_WORKSPACE" ]; then
    LOG_ANALYTICS_PARAM=(--parameters "logAnalyticsWorkspaceName=$EXISTING_WORKSPACE")
  fi
fi

DEPLOY_PARAMS=(
  --resource-group "$AZURE_RESOURCE_GROUP"
  --template-file "$BICEP_TEMPLATE"
  --parameters "@$BICEP_PARAMETERS"
  --parameters
    "containerAppsEnvName=$AZURE_CONTAINERAPPS_ENV"
    "storageAccountName=$AZURE_STORAGE_ACCOUNT_NAME"
    "location=$AZURE_LOCATION"
  "${LOG_ANALYTICS_PARAM[@]}"
)

if [ "$WHAT_IF" = true ]; then
  echo "==> what-if (somente leitura, nenhuma mudança será aplicada)..."
  az deployment group what-if "${DEPLOY_PARAMS[@]}"
  echo
  echo "==> what-if concluído. Rode sem --what-if pra aplicar de verdade."
  exit 0
fi

echo "==> Aplicando deployment Bicep (Container Apps env, Storage)..."
# Fixed name (not timestamped) so re-runs update the same deployment record
# instead of piling up new ones — Azure caps deployment history at 800 per
# resource group, and this script is meant to be re-run indefinitely.
DEPLOYMENT_NAME="gim-infra"
az deployment group create \
  --name "$DEPLOYMENT_NAME" \
  "${DEPLOY_PARAMS[@]}" \
  --output none

# Fetched directly via show-connection-string rather than through the Bicep
# deployment's @secure() output — az CLI/API behavior for returning secure
# output *values* (vs. redacting them, which only the Portal does) isn't
# something worth trusting un-tested for a value that silently degrades
# (empty connection string = uploads quietly stop working, no error).
# show-connection-string is the same proven call the pre-Bicep script used.
STORAGE_CONNECTION_STRING="$(az storage account show-connection-string \
  --name "$AZURE_STORAGE_ACCOUNT_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
  --query connectionString -o tsv | tr -d '\r')"
if [ -z "$STORAGE_CONNECTION_STRING" ]; then
  echo "ERRO: não foi possível obter a connection string do Storage Account $AZURE_STORAGE_ACCOUNT_NAME." >&2
  exit 1
fi

echo "==> Static Web App ($AZURE_STATIC_WEB_APP_NAME)..."
if ! az staticwebapp show --name "$AZURE_STATIC_WEB_APP_NAME" --resource-group "$AZURE_RESOURCE_GROUP" >/dev/null 2>&1; then
  az staticwebapp create \
    --name "$AZURE_STATIC_WEB_APP_NAME" \
    --resource-group "$AZURE_RESOURCE_GROUP" \
    --location "$AZURE_LOCATION" \
    --sku Free \
    --output none
else
  echo "    já existe, pulando."
fi

FRONTEND_HOSTNAME="$(az staticwebapp show --name "$AZURE_STATIC_WEB_APP_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
  --query defaultHostname -o tsv | tr -d '\r')"
require_nonempty "FRONTEND_HOSTNAME" "$FRONTEND_HOSTNAME"
FRONTEND_URL="https://$FRONTEND_HOSTNAME"
echo "    frontend: $FRONTEND_URL"

echo "==> Static Web App da landing page ($AZURE_LANDING_APP_NAME)..."
if ! az staticwebapp show --name "$AZURE_LANDING_APP_NAME" --resource-group "$AZURE_RESOURCE_GROUP" >/dev/null 2>&1; then
  az staticwebapp create \
    --name "$AZURE_LANDING_APP_NAME" \
    --resource-group "$AZURE_RESOURCE_GROUP" \
    --location "$AZURE_LOCATION" \
    --sku Free \
    --output none
else
  echo "    já existe, pulando."
fi

LANDING_HOSTNAME="$(az staticwebapp show --name "$AZURE_LANDING_APP_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
  --query defaultHostname -o tsv | tr -d '\r')"
require_nonempty "LANDING_HOSTNAME" "$LANDING_HOSTNAME"
LANDING_URL="https://$LANDING_HOSTNAME"
echo "    landing: $LANDING_URL"

LANDING_SWA_TOKEN="$(az staticwebapp secrets list --name "$AZURE_LANDING_APP_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
  --query "properties.apiKey" -o tsv | tr -d '\r')"
require_nonempty "LANDING_SWA_TOKEN" "$LANDING_SWA_TOKEN"

echo "==> Container App ($AZURE_CONTAINER_APP_NAME)..."
if ! az containerapp show --name "$AZURE_CONTAINER_APP_NAME" --resource-group "$AZURE_RESOURCE_GROUP" >/dev/null 2>&1; then
  # Placeholder image on first create — o workflow do GitHub Actions
  # substitui pela imagem real do backend (ghcr.io) no primeiro push.
  az containerapp create \
    --name "$AZURE_CONTAINER_APP_NAME" \
    --resource-group "$AZURE_RESOURCE_GROUP" \
    --environment "$AZURE_CONTAINERAPPS_ENV" \
    --image mcr.microsoft.com/azuredocs/containerapps-helloworld:latest \
    --target-port 80 \
    --ingress external \
    --min-replicas 0 \
    --max-replicas 2 \
    --output none
else
  echo "    já existe, pulando criação (env vars são atualizadas abaixo mesmo assim)."
fi

echo "==> Configurando variáveis de ambiente do backend..."
az containerapp update \
  --name "$AZURE_CONTAINER_APP_NAME" \
  --resource-group "$AZURE_RESOURCE_GROUP" \
  --set-env-vars \
    "DB_HOST=$SUPABASE_DB_HOST" \
    "DB_PORT=5432" \
    "DB_NAME=$SUPABASE_DB_NAME" \
    "DB_USER=$SUPABASE_DB_USER" \
    "DB_PASSWORD=$SUPABASE_DB_PASSWORD" \
    "DB_SSLMODE=require" \
    "HIBERNATE_DDL_AUTO=validate" \
    "APP_TEST_SUPPORT_ENABLED=false" \
    "RESEND_API_KEY=${RESEND_API_KEY:-}" \
    "RESEND_FROM_EMAIL=${RESEND_FROM_EMAIL:-no-reply@imoveis.local}" \
    "CORS_ALLOWED_ORIGIN_1=$FRONTEND_URL" \
    "APP_CONVITES_FRONTEND_BASE_URL=$FRONTEND_URL" \
    "AZURE_STORAGE_CONNECTION_STRING=$STORAGE_CONNECTION_STRING" \
  --output none

BACKEND_FQDN="$(az containerapp show --name "$AZURE_CONTAINER_APP_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
  --query properties.configuration.ingress.fqdn -o tsv | tr -d '\r')"
require_nonempty "BACKEND_FQDN" "$BACKEND_FQDN"
BACKEND_URL="https://$BACKEND_FQDN"
echo "    backend: $BACKEND_URL"

echo "==> Service principal para o GitHub Actions ($AZURE_SP_NAME)..."
SP_OUTPUT="$(az ad sp create-for-rbac \
  --name "$AZURE_SP_NAME" \
  --role Contributor \
  --scopes "/subscriptions/$SUBSCRIPTION_ID/resourceGroups/$AZURE_RESOURCE_GROUP" \
  --query "[appId,password,tenant]" -o tsv | tr -d '\r')"
# `-o tsv` on a flat 3-element array prints one value per line (CRLF on
# Windows, hence the tr above), not tab-separated on one line — despite
# the format's name. Extract by line number, not `cut -f`.
CLIENT_ID="$(echo "$SP_OUTPUT" | sed -n '1p')"
CLIENT_SECRET="$(echo "$SP_OUTPUT" | sed -n '2p')"
TENANT_ID="$(echo "$SP_OUTPUT" | sed -n '3p')"
require_nonempty "CLIENT_ID" "$CLIENT_ID"
require_nonempty "CLIENT_SECRET" "$CLIENT_SECRET"
require_nonempty "TENANT_ID" "$TENANT_ID"
AZURE_CREDENTIALS_JSON=$(cat <<JSON
{"clientId":"$CLIENT_ID","clientSecret":"$CLIENT_SECRET","subscriptionId":"$SUBSCRIPTION_ID","tenantId":"$TENANT_ID"}
JSON
)

echo "==> Token de deploy do Static Web App..."
SWA_TOKEN="$(az staticwebapp secrets list --name "$AZURE_STATIC_WEB_APP_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
  --query "properties.apiKey" -o tsv | tr -d '\r')"
require_nonempty "SWA_TOKEN" "$SWA_TOKEN"

echo "==> Publicando GitHub Secrets em $GITHUB_REPO..."
gh secret set AZURE_CREDENTIALS --repo "$GITHUB_REPO" --body "$AZURE_CREDENTIALS_JSON"
gh secret set AZURE_RESOURCE_GROUP --repo "$GITHUB_REPO" --body "$AZURE_RESOURCE_GROUP"
gh secret set AZURE_CONTAINERAPPS_ENV --repo "$GITHUB_REPO" --body "$AZURE_CONTAINERAPPS_ENV"
gh secret set AZURE_CONTAINER_APP_NAME --repo "$GITHUB_REPO" --body "$AZURE_CONTAINER_APP_NAME"
gh secret set AZURE_STATIC_WEB_APPS_API_TOKEN --repo "$GITHUB_REPO" --body "$SWA_TOKEN"
gh secret set AZURE_LANDING_STATIC_WEB_APPS_API_TOKEN --repo "$GITHUB_REPO" --body "$LANDING_SWA_TOKEN"
gh secret set EXPO_PUBLIC_API_URL --repo "$GITHUB_REPO" --body "$BACKEND_URL"
gh secret set AZURE_STORAGE_CONNECTION_STRING --repo "$GITHUB_REPO" --body "$STORAGE_CONNECTION_STRING"

cat <<SUMMARY

==> Pronto.
    Backend (Container App):   $BACKEND_URL
    Frontend (Static Web App): $FRONTEND_URL
    Landing (Static Web App):  $LANDING_URL
    Secrets publicados em $GITHUB_REPO: AZURE_CREDENTIALS, AZURE_RESOURCE_GROUP,
    AZURE_CONTAINERAPPS_ENV, AZURE_CONTAINER_APP_NAME, AZURE_STATIC_WEB_APPS_API_TOKEN,
    AZURE_LANDING_STATIC_WEB_APPS_API_TOKEN, EXPO_PUBLIC_API_URL, AZURE_STORAGE_CONNECTION_STRING.

    Próximo passo: dar push na main (ou re-rodar os workflows) pra disparar
    o primeiro deploy real do backend e do frontend — os dois workflows em
    .github/workflows/azure-*.yml cuidam do resto. Domínio customizado
    (opcional) continua em deploy/azure-custom-domain.sh, sem mudanças.
SUMMARY
