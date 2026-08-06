import { useCallback, useState } from 'react';
import { ActivityIndicator, ScrollView, Text, View } from 'react-native';
import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiFetch, getErrorMessage } from '@/api/client';
import { Button } from '@/design/Button';
import { Card } from '@/design/Card';
import { StatusBadge } from '@/design/StatusBadge';
import { Contrato, Inquilino } from '@/api/types';

export default function InquilinoDetalheScreen() {
  const insets = useSafeAreaInsets();
  const { id } = useLocalSearchParams<{ id: string }>();

  const [inquilino, setInquilino] = useState<Inquilino | null>(null);
  const [contratos, setContratos] = useState<Contrato[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const carregar = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError('');
    try {
      const [inquilinoData, contratosData] = await Promise.all([
        apiFetch<Inquilino>(`/inquilinos/${id}`),
        apiFetch<Contrato[]>('/contratos'),
      ]);
      setInquilino(inquilinoData);
      setContratos(contratosData.filter((c) => c.inquilinoId === id));
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível carregar os dados do inquilino'));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useFocusEffect(useCallback(() => { void carregar(); }, [carregar]));

  if (loading) {
    return (
      <View className="flex-1 items-center justify-center bg-surface gap-3">
        <ActivityIndicator color="#2563EB" />
        <Text className="text-muted">Carregando inquilino...</Text>
      </View>
    );
  }

  if (!inquilino) {
    return (
      <View className="flex-1 items-center justify-center bg-surface gap-3 p-6">
        <Text style={{ color: '#DC2626' }}>{error || 'Inquilino não encontrado.'}</Text>
        <Button label="Voltar" variant="outline" onPress={() => router.back()} />
      </View>
    );
  }

  return (
    <ScrollView className="flex-1 bg-surface" contentContainerClassName="p-6 gap-4" contentContainerStyle={{ paddingTop: insets.top + 24 }}>
      <View className="flex-row items-center gap-4">
        <Button label="← Voltar" variant="outline" onPress={() => router.back()} />
        <Text className="text-primary" style={{ fontSize: 22, fontWeight: '800' }}>{inquilino.nome}</Text>
      </View>

      {error ? <Card><Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text></Card> : null}

      <Card className="gap-3">
        <Text className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>Dados do inquilino</Text>
        <DetailRow label="Nome" value={inquilino.nome} />
        <DetailRow label="CPF" value={formatCpf(inquilino.cpf)} />
        <DetailRow label="E-mail" value={inquilino.email ?? 'Não informado'} />
        <DetailRow label="Cadastrado em" value={formatDate(inquilino.criadoEm)} />
      </Card>

      <Card className="gap-3">
        <Text className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>
          Contratos ({contratos.length})
        </Text>
        {contratos.length === 0 ? (
          <Text className="text-muted">Nenhum contrato com este inquilino.</Text>
        ) : (
          contratos.map((c) => (
            <View key={c.id} className="gap-2 rounded-xl px-4 py-3" style={{ borderWidth: 1, borderColor: '#E5E7EB' }}>
              <View className="flex-row items-center justify-between gap-2">
                <Text className="text-primary" style={{ fontWeight: '700' }}>{formatTipo(c.tipo ?? c.tipoContrato)}</Text>
                <StatusBadge status={c.statusAssinatura} />
              </View>
              <Text className="text-muted" style={{ fontSize: 13 }}>
                {formatDate(c.dataInicio)} até {formatDate(c.dataFim)} — {formatCurrency(c.valorAluguel)}
              </Text>
              <Button label="Abrir contrato" variant="outline" onPress={() => router.push(`/${c.id}/revisar`)} />
            </View>
          ))
        )}
      </Card>
    </ScrollView>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <View className="flex-row items-center justify-between gap-4">
      <Text className="text-muted" style={{ fontSize: 14 }}>{label}</Text>
      <Text className="text-primary text-right" style={{ fontSize: 14, fontWeight: '600', flexShrink: 1 }}>{value}</Text>
    </View>
  );
}

function formatCpf(cpf: string | null) {
  if (!cpf) return 'Não informado';
  return cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
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
  return new Date(value.length <= 10 ? `${value}T00:00:00` : value).toLocaleDateString('pt-BR');
}
