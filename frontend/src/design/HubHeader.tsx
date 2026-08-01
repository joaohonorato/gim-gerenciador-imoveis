import { useCallback, useState } from 'react';
import { Pressable, Text, View } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { apiFetch } from '@/api/client';
import { session } from '@/api/session';
import { MeResponse } from '@/api/types';
import { Button } from './Button';
import { Avatar } from './Avatar';

interface Props {
  title: string;
  subtitle?: string;
}

export function HubHeader({ title, subtitle }: Props) {
  const [loggingOut, setLoggingOut] = useState(false);
  const [me, setMe] = useState<MeResponse | null>(null);

  useFocusEffect(useCallback(() => {
    apiFetch<MeResponse>('/auth/me').then(setMe).catch(() => {});
  }, []));

  async function logout() {
    if (loggingOut) return;

    setLoggingOut(true);
    try {
      await apiFetch('/auth/logout', { method: 'POST' });
    } catch {
      // Mesmo com erro de rede/401, garantir logout local.
    } finally {
      await session.clear();
      router.replace('/login');
      setLoggingOut(false);
    }
  }

  return (
    <View className="px-6 pt-12 pb-4 bg-surface">
      <View className="flex-row items-start justify-between gap-4">
        <Pressable onPress={() => router.push('/perfil')} className="flex-row items-center gap-3 flex-1 pr-2">
          <Avatar url={me?.avatarUrl} nome={me?.nome} size={40} />
          <View className="flex-1">
            <Text className="text-primary mb-1" style={{ fontSize: 24, fontWeight: '800' }}>{title}</Text>
            {subtitle ? <Text className="text-muted" style={{ fontSize: 14 }}>{subtitle}</Text> : null}
          </View>
        </Pressable>
        <Button testID="btn-logout" label="Sair" onPress={logout} variant="outline" loading={loggingOut} disabled={loggingOut} />
      </View>
    </View>
  );
}
