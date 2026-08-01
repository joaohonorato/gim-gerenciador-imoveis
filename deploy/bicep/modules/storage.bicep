@description('Globally-unique Storage Account name (3-24 lowercase alphanumeric chars, no dashes).')
param accountName string

param location string = resourceGroup().location

resource storageAccount 'Microsoft.Storage/storageAccounts@2023-01-01' = {
  name: accountName
  location: location
  sku: {
    name: 'Standard_LRS'
  }
  kind: 'StorageV2'
  properties: {
    allowBlobPublicAccess: true
    minimumTlsVersion: 'TLS1_2'
  }
}

resource blobServices 'Microsoft.Storage/storageAccounts/blobServices@2023-01-01' = {
  parent: storageAccount
  name: 'default'
}

// avatares / fotos-imoveis: public blob-level access (direct URL, cacheable).
resource containerAvatares 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-01-01' = {
  parent: blobServices
  name: 'avatares'
  properties: {
    publicAccess: 'Blob'
  }
}

resource containerFotosImoveis 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-01-01' = {
  parent: blobServices
  name: 'fotos-imoveis'
  properties: {
    publicAccess: 'Blob'
  }
}

// documentos: private — only accessible via short-lived SAS URL generated
// on-demand by the backend (GET /arquivos/{id}/url).
resource containerDocumentos 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-01-01' = {
  parent: blobServices
  name: 'documentos'
  properties: {
    publicAccess: 'None'
  }
}

output accountName string = storageAccount.name
@secure()
output connectionString string = 'DefaultEndpointsProtocol=https;AccountName=${storageAccount.name};AccountKey=${storageAccount.listKeys().keys[0].value};EndpointSuffix=${environment().suffixes.storage}'
