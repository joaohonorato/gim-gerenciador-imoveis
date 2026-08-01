// Resource-group-scoped provisioning for the Azure side of the deploy
// described in docs/deploy-azure.md. Deploy with:
//   az deployment group create --resource-group <rg> \
//     --template-file deploy/bicep/main.bicep --parameters @deploy/bicep/main.parameters.json \
//     --parameters containerAppsEnvName=... storageAccountName=...
//
// Scope is deliberately narrow — only resources where a real `az deployment
// group what-if` against the live production resource group came back
// clean (no diffs beyond Azure-computed defaults matching Azure-computed
// defaults). Everything else stays on the imperative az CLI path in
// deploy/azure-setup.sh / deploy/azure-custom-domain.sh:
//
//   - az login / gh auth login (interactive)
//   - the GitHub Actions service principal (Entra ID/Graph, not an RG resource)
//   - publishing GitHub Secrets (this template only produces outputs)
//   - the custom domain / DNS zone (already hosts unrelated Resend email
//     records, and the apex-domain validation flow is inherently an async,
//     multi-round polling operation, not a declarative resource)
//   - the Container App itself (backend). Its GHCR pull credential is a
//     Container-Apps-managed *secret* referenced by name
//     (`configuration.secrets` + `registries[].passwordSecretRef`) —
//     `az containerapp show` never returns secret values, so there's no
//     safe way to read the current value back through a Bicep parameter,
//     and this is exactly the failure mode in gotcha #6 of
//     docs/deploy-azure.md (GHCR credential silently disappearing,
//     replicas stuck at 0)
//   - both Static Web Apps. A real `what-if` against them showed diffs on
//     `deploymentAuthPolicy`, `provider`, `repositoryUrl`, `branch` —
//     properties that plausibly govern how the token-based CI/CD deploy
//     authenticates. Whether re-applying actually resets them (vs. ARM
//     just reporting them as "not in template") isn't something to guess
//     against a resource serving real production traffic.

@description('Azure region for all resources.')
param location string = resourceGroup().location

@description('Container Apps managed environment name.')
param containerAppsEnvName string

@description('Log Analytics workspace backing the environment — pass the existing workspace name to adopt it.')
param logAnalyticsWorkspaceName string

@description('Globally-unique Storage Account name for avatars/fotos/documentos.')
param storageAccountName string

module containerAppsEnv 'modules/containerapp-env.bicep' = {
  name: 'containerapps-env'
  params: {
    environmentName: containerAppsEnvName
    logAnalyticsWorkspaceName: logAnalyticsWorkspaceName
    location: location
  }
}

module storage 'modules/storage.bicep' = {
  name: 'storage'
  params: {
    accountName: storageAccountName
    location: location
  }
}

output containerAppsEnvName string = containerAppsEnv.outputs.environmentName
@secure()
output storageConnectionString string = storage.outputs.connectionString
