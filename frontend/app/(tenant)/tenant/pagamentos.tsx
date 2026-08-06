import { useCallback, useMemo, useState } from 'react';
import { ActivityIndicator, ScrollView, Text, View } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiFetch, getErrorMessage } from '@/api/client';
import { Button } from '@/design/Button';
import { Card } from '@/design/Card';
import { StatusBadge } from '@/design/StatusBadge';
import { Contrato, Pagamento } from '@/api/types';
import { isFuturo } from '@/utils/pagamentos';

interface PagamentoComContrato extends Pagamento {
  contrato: Contrato;
}

export default function TenantPagamentosScreen() {
  const insets = useSafeAreaInsets();
  const [pagamentos, setPagamentos] = useState<PagamentoComContrato[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const carregar = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const contratos = await apiFetch<Contrato[]>('/contratos');
      const assinados = contratos.filter((c) => c.statusAssinatura === 'ASSINADO');
      const porContrato = await Promise.all(assinados.map(async (contrato) => {
        const lista = await apiFetch<Pagamento[]>(`/contratos/${contrato.id}/pagamentos`);
        return lista.map((p) => ({ ...p, contrato }));
      }));
      const todos = porContrato.flat().sort((a, b) => a.vencimento.localeCompare(b.vencimento));
      setPagamentos(todos);
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível carregar seus pagamentos'));
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { void carregar(); }, [carregar]));

  const temMaisDeUmContrato = useMemo(
    () => new Set(pagamentos.map((p) => p.contratoId)).size > 1,
    [pagamentos],
  );

  if (loading) {
    return (
      <View className="flex-1 items-center justify-center bg-surface gap-3">
        <ActivityIndicator color="#2563EB" />
        <Text className="text-muted">Carregando pagamentos...</Text>
      </View>
    );
  }

  return (
    <ScrollView className="flex-1 bg-surface" contentContainerClassName="p-6 gap-4" contentContainerStyle={{ paddingTop: insets.top + 24 }}>
      <View className="flex-row items-center gap-4">
        <Button label="← Voltar" variant="outline" onPress={() => router.back()} />
        <Text className="text-primary" style={{ fontSize: 22, fontWeight: '800' }}>Meus pagamentos</Text>
      </View>

      {error ? <Card><Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text></Card> : null}

      <Card className="gap-3">
        <Text className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>
          Aluguéis ({pagamentos.length})
        </Text>
        {pagamentos.length === 0 ? (
          <Text className="text-muted">Nenhum pagamento encontrado nos seus contratos assinados.</Text>
        ) : (
          pagamentos.map((p) => {
            const agendado = p.status === 'PENDENTE' && isFuturo(p.vencimento);
            return (
              <View
                key={p.id}
                className="gap-1 rounded-xl px-4 py-3"
                style={{ borderWidth: 1, borderColor: '#E5E7EB' }}
              >
                <View className="flex-row items-center justify-between gap-2">
                  <Text className="text-primary" style={{ fontWeight: '700' }}>Venc. {formatDate(p.vencimento)}</Text>
                  {agendado
                    ? <Text style={{ color: '#6B7280', fontSize: 12, fontWeight: '700' }}>Agendado</Text>
                    : <StatusBadge status={p.status} />}
                </View>
                {temMaisDeUmContrato ? (
                  <Text className="text-muted" style={{ fontSize: 13 }}>
                    {p.contrato.enderecoImovel ?? `Contrato ${p.contratoId}`}
                  </Text>
                ) : null}
                <Text className="text-primary" style={{ fontSize: 14, fontWeight: '600' }}>{formatCurrency(p.valor)}</Text>
              </View>
            );
          })
        )}
      </Card>
    </ScrollView>
  );
}

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function formatDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString('pt-BR');
}
