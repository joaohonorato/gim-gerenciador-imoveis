#!/usr/bin/env bash
# One-time (but safely re-runnable) provisioning for the Azure + Supabase
# deploy described in docs/deploy-azure.md.
#
# What it does NOT do: create the Supabase project (needs your account —
# do that first at supabase.com and fill deploy/.env.azure with the
# connection-pooler details), or `az login` / `gh auth login` (interactive,
# run those yourself before this script).
#
# Usage:
#   cp deploy/.env.azure.example deploy/.env.azure   # fill it in
#   az login
#   ./deploy/azure-setup.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env.azure"

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
           AZURE_STATIC_WEB_APP_NAME AZURE_SP_NAME GITHUB_REPO \
           SUPABASE_DB_HOST SUPABASE_DB_USER SUPABASE_DB_PASSWORD SUPABASE_DB_NAME; do
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

SUBSCRIPTION_ID="$(az account show --query id -o tsv)"
echo "==> Subscription ativa: $SUBSCRIPTION_ID"

echo "==> Resource group ($AZURE_RESOURCE_GROUP)..."
az group create --name "$AZURE_RESOURCE_GROUP" --location "$AZURE_LOCATION" --output none

echo "==> Container Apps extension..."
az extension add --name containerapp --upgrade --only-show-errors --output none 2>/dev/null || true

echo "==> Container Apps environment ($AZURE_CONTAINERAPPS_ENV)..."
if ! az containerapp env show --name "$AZURE_CONTAINERAPPS_ENV" --resource-group "$AZURE_RESOURCE_GROUP" >/dev/null 2>&1; then
  az containerapp env create \
    --name "$AZURE_CONTAINERAPPS_ENV" \
    --resource-group "$AZURE_RESOURCE_GROUP" \
    --location "$AZURE_LOCATION" \
    --output none
else
  echo "    já existe, pulando."
fi

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
    "HIBERNATE_DDL_AUTO=update" \
    "APP_TEST_SUPPORT_ENABLED=false" \
    "RESEND_API_KEY=${RESEND_API_KEY:-}" \
    "RESEND_FROM_EMAIL=${RESEND_FROM_EMAIL:-no-reply@imoveis.local}" \
  --output none

BACKEND_FQDN="$(az containerapp show --name "$AZURE_CONTAINER_APP_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
  --query properties.configuration.ingress.fqdn -o tsv)"
BACKEND_URL="https://$BACKEND_FQDN"
echo "    backend: $BACKEND_URL"

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
  --query defaultHostname -o tsv)"
FRONTEND_URL="https://$FRONTEND_HOSTNAME"
echo "    frontend: $FRONTEND_URL"

echo "==> Fechando o loop de CORS (backend passa a aceitar o frontend)..."
az containerapp update \
  --name "$AZURE_CONTAINER_APP_NAME" \
  --resource-group "$AZURE_RESOURCE_GROUP" \
  --set-env-vars \
    "CORS_ALLOWED_ORIGIN_1=$FRONTEND_URL" \
    "APP_CONVITES_FRONTEND_BASE_URL=$FRONTEND_URL" \
  --output none

echo "==> Service principal para o GitHub Actions ($AZURE_SP_NAME)..."
SP_OUTPUT="$(az ad sp create-for-rbac \
  --name "$AZURE_SP_NAME" \
  --role Contributor \
  --scopes "/subscriptions/$SUBSCRIPTION_ID/resourceGroups/$AZURE_RESOURCE_GROUP" \
  --query "[appId,password,tenant]" -o tsv)"
CLIENT_ID="$(echo "$SP_OUTPUT" | cut -f1)"
CLIENT_SECRET="$(echo "$SP_OUTPUT" | cut -f2)"
TENANT_ID="$(echo "$SP_OUTPUT" | cut -f3)"
AZURE_CREDENTIALS_JSON=$(cat <<JSON
{"clientId":"$CLIENT_ID","clientSecret":"$CLIENT_SECRET","subscriptionId":"$SUBSCRIPTION_ID","tenantId":"$TENANT_ID"}
JSON
)

echo "==> Token de deploy do Static Web App..."
SWA_TOKEN="$(az staticwebapp secrets list --name "$AZURE_STATIC_WEB_APP_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
  --query "properties.apiKey" -o tsv)"

echo "==> Publicando GitHub Secrets em $GITHUB_REPO..."
gh secret set AZURE_CREDENTIALS --repo "$GITHUB_REPO" --body "$AZURE_CREDENTIALS_JSON"
gh secret set AZURE_RESOURCE_GROUP --repo "$GITHUB_REPO" --body "$AZURE_RESOURCE_GROUP"
gh secret set AZURE_CONTAINERAPPS_ENV --repo "$GITHUB_REPO" --body "$AZURE_CONTAINERAPPS_ENV"
gh secret set AZURE_CONTAINER_APP_NAME --repo "$GITHUB_REPO" --body "$AZURE_CONTAINER_APP_NAME"
gh secret set AZURE_STATIC_WEB_APPS_API_TOKEN --repo "$GITHUB_REPO" --body "$SWA_TOKEN"
gh secret set EXPO_PUBLIC_API_URL --repo "$GITHUB_REPO" --body "$BACKEND_URL"

cat <<SUMMARY

==> Pronto.
    Backend (Container App):   $BACKEND_URL
    Frontend (Static Web App): $FRONTEND_URL
    Secrets publicados em $GITHUB_REPO: AZURE_CREDENTIALS, AZURE_RESOURCE_GROUP,
    AZURE_CONTAINERAPPS_ENV, AZURE_STATIC_WEB_APPS_API_TOKEN, EXPO_PUBLIC_API_URL.

    Próximo passo: dar push na main (ou re-rodar os workflows) pra disparar
    o primeiro deploy real do backend e do frontend — os dois workflows em
    .github/workflows/azure-*.yml cuidam do resto.
SUMMARY
