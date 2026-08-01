#!/usr/bin/env bash
# One-time (but safely re-runnable) provisioning of the Azure Storage Account
# used for avatars, imóvel photos, and contrato/garantia documents.
#
# Usage:
#   # deploy/.env.azure must already exist (see azure-setup.sh) and now also
#   # define AZURE_STORAGE_ACCOUNT_NAME (globally unique, 3-24 lowercase
#   # alphanumeric chars — no dashes).
#   az login
#   ./deploy/azure-storage-setup.sh
set -euo pipefail

export MSYS_NO_PATHCONV=1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env.azure"

if ! command -v az >/dev/null 2>&1; then
  echo "Azure CLI (az) não encontrado." >&2
  exit 1
fi
if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI (gh) não encontrado." >&2
  exit 1
fi
if [ ! -f "$ENV_FILE" ]; then
  echo "Faltando $ENV_FILE — copie de deploy/.env.azure.example e preencha antes de rodar." >&2
  exit 1
fi

# shellcheck disable=SC1090
set -a; source "$ENV_FILE"; set +a

for var in AZURE_LOCATION AZURE_RESOURCE_GROUP AZURE_STORAGE_ACCOUNT_NAME AZURE_CONTAINER_APP_NAME GITHUB_REPO; do
  if [ -z "${!var:-}" ]; then
    echo "Variável obrigatória vazia em $ENV_FILE: $var" >&2
    exit 1
  fi
done

if ! az account show >/dev/null 2>&1; then
  echo "Não autenticado no Azure CLI. Rode 'az login' primeiro." >&2
  exit 1
fi

echo "==> Storage Account ($AZURE_STORAGE_ACCOUNT_NAME)..."
if ! az storage account show --name "$AZURE_STORAGE_ACCOUNT_NAME" --resource-group "$AZURE_RESOURCE_GROUP" >/dev/null 2>&1; then
  az storage account create \
    --name "$AZURE_STORAGE_ACCOUNT_NAME" \
    --resource-group "$AZURE_RESOURCE_GROUP" \
    --location "$AZURE_LOCATION" \
    --sku Standard_LRS \
    --kind StorageV2 \
    --allow-blob-public-access true \
    --min-tls-version TLS1_2 \
    --output none
else
  echo "    já existe, pulando."
fi

CONNECTION_STRING="$(az storage account show-connection-string \
  --name "$AZURE_STORAGE_ACCOUNT_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
  --query connectionString -o tsv | tr -d '\r')"

echo "==> Containers (avatares, fotos-imoveis públicos; documentos privado)..."
for container in avatares fotos-imoveis; do
  az storage container create \
    --name "$container" \
    --connection-string "$CONNECTION_STRING" \
    --public-access blob \
    --output none
  echo "    $container (público) ok"
done
az storage container create \
  --name documentos \
  --connection-string "$CONNECTION_STRING" \
  --public-access off \
  --output none
echo "    documentos (privado) ok"

echo "==> Publicando connection string no Container App e no GitHub Secrets..."
az containerapp update \
  --name "$AZURE_CONTAINER_APP_NAME" \
  --resource-group "$AZURE_RESOURCE_GROUP" \
  --set-env-vars "AZURE_STORAGE_CONNECTION_STRING=$CONNECTION_STRING" \
  --output none
gh secret set AZURE_STORAGE_CONNECTION_STRING --repo "$GITHUB_REPO" --body "$CONNECTION_STRING"

cat <<SUMMARY

==> Pronto.
    Storage Account: $AZURE_STORAGE_ACCOUNT_NAME
    Containers: avatares (público), fotos-imoveis (público), documentos (privado)
    Connection string aplicada no Container App '$AZURE_CONTAINER_APP_NAME' e
    publicada como secret AZURE_STORAGE_CONNECTION_STRING em $GITHUB_REPO.
SUMMARY
