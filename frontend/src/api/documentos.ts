import * as DocumentPicker from 'expo-document-picker';
import { Platform } from 'react-native';
import { apiFetch } from './client';
import { ArquivoInfo } from './types';

const TIPOS_PERMITIDOS = ['application/pdf', 'image/jpeg', 'image/png', 'image/webp'];

/** Opens the document/image picker and returns the picked asset, or null if canceled. */
async function escolherArquivo() {
  const resultado = await DocumentPicker.getDocumentAsync({
    type: [...TIPOS_PERMITIDOS],
    multiple: false,
    copyToCacheDirectory: true,
  });
  if (resultado.canceled) return null;
  return resultado.assets?.[0] ?? null;
}

function buildFormData(campo: string, asset: DocumentPicker.DocumentPickerAsset): FormData {
  const formData = new FormData();
  if (Platform.OS === 'web' && asset.file) {
    formData.append(campo, asset.file, asset.name);
  } else {
    formData.append(campo, {
      uri: asset.uri,
      name: asset.name,
      type: asset.mimeType ?? 'application/octet-stream',
    } as unknown as Blob);
  }
  return formData;
}

export async function escolherEEnviarDocumentoContrato(contratoId: string): Promise<ArquivoInfo | null> {
  const asset = await escolherArquivo();
  if (!asset) return null;
  return apiFetch<ArquivoInfo>(`/contratos/${contratoId}/documento`, {
    method: 'POST',
    body: buildFormData('documento', asset),
  });
}

export async function escolherEEnviarDocumentoGarantia(contratoId: string): Promise<ArquivoInfo | null> {
  const asset = await escolherArquivo();
  if (!asset) return null;
  return apiFetch<ArquivoInfo>(`/contratos/${contratoId}/garantia/documentos`, {
    method: 'POST',
    body: buildFormData('documento', asset),
  });
}
