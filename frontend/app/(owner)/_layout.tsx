import { useEffect, useState } from 'react';
import { ActivityIndicator, View } from 'react-native';
import { Tabs, router } from 'expo-router';
import { apiFetch } from '@/api/client';
import { session } from '@/api/session';
import { MeResponse } from '@/api/types';
import { colors } from '@/design/tokens';
import { PushPrimerModal } from '@/design/PushPrimerModal';
import { Sidebar, SidebarToggle, SIDEBAR_COLLAPSED_WIDTH, SIDEBAR_EXPANDED_WIDTH } from '@/design/Sidebar';
import { usePushPrimer } from '@/hooks/usePushPrimer';
import { useIsDesktopWeb } from '@/hooks/useIsDesktopWeb';

export default function OwnerLayout() {
  const [checkingAuth, setCheckingAuth] = useState(true);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const pushPrimer = usePushPrimer();
  const isDesktopWeb = useIsDesktopWeb();

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

  // Onda 3 item 6 (docs/jornadas-e-backlog-tecnico.md): em desktop web a
  // <Tabs> continua sendo o navegador (mesmas rotas/telas, mesmo estado de
  // navegação) — só a tab bar padrão (inferior, pensada pra mobile) é
  // escondida via tabBarStyle: { display: 'none' }, e a <Sidebar/> assume a
  // navegação visível ao lado. Em mobile/tablet nativo nada muda.
  const tabs = (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.accent,
        tabBarInactiveTintColor: colors.muted,
        tabBarStyle: isDesktopWeb
          ? { display: 'none' }
          : { backgroundColor: colors.card, borderTopColor: colors.border, borderTopWidth: 1 },
        tabBarLabelStyle: { fontSize: 12, fontWeight: '600' },
      }}
    >
      <Tabs.Screen name="imoveis" options={{ title: 'Imóveis' }} />
      <Tabs.Screen name="contratos" options={{ title: 'Contratos' }} />
      <Tabs.Screen name="pagamentos" options={{ title: 'Pagamentos' }} />
      <Tabs.Screen name="convites" options={{ title: 'Convites' }} />
      <Tabs.Screen name="candidaturas" options={{ title: 'Candidaturas' }} />
      <Tabs.Screen name="inquilinos/[id]" options={{ href: null }} />
      <Tabs.Screen name="perfil/index" options={{ href: null }} />
    </Tabs>
  );

  return (
    <>
      {isDesktopWeb ? (
        <View style={{ flex: 1, flexDirection: 'row', position: 'relative' }}>
          <Sidebar collapsed={sidebarCollapsed} />
          <View style={{ flex: 1 }}>{tabs}</View>
          <SidebarToggle
            collapsed={sidebarCollapsed}
            onToggle={() => setSidebarCollapsed((c) => !c)}
            left={sidebarCollapsed ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH}
          />
        </View>
      ) : (
        tabs
      )}
      <PushPrimerModal
        visible={pushPrimer.visible}
        loading={pushPrimer.loading}
        onAccept={pushPrimer.onAccept}
        onDecline={pushPrimer.onDecline}
      />
    </>
  );
}
