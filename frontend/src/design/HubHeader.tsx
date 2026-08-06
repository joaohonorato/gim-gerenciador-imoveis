import { useCallback, useState } from 'react';
import { Pressable, Text, View } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiFetch } from '@/api/client';
import { MeResponse } from '@/api/types';
import { useLogout } from '@/hooks/useLogout';
import { useIsDesktopWeb } from '@/hooks/useIsDesktopWeb';
import { Button } from './Button';
import { Avatar } from './Avatar';
import { maxContentWidth } from './tokens';

interface Props {
  title: string;
  subtitle?: string;
}

export function HubHeader({ title, subtitle }: Props) {
  const [me, setMe] = useState<MeResponse | null>(null);
  const { logout, loggingOut } = useLogout();
  const insets = useSafeAreaInsets();
  const isDesktopWeb = useIsDesktopWeb();

  useFocusEffect(useCallback(() => {
    apiFetch<MeResponse>('/auth/me').then(setMe).catch(() => {});
  }, []));

  return (
    <View className="px-6 pb-4 bg-surface" style={{ paddingTop: insets.top + 16 }}>
      <View
        className="flex-row items-start justify-between gap-4"
        style={isDesktopWeb ? { maxWidth: maxContentWidth, width: '100%', alignSelf: 'center' } : undefined}
      >
        <Pressable
          onPress={() => router.push('/perfil')}
          className="flex-row items-center gap-3 flex-1 pr-2"
          accessibilityRole="button"
          accessibilityLabel="Abrir meu perfil"
        >
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
