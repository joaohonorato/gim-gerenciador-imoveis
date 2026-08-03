import { useEffect, useState } from 'react';
import { View, Text, ActivityIndicator } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { apiFetch } from '@/api/client';
import { Button } from '@/design/Button';

export default function VerificarEmailScreen() {
  const { token } = useLocalSearchParams<{ token: string }>();
  const [status, setStatus] = useState<'carregando' | 'sucesso' | 'erro'>('carregando');
  const [error, setError] = useState('');

  useEffect(() => {
    let ativo = true;
    (async () => {
      try {
        await apiFetch('/auth/email/confirmar', {
          method: 'POST',
          body: JSON.stringify({ token }),
          auth: false,
        });
        if (ativo) setStatus('sucesso');
      } catch (e: any) {
        if (!ativo) return;
        setError(e.message ?? 'Não foi possível confirmar o e-mail — o link pode ter expirado.');
        setStatus('erro');
      }
    })();
    return () => { ativo = false; };
  }, [token]);

  return (
    <View className="flex-1 items-center justify-center bg-surface px-6 gap-4">
      {status === 'carregando' ? (
        <>
          <ActivityIndicator color="#2563EB" />
          <Text className="text-muted">Confirmando seu e-mail...</Text>
        </>
      ) : status === 'sucesso' ? (
        <>
          <Text className="text-primary" style={{ fontSize: 18, fontWeight: '700' }}>E-mail confirmado!</Text>
          <Button testID="btn-ir-login" label="Ir para o login" onPress={() => router.replace('/login')} />
        </>
      ) : (
        <>
          <Text className="text-primary" style={{ fontSize: 18, fontWeight: '700' }}>Não foi possível confirmar</Text>
          <Text className="text-muted" style={{ textAlign: 'center' }}>{error}</Text>
          <Button label="Ir para o login" variant="outline" onPress={() => router.replace('/login')} />
        </>
      )}
    </View>
  );
}
