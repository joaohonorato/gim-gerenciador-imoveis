import { useCallback, useState } from 'react';
import { ActivityIndicator, Linking, RefreshControl, Text, View } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { apiFetch, getErrorMessage } from '@/api/client';
import { Button } from '@/design/Button';
import { Card } from '@/design/Card';
import { ConfirmDialog } from '@/design/ConfirmDialog';
import { StatusBadge } from '@/design/StatusBadge';
import { HubHeader } from '@/design/HubHeader';
import { HubScrollView } from '@/design/HubScrollView';
import { useConfirm } from '@/hooks/useConfirm';
import { ArquivoInfo, CandidaturaPendente } from '@/api/types';

export default function CandidaturasScreen() {
  const [candidaturas, setCandidaturas] = useState<CandidaturaPendente[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [processandoId, setProcessandoId] = useState<string | null>(null);
  const confirmRecusar = useConfirm();

  const carregar = useCallback(async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true); else setLoading(true);
    setError('');
    try {
      const data = await apiFetch<CandidaturaPendente[]>('/candidaturas');
      setCandidaturas(data);
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível carregar as candidaturas'));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      void carregar();
    }, [carregar]),
  );

  async function aprovar(id: string) {
    setProcessandoId(id);
    setError('');
    setFeedback('');
    try {
      await apiFetch(`/candidaturas/${id}/aprovar`, { method: 'POST' });
      setCandidaturas((atual) => atual.filter((c) => c.id !== id));
      setFeedback('Candidatura aprovada. O contrato já está disponível para assinatura de ambas as partes.');
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível aprovar a candidatura'));
    } finally {
      setProcessandoId(null);
    }
  }

  async function abrirArquivo(arquivo: ArquivoInfo) {
    try {
      const { url } = await apiFetch<{ url: string }>(`/arquivos/${arquivo.id}/url`);
      Linking.openURL(url);
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível abrir o documento'));
    }
  }

  async function recusar(id: string) {
    setProcessandoId(id);
    setError('');
    setFeedback('');
    try {
      await apiFetch(`/candidaturas/${id}/recusar`, { method: 'POST' });
      setCandidaturas((atual) => atual.filter((c) => c.id !== id));
      setFeedback('Candidatura recusada.');
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível recusar a candidatura'));
    } finally {
      setProcessandoId(null);
    }
  }

  function confirmarRecusar(item: CandidaturaPendente) {
    confirmRecusar.confirm({
      title: 'Recusar candidatura',
      message: `A candidatura de ${item.inquilinoNome ?? 'este inquilino'} será recusada e o convite não poderá mais ser usado por ele. Continuar?`,
      confirmLabel: 'Recusar',
      onConfirm: () => recusar(item.id),
    });
  }

  if (loading) {
    return (
      <View className="flex-1 items-center justify-center bg-surface gap-3">
        <ActivityIndicator color="#2563EB" />
        <Text className="text-muted">Carregando candidaturas...</Text>
      </View>
    );
  }

  return (
    <View className="flex-1 bg-surface">
      <HubHeader title="Candidaturas" subtitle="Analise e responda às candidaturas dos seus convites." />
      <HubScrollView
        contentContainerClassName="p-6 gap-4"
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void carregar(true)} />}
      >
      {error ? (
        <Card><Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text></Card>
      ) : null}
      {feedback ? (
        <Card><Text style={{ color: '#16A34A', fontSize: 13, fontWeight: '600' }}>{feedback}</Text></Card>
      ) : null}

      {candidaturas.length === 0 ? (
        <Card>
          <Text className="text-muted">Nenhuma candidatura aguardando análise no momento.</Text>
        </Card>
      ) : (
        candidaturas.map((item) => {
          const exigeGarantia = item.garantiaAceita != null && item.garantiaAceita !== 'NENHUMA';
          const garantiaPendente = exigeGarantia && item.garantiaEscolhida == null;
          const processando = processandoId === item.id;
          return (
            <Card key={item.id} className="gap-3">
              <View className="flex-row items-center justify-between gap-2">
                <Text className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>
                  {item.inquilinoNome ?? 'Inquilino'}
                </Text>
                <StatusBadge status="PENDENTE" />
              </View>
              <Text className="text-muted">{item.inquilinoEmail ?? 'e-mail não informado'}</Text>
              <Text className="text-muted">Tipo: {formatTipo(item.tipoContrato)}</Text>
              <Text className="text-muted">Período: {formatDate(item.dataInicio)} até {formatDate(item.dataFim)}</Text>
              <Text className="text-muted">Aluguel: {formatCurrency(item.valorAluguel)}</Text>
              <Text className="text-muted">
                Garantia: {item.garantiaAceita == null ? 'Nenhuma' : formatTipo(item.garantiaAceita)}
                {exigeGarantia ? ` — ${item.garantiaEscolhida ? `enviada (${formatTipo(item.garantiaEscolhida)})` : 'aguardando envio pelo inquilino'}` : ''}
              </Text>

              {exigeGarantia ? (
                <View className="gap-2">
                  <Text className="text-muted" style={{ fontSize: 13, fontWeight: '600' }}>
                    Documentos comprobatórios ({item.documentosGarantia?.length ?? 0})
                  </Text>
                  {!item.documentosGarantia?.length ? (
                    <Text className="text-muted" style={{ fontSize: 13 }}>Nenhum documento enviado ainda.</Text>
                  ) : (
                    item.documentosGarantia.map((arquivo) => (
                      <View
                        key={arquivo.id}
                        className="flex-row items-center justify-between rounded-lg px-4 py-3"
                        style={{ borderWidth: 1, borderColor: '#E5E7EB', backgroundColor: '#F5F6F8' }}
                      >
                        <Text className="text-primary" style={{ fontSize: 13, fontWeight: '600', flexShrink: 1 }} numberOfLines={1}>
                          {arquivo.nomeOriginal}
                        </Text>
                        <Text
                          onPress={() => abrirArquivo(arquivo)}
                          accessibilityRole="link"
                          accessibilityLabel={`Abrir documento ${arquivo.nomeOriginal}`}
                          style={{ color: '#2563EB', fontSize: 13, fontWeight: '700' }}
                        >
                          Abrir
                        </Text>
                      </View>
                    ))
                  )}
                </View>
              ) : null}

              <Button
                label="Ver inquilino"
                variant="outline"
                onPress={() => router.push(`/inquilinos/${item.inquilinoId}`)}
              />

              {garantiaPendente ? (
                <View className="rounded-xl px-4 py-3" style={{ borderWidth: 1, borderColor: '#E5E7EB', backgroundColor: '#F5F6F8' }}>
                  <Text className="text-muted" style={{ fontSize: 13 }}>
                    Este convite exige garantia. Aguarde o inquilino enviar os dados antes de aprovar.
                  </Text>
                </View>
              ) : (
                <View className="flex-row gap-2">
                  <Button label="Aprovar" onPress={() => aprovar(item.id)} loading={processando} disabled={processando} />
                  <Button label="Recusar" variant="outline" onPress={() => confirmarRecusar(item)} loading={processando} disabled={processando} />
                </View>
              )}
            </Card>
          );
        })
      )}
      </HubScrollView>
      <ConfirmDialog
        visible={confirmRecusar.visible}
        title={confirmRecusar.title}
        message={confirmRecusar.message}
        confirmLabel={confirmRecusar.confirmLabel}
        loading={processandoId !== null}
        onConfirm={confirmRecusar.accept}
        onCancel={confirmRecusar.cancel}
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
