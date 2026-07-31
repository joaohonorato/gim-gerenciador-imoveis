import { useCallback, useMemo, useState } from 'react';
import { ActivityIndicator, ScrollView, Text, View } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { apiFetch } from '@/api/client';
import { Button } from '@/design/Button';
import { Card } from '@/design/Card';
import { HubHeader } from '@/design/HubHeader';
import { Pill } from '@/design/Pill';
import { Convite, Imovel } from '@/api/types';

const CONVITE_STATUS_COLOR: Record<string, string> = {
  PENDENTE: '#D97706',
  EM_ANALISE: '#2563EB',
  CONSUMIDO: '#16A34A',
  EXPIRADO: '#6B7280',
  RECUSADO: '#DC2626',
  REVOGADO: '#6B7280',
};

export default function ConvitesScreen() {
  const [convites, setConvites] = useState<Convite[]>([]);
  const [imoveis, setImoveis] = useState<Imovel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [somenteAtivos, setSomenteAtivos] = useState(true);
  const [revogandoToken, setRevogandoToken] = useState<string | null>(null);

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
      setError(e.message ?? 'Não foi possível carregar os convites');
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
      setError(e.message ?? 'Não foi possível revogar o convite');
    } finally {
      setRevogandoToken(null);
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
      <ScrollView contentContainerClassName="p-6 gap-3">
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
            return (
              <Card key={c.token} className="gap-2">
                <View className="flex-row items-center justify-between gap-2">
                  <Text className="text-primary" style={{ fontWeight: '700' }}>{formatTipo(c.tipoContrato)}</Text>
                  <Text style={{ color: CONVITE_STATUS_COLOR[c.status ?? ''] ?? '#6B7280', fontSize: 12, fontWeight: '700' }}>
                    {formatTipo(c.status ?? '')}
                  </Text>
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
                <View className="flex-row gap-2">
                  <Button label="Ver convite" variant="outline" onPress={() => router.push(`/locacao/${c.token}`)} />
                  {c.status === 'PENDENTE' ? (
                    <Button
                      label="Revogar"
                      variant="outline"
                      onPress={() => revogarConvite(c.token)}
                      loading={revogandoToken === c.token}
                      disabled={revogandoToken === c.token}
                    />
                  ) : null}
                </View>
              </Card>
            );
          })
        )}
      </ScrollView>
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
