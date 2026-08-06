import { Platform, useWindowDimensions } from 'react-native';

const BREAKPOINT = 768;

// Onda 3 item 6 (docs/jornadas-e-backlog-tecnico.md): o portal web herdava a
// <Tabs> de navegação inferior do mobile sem adaptação nenhuma — janela
// larga com uma barra de abas mobile grudada embaixo. Só entra em modo
// desktop no próprio Expo Web (mobile nativo sempre usa a Tabs normal,
// mesmo em tablets — ver "fora de escopo" no pedido original sobre suporte
// a iPad).
export function useIsDesktopWeb(): boolean {
  const { width } = useWindowDimensions();
  return Platform.OS === 'web' && width >= BREAKPOINT;
}
