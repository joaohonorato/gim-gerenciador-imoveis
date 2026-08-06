import { ScrollView, ScrollViewProps } from 'react-native';
import { useIsDesktopWeb } from '@/hooks/useIsDesktopWeb';
import { maxContentWidth } from './tokens';

// Corpo rolável das telas de hub (imóveis, contratos, pagamentos, convites,
// candidaturas) — em desktop web, centraliza o conteúdo com largura máxima
// em vez de esticar borda a borda (Onda 3 item 6,
// docs/jornadas-e-backlog-tecnico.md). Em mobile/tablet nativo e web
// estreito, comporta-se como um ScrollView normal.
export function HubScrollView({ contentContainerStyle, ...props }: ScrollViewProps) {
  const isDesktopWeb = useIsDesktopWeb();

  return (
    <ScrollView
      {...props}
      contentContainerStyle={[
        isDesktopWeb ? { maxWidth: maxContentWidth, width: '100%', alignSelf: 'center' } : null,
        contentContainerStyle,
      ]}
    />
  );
}
