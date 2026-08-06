import { Platform } from 'react-native';
import * as Notifications from 'expo-notifications';
import Constants from 'expo-constants';
import { apiFetch } from './client';

// Canal complementar ao e-mail pros alertas de vencimento (ver
// docs/especificacao-produto.md §4 — contrato/garantia/conta). Só funciona
// de fato em dev build/produção mobile com um projeto EAS configurado
// (extra.eas.projectId em app.json, ainda não provisionado neste
// repositório — ver README "O que não está neste MVP"). Sem isso,
// getExpoPushTokenAsync lança e o registro é pulado silenciosamente, mesmo
// padrão de degradação graciosa já usado pro e-mail via Resend quando a
// chave não está configurada. Não suportado em Web (Expo SDK 57).
//
// A permissão do SO só pode ser pedida uma vez de forma "limpa" (recusar
// exige ir nas configurações do app depois) — por isso quem decide *quando*
// chamar requestPermissionsAsync é o primer (ver usePushPrimer), nunca este
// módulo sozinho.

async function registrarTokenAtual(): Promise<void> {
  const projectId = Constants.expoConfig?.extra?.eas?.projectId as string | undefined;
  if (!projectId) {
    console.warn('Push notification: projeto EAS não configurado (extra.eas.projectId em app.json) — registro pulado.');
    return;
  }

  const { data: token } = await Notifications.getExpoPushTokenAsync({ projectId });
  await apiFetch('/auth/push-token', {
    method: 'POST',
    body: JSON.stringify({ token }),
  });
}

export async function getPushPermissionStatus(): Promise<Notifications.PermissionStatus | null> {
  if (Platform.OS === 'web') return null;
  try {
    const { status } = await Notifications.getPermissionsAsync();
    return status;
  } catch (e) {
    console.warn('Não foi possível checar permissão de push', e);
    return null;
  }
}

/** Registra o token se a permissão já tiver sido concedida antes — nunca solicita. */
export async function registrarPushTokenSeJaPermitido(): Promise<void> {
  if (Platform.OS === 'web') return;
  try {
    const status = await getPushPermissionStatus();
    if (status === 'granted') {
      await registrarTokenAtual();
    }
  } catch (e) {
    console.warn('Não foi possível registrar push token', e);
  }
}

/** Solicita a permissão do SO (deve ser chamado só após o usuário confirmar o primer) e registra o token se concedida. */
export async function solicitarPermissaoERegistrarPush(): Promise<void> {
  if (Platform.OS === 'web') return;
  try {
    const { status } = await Notifications.requestPermissionsAsync();
    if (status !== 'granted') return;
    await registrarTokenAtual();
  } catch (e) {
    console.warn('Não foi possível registrar push token', e);
  }
}
