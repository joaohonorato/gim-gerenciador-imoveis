import { useCallback, useState } from 'react';
import { hapticDestructiveConfirm } from '@/utils/haptics';

interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  onConfirm: () => void;
}

// Pareado com <ConfirmDialog />: centraliza o estado "pendente de
// confirmação" pra telas com ações destrutivas de um toque (revogar
// convite, recusar candidatura, remover foto, etc. — ver useConfirm nas
// respectivas telas). `accept()` é o único ponto de saída de toda ação
// destrutiva confirmada no app, por isso é onde o feedback tátil (Onda 4
// item 10) é disparado — cobre as 4 ações destrutivas de uma vez, sem
// precisar repetir a chamada em cada tela.
export function useConfirm() {
  const [pending, setPending] = useState<ConfirmOptions | null>(null);

  const confirm = useCallback((options: ConfirmOptions) => {
    setPending(options);
  }, []);

  const cancel = useCallback(() => setPending(null), []);

  const accept = useCallback(() => {
    hapticDestructiveConfirm();
    setPending((current) => {
      current?.onConfirm();
      return null;
    });
  }, []);

  return {
    visible: pending !== null,
    title: pending?.title ?? '',
    message: pending?.message ?? '',
    confirmLabel: pending?.confirmLabel,
    confirm,
    cancel,
    accept,
  };
}
