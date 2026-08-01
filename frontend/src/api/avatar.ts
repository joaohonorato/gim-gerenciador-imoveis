import * as ImagePicker from 'expo-image-picker';
import { Platform } from 'react-native';
import { apiFetch } from './client';
import { MeResponse } from './types';

/** Opens the image library, uploads the pick as the current user's avatar,
 * and returns the refreshed MeResponse. Returns null if the user canceled. */
export async function escolherEEnviarAvatar(): Promise<MeResponse | null> {
  const permissao = await ImagePicker.requestMediaLibraryPermissionsAsync();
  if (!permissao.granted && Platform.OS !== 'web') {
    throw new Error('Permissão de acesso às fotos negada.');
  }

  const resultado = await ImagePicker.launchImageLibraryAsync({
    mediaTypes: ['images'],
    allowsMultipleSelection: false,
    quality: 0.9,
  });
  const asset = resultado.canceled ? null : resultado.assets?.[0];
  if (!asset) return null;

  const formData = new FormData();
  if (Platform.OS === 'web' && asset.file) {
    formData.append('avatar', asset.file, asset.fileName ?? 'avatar.jpg');
  } else {
    formData.append('avatar', {
      uri: asset.uri,
      name: asset.fileName ?? 'avatar.jpg',
      type: asset.mimeType ?? 'image/jpeg',
    } as unknown as Blob);
  }

  return apiFetch<MeResponse>('/auth/avatar', {
    method: 'POST',
    body: formData,
  });
}
