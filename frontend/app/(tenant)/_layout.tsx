import { useEffect, useState } from 'react';
import { ActivityIndicator, View } from 'react-native';
import { Stack, router } from 'expo-router';
import { apiFetch } from '@/api/client';
import { session } from '@/api/session';
import { MeResponse } from '@/api/types';

export default function TenantLayout() {
  const [checkingAuth, setCheckingAuth] = useState(true);

  useEffect(() => {
    let mounted = true;

    async function ensureTenant() {
      const token = await session.get();
      if (!mounted) return;

      if (!token) {
        router.replace('/login');
        return;
      }

      try {
        const me = await apiFetch<MeResponse>('/auth/me');
        if (!mounted) return;

        if (me.tipoConta !== 'INQUILINO') {
          router.replace('/imoveis');
          return;
        }

        setCheckingAuth(false);
      } catch {
        if (!mounted) return;
        await session.clear();
        router.replace('/login');
      }
    }

    void ensureTenant();

    return () => {
      mounted = false;
    };
  }, []);

  if (checkingAuth) {
    return (
      <View className="flex-1 items-center justify-center bg-surface">
        <ActivityIndicator color="#2563EB" />
      </View>
    );
  }

  return <Stack screenOptions={{ headerShown: false }} />;
}
