import { useCallback, useMemo, useState } from 'react';
import { ActivityIndicator, Text, View } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { apiFetch, getErrorMessage } from '@/api/client';
import { Button } from '@/design/Button';
import { Card } from '@/design/Card';
import { ConfirmDialog } from '@/design/ConfirmDialog';
import { HubHeader } from '@/design/HubHeader';
import { HubScrollView } from '@/design/HubScrollView';
import { Pill } from '@/design/Pill';
import { useConfirm } from '@/hooks/useConfirm';
import { Convite, EventoAuditoriaConvite, Imovel } from '@/api/types';

const CONVITE_STATUS_COLOR: Record<string, string> = {
  PENDENTE: '#D97706',
  EM_ANALISE: '#2563EB',
  CONSUMIDO: '#16A34A',
  EXPIRADO: '#6B7280',
  RECUSADO: '#DC2626',
  REVOGADO: '#6B7280',
};

const EVENTO_LABEL: Record<string, string> = {
  CRIADO: 'Convite criado',
  ENVIADO: 'Envio',
  RENOVADO: 'Prazo renovado',
  CANDIDATURA_CRIADA: 'Candidatura recebida',
  CONSUMIDO: 'Contrato assinado — convite concluído',
  ACEITO: 'Aceito',
  REVOGADO: 'Revogado',
  RECUSADO: 'Recusado pelo inquilino',
  TENTATIVA_COM_TOKEN_EXPIRADO: 'Tentativa de acesso com o link já expirado',
  TENTATIVA_COM_TOKEN_INVALIDO_OU_CONSUMIDO: 'Tentativa de acesso com link inválido/já usado',
};

