#!/usr/bin/env bash
# Wires a custom domain (registered elsewhere, e.g. Squarespace) to the
# Azure resources created by deploy/azure-setup.sh. Safe to re-run — it
# only binds the custom domains once the registrar's nameservers have
# actually been pointed at the Azure DNS zone this script creates.
#
# Usage: fill DOMAIN_NAME/FRONTEND_SUBDOMAIN/BACKEND_SUBDOMAIN in
# deploy/.env.azure, run ./deploy/azure-custom-domain.sh once to create the
# zone and print the nameservers to set at your registrar, update them
# there, then run this script again (as many times as needed) until it
# reports the domains are bound.
set -euo pipefail
export MSYS_NO_PATHCONV=1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env.azure"

if [ ! -f "$ENV_FILE" ]; then
  echo "Faltando $ENV_FILE — rode deploy/azure-setup.sh primeiro (ou copie de .env.azure.example)." >&2
  exit 1
fi
# shellcheck disable=SC1090
set -a; source "$ENV_FILE"; set +a

if [ -z "${DOMAIN_NAME:-}" ]; then
  echo "DOMAIN_NAME vazio em $ENV_FILE — sem domínio customizado configurado, nada a fazer."
  exit 0
fi

for var in AZURE_RESOURCE_GROUP AZURE_CONTAINER_APP_NAME AZURE_STATIC_WEB_APP_NAME \
           FRONTEND_SUBDOMAIN BACKEND_SUBDOMAIN GITHUB_REPO; do
  if [ -z "${!var:-}" ]; then
    echo "Variável obrigatória vazia em $ENV_FILE: $var" >&2
    exit 1
  fi
done

FRONTEND_DOMAIN="$FRONTEND_SUBDOMAIN.$DOMAIN_NAME"
BACKEND_DOMAIN="$BACKEND_SUBDOMAIN.$DOMAIN_NAME"

echo "==> Zona DNS ($DOMAIN_NAME)..."
if ! az network dns zone show --resource-group "$AZURE_RESOURCE_GROUP" --name "$DOMAIN_NAME" >/dev/null 2>&1; then
  az network dns zone create --resource-group "$AZURE_RESOURCE_GROUP" --name "$DOMAIN_NAME" --output none
else
  echo "    já existe, pulando."
fi

NAMESERVERS="$(az network dns zone show --resource-group "$AZURE_RESOURCE_GROUP" --name "$DOMAIN_NAME" \
  --query nameServers -o tsv | tr -d '\r')"

FRONTEND_HOSTNAME="$(az staticwebapp show --name "$AZURE_STATIC_WEB_APP_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
  --query defaultHostname -o tsv | tr -d '\r')"
BACKEND_FQDN="$(az containerapp show --name "$AZURE_CONTAINER_APP_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
  --query properties.configuration.ingress.fqdn -o tsv | tr -d '\r')"

echo "==> Registros CNAME ($FRONTEND_DOMAIN, $BACKEND_DOMAIN)..."
az network dns record-set cname set-record \
  --resource-group "$AZURE_RESOURCE_GROUP" --zone-name "$DOMAIN_NAME" \
  --record-set-name "$FRONTEND_SUBDOMAIN" --cname "$FRONTEND_HOSTNAME" --output none
az network dns record-set cname set-record \
  --resource-group "$AZURE_RESOURCE_GROUP" --zone-name "$DOMAIN_NAME" \
  --record-set-name "$BACKEND_SUBDOMAIN" --cname "$BACKEND_FQDN" --output none

echo "==> Registro TXT de verificação do Container Apps..."
VERIFICATION_ID="$(az containerapp show --name "$AZURE_CONTAINER_APP_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
  --query properties.customDomainVerificationId -o tsv | tr -d '\r')"
az network dns record-set txt add-record \
  --resource-group "$AZURE_RESOURCE_GROUP" --zone-name "$DOMAIN_NAME" \
  --record-set-name "asuid.$BACKEND_SUBDOMAIN" --value "$VERIFICATION_ID" --output none

echo "==> Checando se os nameservers do registrador já apontam pra Azure..."
if ! nslookup -type=NS "$DOMAIN_NAME" 8.8.8.8 2>&1 | grep -qi "azure-dns"; then
  cat <<INSTRUCTIONS

    Ainda não propagou (ou os nameservers ainda não foram trocados no
    registrador). Configure lá:

    $(echo "$NAMESERVERS" | sed 's/^/      /')

    Rode este script de novo depois que propagar (pode levar de minutos
    até ~48h, geralmente bem mais rápido).
INSTRUCTIONS
  exit 0
fi

echo "==> Propagado. Vinculando domínio customizado no Static Web App..."
az staticwebapp hostname set \
  --name "$AZURE_STATIC_WEB_APP_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
  --hostname "$FRONTEND_DOMAIN" --output none

echo "==> Vinculando domínio customizado + certificado gerenciado no Container App..."
az containerapp hostname add \
  --hostname "$BACKEND_DOMAIN" --resource-group "$AZURE_RESOURCE_GROUP" --name "$AZURE_CONTAINER_APP_NAME" \
  --output none
az containerapp hostname bind \
  --hostname "$BACKEND_DOMAIN" --resource-group "$AZURE_RESOURCE_GROUP" --name "$AZURE_CONTAINER_APP_NAME" \
  --environment "$AZURE_CONTAINERAPPS_ENV" --validation-method CNAME --output none

echo "==> Atualizando CORS do backend pro domínio customizado..."
az containerapp update \
  --name "$AZURE_CONTAINER_APP_NAME" --resource-group "$AZURE_RESOURCE_GROUP" \
  --set-env-vars \
    "CORS_ALLOWED_ORIGIN_1=https://$FRONTEND_DOMAIN" \
    "APP_CONVITES_FRONTEND_BASE_URL=https://$FRONTEND_DOMAIN" \
  --output none

echo "==> Publicando EXPO_PUBLIC_API_URL com o domínio customizado do backend..."
gh secret set EXPO_PUBLIC_API_URL --repo "$GITHUB_REPO" --body "https://$BACKEND_DOMAIN"

cat <<SUMMARY

==> Domínio customizado vinculado.
    Frontend: https://$FRONTEND_DOMAIN
    Backend:  https://$BACKEND_DOMAIN

    EXPO_PUBLIC_API_URL é build-time — dispare o workflow do frontend de
    novo pra ele pegar a nova URL do backend:
      gh workflow run azure-static-web-apps.yml --repo $GITHUB_REPO
SUMMARY
