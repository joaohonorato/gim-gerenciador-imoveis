import { useCallback, useMemo, useState } from 'react';
import { ActivityIndicator, ScrollView, Text, View } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { apiFetch } from '@/api/client';
import { Button } from '@/design/Button';
import { Card } from '@/design/Card';
import { StatusBadge } from '@/design/StatusBadge';
import { HubHeader } from '@/design/HubHeader';
import { Contrato, Imovel } from '@/api/types';

export default function ContratosScreen() {
  const [contratos, setContratos] = useState<Contrato[]>([]);
  const [imoveis, setImoveis] = useState<Imovel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const carregar = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [contratosData, imoveisData] = await Promise.all([
        apiFetch<Contrato[]>('/contratos'),
        apiFetch<Imovel[]>('/imoveis'),
      ]);
      setContratos(contratosData);
      setImoveis(imoveisData);
    } catch (e: any) {
      setError(e.message ?? 'Não foi possível carregar os contratos');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { void carregar(); }, [carregar]));

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
    return [...contratos].sort((a, b) => {
      if (a.statusAssinatura === b.statusAssinatura) return 0;
      return a.statusAssinatura === 'PENDENTE' ? -1 : 1;
    });
  }, [contratos]);

  if (loading) {
    return (
      <View className="flex-1 items-center justify-center bg-surface gap-3">
        <ActivityIndicator color="#2563EB" />
        <Text className="text-muted">Carregando contratos...</Text>
      </View>
    );
  }

  return (
    <View className="flex-1 bg-surface">
      <HubHeader title="Contratos" subtitle="Todos os contratos da sua carteira." />
      <ScrollView contentContainerClassName="p-6 gap-3">
        {error ? <Card><Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text></Card> : null}

        {ordenados.length === 0 ? (
          <Card><Text className="text-muted">Nenhum contrato ainda.</Text></Card>
        ) : (
          ordenados.map((contrato) => {
            const imovel = imovelPorUnidade.get(contrato.unidadeId);
            return (
              <Card key={contrato.id} className="gap-2">
                <View className="flex-row items-center justify-between gap-2">
                  <Text className="text-primary" style={{ fontWeight: '700' }}>
                    {formatTipo(contrato.tipo ?? contrato.tipoContrato)}
                  </Text>
                  <StatusBadge status={contrato.statusAssinatura} />
                </View>
                {imovel ? <Text className="text-muted" style={{ fontSize: 13 }}>{imovel.endereco} — {imovel.cidade}</Text> : null}
                <Text className="text-muted" style={{ fontSize: 13 }}>
                  {formatDate(contrato.dataInicio)} até {formatDate(contrato.dataFim)} — {formatCurrency(contrato.valorAluguel)}
                </Text>
                <View className="flex-row gap-2">
                  <Button
                    label={contrato.statusAssinatura === 'PENDENTE' ? 'Revisar e assinar' : 'Abrir contrato'}
                    onPress={() => router.push(`/${contrato.id}/revisar`)}
                  />
                  <Button label="Ver inquilino" variant="outline" onPress={() => router.push(`/inquilinos/${contrato.inquilinoId}`)} />
                </View>
              </Card>
            );
          })
        )}
      </ScrollView>
    </View>
  );
}

function formatTipo(value?: string) {
  if (!value) return 'Não informado';
  const normalized = value.replace(/_/g, ' ').toLowerCase();
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function formatDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString('pt-BR');
}