export default function ConvitesScreen() {
  const [convites, setConvites] = useState<Convite[]>([]);
  const [imoveis, setImoveis] = useState<Imovel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [somenteAtivos, setSomenteAtivos] = useState(true);
  const [revogandoToken, setRevogandoToken] = useState<string | null>(null);
  const [reenviandoToken, setReenviandoToken] = useState<string | null>(null);
  const [historicoAberto, setHistoricoAberto] = useState<string | null>(null);
  const [eventosPorToken, setEventosPorToken] = useState<Record<string, EventoAuditoriaConvite[]>>({});
  const [carregandoHistorico, setCarregandoHistorico] = useState(false);
  const confirmRevogar = useConfirm();

  const carregar = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [convitesData, imoveisData] = await Promise.all([
        apiFetch<Convite[]>('/convites'),
        apiFetch<Imovel[]>('/imoveis'),
      ]);
      setConvites(convitesData);
      setImoveis(imoveisData);
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível carregar os convites'));
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { void carregar(); }, [carregar]));

  const imovelPorId = useMemo(() => {
    const map = new Map<string, Imovel>();
    for (const imovel of imoveis) map.set(imovel.id, imovel);
    return map;
  }, [imoveis]);

  const exibidos = useMemo(() => {
    if (!somenteAtivos) return convites;
    return convites.filter((c) => c.status === 'PENDENTE' || c.status === 'EM_ANALISE');
  }, [convites, somenteAtivos]);

  async function revogarConvite(token: string) {
    setRevogandoToken(token);
    setError('');
    try {
      await apiFetch(`/convites/${token}/revogar`, { method: 'POST' });
      setConvites((atual) => atual.map((c) => (c.token === token ? { ...c, status: 'REVOGADO' } : c)));
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível revogar o convite'));
    } finally {
      setRevogandoToken(null);
    }
  }

  function confirmarRevogarConvite(token: string) {
    confirmRevogar.confirm({
      title: 'Revogar convite',
      message: 'O convite será revogado e o link parará de funcionar. O inquilino não conseguirá mais aceitá-lo. Continuar?',
      confirmLabel: 'Revogar',
      onConfirm: () => revogarConvite(token),
    });
  }

  // Reenviar reaproveita o canal e o destino do último envio (o backend cai
  // de volta pro ultimoDestinoEnvio quando o corpo não repete o e-mail/
  // telefone) — e, se o convite já tiver expirado, o próprio reenvio renova
  // o prazo em 7 dias sem trocar o link. É a recuperação de acesso do rank 5
  // acessível direto do hub, sem precisar recriar o convite do zero.
  async function reenviarConvite(c: Convite) {
    setReenviandoToken(c.token);
    setError('');
    try {
      const atualizado = await apiFetch<Convite>(`/convites/${c.token}/reenviar`, {
        method: 'POST',
        body: JSON.stringify({ canalEnvio: c.envio?.canal ?? 'EMAIL' }),
      });
      setConvites((atual) => atual.map((item) => (item.token === c.token ? atualizado : item)));
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível reenviar o convite'));
    } finally {
      setReenviandoToken(null);
    }
  }

  async function alternarHistorico(token: string) {
    if (historicoAberto === token) {
      setHistoricoAberto(null);
      return;
    }
    setHistoricoAberto(token);
    if (eventosPorToken[token]) return;
    setCarregandoHistorico(true);
    try {
      const eventos = await apiFetch<EventoAuditoriaConvite[]>(`/convites/${token}/eventos`);
      setEventosPorToken((atual) => ({ ...atual, [token]: eventos }));
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível carregar o histórico do convite'));
    } finally {
      setCarregandoHistorico(false);
    }
  }

  if (loading) {
    return (
      <View className="flex-1 items-center justify-center bg-surface gap-3">
        <ActivityIndicator color="#2563EB" />
        <Text className="text-muted">Carregando convites...</Text>
      </View>
    );
  }

  return (
    <View className="flex-1 bg-surface">
      <HubHeader title="Convites" subtitle="Convites de locação enviados." />
      <HubScrollView contentContainerClassName="p-6 gap-3">
        {error ? <Card><Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text></Card> : null}

        <View className="flex-row items-center justify-between gap-2" style={{ flexWrap: 'wrap' }}>
          <View className="flex-row gap-2">
            <Pill label="Ativos" selected={somenteAtivos} onPress={() => setSomenteAtivos(true)} />
            <Pill label="Todos" selected={!somenteAtivos} onPress={() => setSomenteAtivos(false)} />
          </View>
          <Button testID="btn-novo-convite" label="+ Novo convite" onPress={() => router.push('/convites/novo')} />
        </View>

        {exibidos.length === 0 ? (
          <Card><Text className="text-muted">Nenhum convite {somenteAtivos ? 'ativo' : 'encontrado'}.</Text></Card>
        ) : (
          exibidos.map((c) => {
            const imovel = c.imovelId ? imovelPorId.get(c.imovelId) : undefined;
            const expirado = c.status === 'PENDENTE' && c.expirado;
            const statusLabel = expirado ? 'Expirado' : formatTipo(c.status ?? '');
            const statusColor = expirado ? CONVITE_STATUS_COLOR.EXPIRADO : (CONVITE_STATUS_COLOR[c.status ?? ''] ?? '#6B7280');
            const eventos = eventosPorToken[c.token];
            return (
              <Card key={c.token} className="gap-2">
                <View className="flex-row items-center justify-between gap-2">
                  <Text className="text-primary" style={{ fontWeight: '700' }}>{formatTipo(c.tipoContrato)}</Text>
                  <Text style={{ color: statusColor, fontSize: 12, fontWeight: '700' }}>{statusLabel}</Text>
                </View>
                {imovel ? <Text className="text-muted" style={{ fontSize: 13 }}>{imovel.endereco} — {imovel.cidade}</Text> : null}
                <Text className="text-muted" style={{ fontSize: 13 }}>
                  {formatDate(c.dataInicio)} até {formatDate(c.dataFim)} — {formatCurrency(c.valorAluguel)}
                </Text>
                {c.envio ? (
                  <Text className="text-muted" style={{ fontSize: 13 }}>
                    Envio: {formatTipo(c.envio.status)} {c.envio.destino ? `(${c.envio.destino})` : ''}
                  </Text>
                ) : null}
                {expirado ? (
                  <Text style={{ color: '#92400E', fontSize: 12 }}>
                    Link fora do prazo — o inquilino não consegue mais aceitar. Reenviar renova o prazo automaticamente.
                  </Text>
                ) : null}
                <View className="flex-row gap-2" style={{ flexWrap: 'wrap' }}>
                  <Button label="Ver convite" variant="outline" onPress={() => router.push(`/locacao/${c.token}`)} />
                  {c.status === 'PENDENTE' ? (
                    <Button
                      label={expirado ? 'Renovar e reenviar' : 'Reenviar'}
                      variant="outline"
                      onPress={() => reenviarConvite(c)}
                      loading={reenviandoToken === c.token}
                      disabled={reenviandoToken === c.token}
                    />
                  ) : null}
                  {c.status === 'PENDENTE' ? (
                    <Button
                      label="Revogar"
                      variant="outline"
                      onPress={() => confirmarRevogarConvite(c.token)}
                      loading={revogandoToken === c.token}
                      disabled={revogandoToken === c.token}
                    />
                  ) : null}
                  <Button label={historicoAberto === c.token ? 'Ocultar histórico' : 'Ver histórico'} variant="outline" onPress={() => alternarHistorico(c.token)} />
                </View>
                {historicoAberto === c.token ? (
                  <View className="gap-1 mt-1 rounded-lg px-3 py-3" style={{ borderWidth: 1, borderColor: '#E5E7EB', backgroundColor: '#F5F6F8' }}>
                    {carregandoHistorico && !eventos ? (
                      <Text className="text-muted" style={{ fontSize: 12 }}>Carregando histórico...</Text>
                    ) : eventos && eventos.length > 0 ? (
                      eventos.map((evento, i) => (
                        <Text key={i} className="text-muted" style={{ fontSize: 12 }}>
                          {formatDateTime(evento.criadoEm)} — {EVENTO_LABEL[evento.tipoEvento] ?? evento.tipoEvento}
                          {evento.detalhe ? ` (${evento.detalhe})` : ''}
                        </Text>
                      ))
                    ) : (
                      <Text className="text-muted" style={{ fontSize: 12 }}>Nenhum evento registrado ainda.</Text>
                    )}
                  </View>
                ) : null}
              </Card>
            );
          })
        )}
      </HubScrollView>
      <ConfirmDialog
        visible={confirmRevogar.visible}
        title={confirmRevogar.title}
        message={confirmRevogar.message}
        confirmLabel={confirmRevogar.confirmLabel}
        loading={revogandoToken !== null}
        onConfirm={confirmRevogar.accept}
        onCancel={confirmRevogar.cancel}
      />
    </View>
  );
}

function formatTipo(value: string) {
  const normalized = value.replace(/_/g, ' ').toLowerCase();
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function formatDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString('pt-BR');
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('pt-BR');
}
