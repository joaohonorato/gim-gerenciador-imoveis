import { useEffect, useState } from 'react';
import { ActivityIndicator, View } from 'react-native';
import { Tabs, router } from 'expo-router';
import { apiFetch } from '@/api/client';
import { session } from '@/api/session';
import { MeResponse } from '@/api/types';
import { colors } from '@/design/tokens';

export default function OwnerLayout() {
  const [checkingAuth, setCheckingAuth] = useState(true);

  useEffect(() => {
    let mounted = true;

    async function ensureAuth() {
      const token = await session.get();

      if (!mounted) return;

      if (!token) {
        router.replace('/login');
        return;
      }

      try {
        const me = await apiFetch<MeResponse>('/auth/me');
        if (!mounted) return;

        if (me.tipoConta !== 'PROPRIETARIO') {
          router.replace('/tenant');
          return;
        }
      } catch {
        if (!mounted) return;
        await session.clear();
        router.replace('/login');
        return;
      }

      setCheckingAuth(false);
    }

    void ensureAuth();

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

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.accent,
        tabBarInactiveTintColor: colors.muted,
        tabBarStyle: { backgroundColor: colors.card, borderTopColor: colors.border, borderTopWidth: 1 },
        tabBarLabelStyle: { fontSize: 12, fontWeight: '600' },
      }}
    >
      <Tabs.Screen name="imoveis" options={{ title: 'Imóveis' }} />
      <Tabs.Screen name="contratos" options={{ title: 'Contratos' }} />
      <Tabs.Screen name="pagamentos" options={{ title: 'Pagamentos' }} />
      <Tabs.Screen name="convites" options={{ title: 'Convites' }} />
      <Tabs.Screen name="candidaturas" options={{ title: 'Candidaturas' }} />
      <Tabs.Screen name="inquilinos/[id]" options={{ href: null }} />
    </Tabs>
  );
}
