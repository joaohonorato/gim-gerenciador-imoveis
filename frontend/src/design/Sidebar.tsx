import { useState } from 'react';
import { Pressable, Text, View } from 'react-native';
import { router, usePathname } from 'expo-router';
import { colors, spacing } from './tokens';

interface NavItem {
  href: string;
  label: string;
}

const NAV_ITEMS: NavItem[] = [
  { href: '/imoveis', label: 'Imóveis' },
  { href: '/contratos', label: 'Contratos' },
  { href: '/pagamentos', label: 'Pagamentos' },
  { href: '/convites', label: 'Convites' },
  { href: '/candidaturas', label: 'Candidaturas' },
];

export const SIDEBAR_EXPANDED_WIDTH = 232;
export const SIDEBAR_COLLAPSED_WIDTH = 56;
const TOGGLE_SIZE = 36;

// Flag de rollback: o design system (CLAUDE.md/spec §6) é "flat, sem
// sombra/gradiente" — esse sombreamento no toggle é uma exceção pontual
// pedida pro botão de menu. Se não agradar, virar pra `false` reverte só
// essa sombra sem mexer no resto do componente.
const TOGGLE_SHADOW_ENABLED = true;
const toggleShadowStyle = TOGGLE_SHADOW_ENABLED
  ? {
      shadowColor: '#000000',
      shadowOffset: { width: 0, height: 2 },
      shadowOpacity: 0.18,
      shadowRadius: 5,
      elevation: 3,
    }
  : {};

// Três barras horizontais desenhadas com View — não há biblioteca de ícones
// no design system (ver CLAUDE.md), então o hamburguer segue a mesma
// convenção do StatusBadge de desenhar formas simples com View em vez de
// depender de um ícone importado.
function HamburgerIcon({ color }: { color: string }) {
  return (
    <View style={{ gap: 4 }}>
      {[0, 1, 2].map((i) => (
        <View key={i} style={{ width: 16, height: 2, backgroundColor: color }} />
      ))}
    </View>
  );
}

// Navegação lateral pro portal web em janela larga (Onda 3 item 6,
// docs/jornadas-e-backlog-tecnico.md) — mesmas rotas da <Tabs> mobile,
// trocando só a forma da navegação. Ver (owner)/_layout.tsx: em desktop web
// a <Tabs> continua montada (mesmas telas, mesmo estado de navegação) só
// com a tab bar padrão escondida (tabBar={() => null}); este componente é
// renderizado ao lado, como a navegação visível.
//
// Fundo azul (mesmo colors.accent do painel esquerdo da tela de login) pra
// separação clara do conteúdo em cinza claro (colors.surface) — não é mais
// só uma borda de 1px.
//
// `collapsed` é controlado pelo _layout (não local): o botão de toggle
// (SidebarToggle, abaixo) precisa ficar fora da árvore do Sidebar/conteúdo
// pra não ser coberto pelo layer com transform que o ScrollView do conteúdo
// usa no React Native Web — um <Pressable> absolute *dentro* do Sidebar com
// zIndex alto ainda ficava atrás desse layer e não recebia clique.
// Renderizado como último irmão na row do _layout, ele pinta por cima dos
// dois sem precisar brigar de zIndex.
export function Sidebar({ collapsed }: { collapsed: boolean }) {
  const pathname = usePathname();

  return (
    <View
      style={{
        width: collapsed ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH,
        backgroundColor: colors.accent,
        paddingTop: spacing.xl,
      }}
    >
      {!collapsed && (
        <Text
          style={{
            fontSize: 18,
            fontWeight: '800',
            color: '#FFFFFF',
            paddingHorizontal: spacing.lg,
            marginBottom: spacing.xl,
          }}
        >
          Gestão de Imóveis
        </Text>
      )}
      {!collapsed &&
        NAV_ITEMS.map((item) => {
          const active = pathname.startsWith(item.href);
          return (
            <Pressable
              key={item.href}
              onPress={() => router.push(item.href as never)}
              accessibilityRole="link"
              style={{
                paddingHorizontal: spacing.lg,
                paddingVertical: spacing.sm + 4,
                borderLeftWidth: 3,
                borderLeftColor: active ? '#FFFFFF' : 'transparent',
                backgroundColor: active ? 'rgba(255,255,255,0.14)' : 'transparent',
              }}
            >
              <Text
                style={{
                  fontSize: 14,
                  fontWeight: '600',
                  color: active ? '#FFFFFF' : 'rgba(255,255,255,0.78)',
                }}
              >
                {item.label}
              </Text>
            </Pressable>
          );
        })}
    </View>
  );
}

// Botão circular flutuando exatamente na costura entre menu e conteúdo
// (metade sobre o azul, metade sobre o cinza) — ver nota acima sobre por
// que ele mora fora do Sidebar. `left` é calculado pelo caller a partir da
// largura atual do menu (SIDEBAR_EXPANDED_WIDTH/SIDEBAR_COLLAPSED_WIDTH).
export function SidebarToggle({
  collapsed,
  onToggle,
  left,
}: {
  collapsed: boolean;
  onToggle: () => void;
  left: number;
}) {
  const [hovered, setHovered] = useState(false);

  // Expandido = azul com ícone branco por padrão; recolhido = branco com
  // ícone azul por padrão. Hover inverte as cores do estado atual (não é
  // um valor fixo de hover) — dá o mesmo feedback "cor trocada" nos dois
  // sentidos do toggle.
  const blueBg = hovered ? collapsed : !collapsed;

  return (
    <Pressable
      onPress={onToggle}
      onHoverIn={() => setHovered(true)}
      onHoverOut={() => setHovered(false)}
      accessibilityRole="button"
      accessibilityLabel={collapsed ? 'Expandir menu' : 'Recolher menu'}
      hitSlop={4}
      style={{
        position: 'absolute',
        // Alinhado ao centro vertical do título "Gestão de Imóveis" do
        // Sidebar (paddingTop spacing.xl + ~metade da linha do texto).
        top: 25,
        left: left - TOGGLE_SIZE / 2,
        width: TOGGLE_SIZE,
        height: TOGGLE_SIZE,
        borderRadius: TOGGLE_SIZE / 2,
        borderWidth: 1.5,
        borderColor: colors.accent,
        backgroundColor: blueBg ? colors.accent : colors.card,
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 10,
        ...toggleShadowStyle,
      }}
    >
      <HamburgerIcon color={blueBg ? '#FFFFFF' : colors.accent} />
    </Pressable>
  );
}
