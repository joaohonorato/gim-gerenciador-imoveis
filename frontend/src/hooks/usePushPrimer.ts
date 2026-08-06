import { useCallback, useEffect, useState } from 'react';
import { Platform } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  getPushPermissionStatus,
  registrarPushTokenSeJaPermitido,
  solicitarPermissaoERegistrarPush,
} from '@/api/pushNotifications';

const PRIMER_SHOWN_KEY = 'push_primer_shown';

// Onda 1 item 1 (docs/jornadas-e-backlog-tecnico.md): o SO só deixa pedir a
// permissão de notificação "de graça" uma vez — recusar no prompt nativo
// exige o usuário ir nas configurações do app depois. Por isso pedimos
// consentimento explícito na UI (este primer) antes de disparar
// requestPermissionsAsync, e só mostramos o primer uma vez por instalação
// (persistido no AsyncStorage), independente da escolha do usuário.
export function usePushPrimer() {
  const [visible, setVisible] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let mounted = true;

    async function check() {
      if (Platform.OS === 'web') return;

      await registrarPushTokenSeJaPermitido();

      const status = await getPushPermissionStatus();
      if (!mounted || status === 'granted') return;

      const shown = await AsyncStorage.getItem(PRIMER_SHOWN_KEY);
      if (!mounted || shown) return;

      setVisible(true);
    }

    void check();

    return () => {
      mounted = false;
    };
  }, []);

  const dismiss = useCallback(async () => {
    setVisible(false);
    await AsyncStorage.setItem(PRIMER_SHOWN_KEY, '1');
  }, []);

  const onAccept = useCallback(async () => {
    setLoading(true);
    try {
      await solicitarPermissaoERegistrarPush();
    } finally {
      setLoading(false);
      await dismiss();
    }
  }, [dismiss]);

  const onDecline = useCallback(() => {
    void dismiss();
  }, [dismiss]);

  return { visible, loading, onAccept, onDecline };
}
