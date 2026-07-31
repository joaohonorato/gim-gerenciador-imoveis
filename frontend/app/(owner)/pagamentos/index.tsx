import { useCallback, useMemo, useState } from 'react';
import { ActivityIndicator, ScrollView, Text, View } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { apiFetch } from '@/api/client';
import { Card } from '@/design/Card';
import { StatusBadge } from '@/design/StatusBadge';
import { HubHeader } from '@/design/HubHeader';
import { Pill } from '@/design/Pill';
import { Contrato, Imovel, Pagamento } from '@/api/types';
import { isFuturo } from '@/utils/pagamentos';

export default function PagamentosScreen() {
  const [pagamentos, setPagamentos] = useState<Pagamento[]>([]);
  const [contratos, setContratos] = useState<Contrato[]>([]);
  const [imoveis, setImoveis] = useState<Imovel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [somenteAcao, setSomenteAcao] = useState(true);

  const carregar = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [pagamentosData, contratosData, imoveisData] = await Promise.all([
        apiFetch<Pagamento[]>('/pagamentos'),
        apiFetch<Contrato[]>('/contratos'),
        apiFetch<Imovel[]>('/imoveis'),
      ]);
      setPagamentos(pagamentosData);
      setContratos(contratosData);
      setImoveis(imoveisData);
    } catch (e: any) {
      setError(e.message ?? 'Não foi possível carregar os pagamentos');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { void carregar(); }, [carregar]));

  const contratoPorId = useMemo(() => {
    const map = new Map<string, Contrato>();
    for (const c of contratos) map.set(c.id, c);
    return map;
  }, [contratos]);

  const imovelPorUnidade = useMemo(() => {
    const map = new Map<string, Imovel>();
    for (const imovel of imoveis) {
      for (const unidade of imovel.unidades ?? []) {
        map.set(unidade.id, imovel);
      }
    }
    return map;
  }, [imoveis]);

  const ordenados = useMemo(() => {
    return [...pagamentos].sort((a, b) => a.vencimento.localeCompare(b.vencimento));
  }, [pagamentos]);

  const exibidos = useMemo(() => {
    if (!somenteAcao) return ordenados;
    return ordenados.filter((p) => p.status === 'ATRASADO' || (p.status === 'PENDENTE' && !isFuturo(p.vencimento)));
  }, [ordenados, somenteAcao]);

  if (loading) {
    return (
      <View className="flex-1 items-center justify-center bg-surface gap-3">
        <ActivityIndicator color="#2563EB" />
        <Text className="text-muted">Carregando pagamentos...</Text>
      </View>
    );
  }

  return (
    <View className="flex-1 bg-surface">
      <HubHeader title="Pagamentos" subtitle="Aluguéis de todos os seus contratos." />
      <ScrollView contentContainerClassName="p-6 gap-3">
        {error ? <Card><Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text></Card> : null}

        <View className="flex-row gap-2">
          <Pill label="Precisam de atenção" selected={somenteAcao} onPress={() => setSomenteAcao(true)} />
          <Pill label="Todos" selected={!somenteAcao} onPress={() => setSomenteAcao(false)} />
        </View>

        {exibidos.length === 0 ? (
          <Card><Text className="text-muted">Nenhum pagamento {somenteAcao ? 'pendente de atenção' : 'encontrado'}.</Text></Card>
        ) : (
          exibidos.map((p) => {
            const contrato = contratoPorId.get(p.contratoId);
            const imovel = contrato ? imovelPorUnidade.get(contrato.unidadeId) : undefined;
            const agendado = p.status === 'PENDENTE' && isFuturo(p.vencimento);
            return (
              <Card key={p.id} className="gap-2">
                <View className="flex-row items-center justify-between gap-2">
                  <Text className="text-primary" style={{ fontWeight: '700' }}>Venc. {formatDate(p.vencimento)}</Text>
                  {agendado
                    ? <Text style={{ color: '#6B7280', fontSize: 12, fontWeight: '700' }}>Agendado</Text>
                    : <StatusBadge status={p.status} />}
                </View>
                {imovel ? <Text className="text-muted" style={{ fontSize: 13 }}>{imovel.endereco} — {imovel.cidade}</Text> : null}
                <Text className="text-primary" style={{ fontSize: 14, fontWeight: '600' }}>{formatCurrency(p.valor)}</Text>
                {contrato ? (
                  <Text
                    className="text-muted"
                    style={{ fontSize: 12, textDecorationLine: 'underline' }}
                    onPress={() => router.push(`/${contrato.id}/revisar`)}
                  >
                    Ver contrato
                  </Text>
                ) : null}
              </Card>
            );
          })
        )}
      </ScrollView>
    </View>
  );
}

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function formatDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString('pt-BR');
}
