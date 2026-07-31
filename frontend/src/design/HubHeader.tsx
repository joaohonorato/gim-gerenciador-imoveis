import { useState } from 'react';
import { Text, View } from 'react-native';
import { router } from 'expo-router';
import { apiFetch } from '@/api/client';
import { session } from '@/api/session';
import { Button } from './Button';

interface Props {
  title: string;
  subtitle?: string;
}

export function HubHeader({ title, subtitle }: Props) {
  const [loggingOut, setLoggingOut] = useState(false);

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
        <View className="flex-1 pr-2">
          <Text className="text-primary mb-1" style={{ fontSize: 24, fontWeight: '800' }}>{title}</Text>
          {subtitle ? <Text className="text-muted" style={{ fontSize: 14 }}>{subtitle}</Text> : null}
        </View>
        <Button testID="btn-logout" label="Sair" onPress={logout} variant="outline" loading={loggingOut} disabled={loggingOut} />
      </View>
    </View>
  );
}
