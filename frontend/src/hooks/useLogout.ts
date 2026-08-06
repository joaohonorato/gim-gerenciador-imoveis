import { useCallback, useState } from 'react';
import { router } from 'expo-router';
import { apiFetch } from '@/api/client';
import { session } from '@/api/session';

// Ponto único de logout — usado no header do proprietário (todos os hubs,
// via HubHeader) e na home do inquilino, pra garantir que os dois pontos de
// saída de sessão se comportem exatamente igual (rank 8:
// docs/jornadas-e-backlog-tecnico.md, "consistência em todos os pontos de
// saída de sessão"). Antes desta extração, cada lado tinha sua própria
// cópia quase idêntica, com pequenas divergências de ordem/detalhe.
export function useLogout() {
  const [loggingOut, setLoggingOut] = useState(false);

  const logout = useCallback(async () => {
    if (loggingOut) return;
    setLoggingOut(true);
    try {
      await apiFetch('/auth/logout', { method: 'POST' });
    } catch {
      // Sessão local é limpa de qualquer forma — trade-off intencional
      // (ver CLAUDE.md): melhor destravar o usuário localmente do que
      // deixá-lo preso numa tela por causa de uma falha de rede no logout.
    } finally {
      await session.clear();
      // `saiu=1` faz a tela de login mostrar uma confirmação explícita —
      // sem isso, logout era um redirect silencioso sem nenhum feedback.
      router.replace({ pathname: '/login', params: { saiu: '1' } });
    }
  }, [loggingOut]);

  return { logout, loggingOut };
}
