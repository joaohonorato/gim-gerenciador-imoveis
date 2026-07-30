import { useEffect, useState } from 'react';
import { View, Text, ScrollView } from 'react-native';
import { useLocalSearchParams, router } from 'expo-router';
import { apiFetch } from '@/api/client';
import { Contrato, MeResponse } from '@/api/types';
import { Card } from '@/design/Card';
import { Button } from '@/design/Button';
import { StatusBadge } from '@/design/StatusBadge';

export default function RevisarContratoScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const [contrato, setContrato] = useState<Contrato | null>(null);
  const [me, setMe] = useState<MeResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([
      apiFetch<Contrato>(`/contratos/${id}`),
      apiFetch<MeResponse>('/auth/me'),
    ])
      .then(([contratoData, meData]) => {
        setContrato(contratoData);
        setMe(meData);
      })
      .catch(() => {});
  }, [id]);

  async function assinar() {
    if (!me) return;

    const parte = me.tipoConta === 'INQUILINO' ? 'INQUILINO' : 'PROPRIETARIO';
    setLoading(true);
    setError('');
    try {
      const updated = await apiFetch<Contrato>(`/contratos/${id}/assinar`, {
        method: 'POST',
        body: JSON.stringify({ parte }),
      });
      setContrato(updated);
    } catch (e: any) {
      setError(e.message ?? 'Erro ao assinar');
    } finally {
      setLoading(false);
    }
  }

  if (!contrato) {
    return <View className="flex-1 bg-surface justify-center items-center"><Text className="text-muted">Carregando...</Text></View>;
  }

  const podeAssinarComoProprietario = me?.tipoConta === 'PROPRIETARIO' && !contrato.assinouProprietario;
  const podeAssinarComoInquilino = me?.tipoConta === 'INQUILINO' && !contrato.assinouInquilino;
  const exibirBotaoAssinar = podeAssinarComoProprietario || podeAssinarComoInquilino;
  const assinaturaLabel = me?.tipoConta === 'INQUILINO' ? 'Assinar como inquilino' : 'Assinar como proprietário';

  return (
    <ScrollView className="flex-1 bg-surface" contentContainerClassName="p-6">
      <View className="flex-row items-center gap-4 mb-6">
        <Button label="← Voltar" onPress={() => router.back()} variant="outline" />
        <Text className="text-primary" style={{ fontSize: 24, fontWeight: '800' }}>Revisar contrato</Text>
      </View>

      <View className="gap-4">
        <Card>
          <Text className="text-primary mb-4" style={{ fontSize: 15, fontWeight: '700' }}>Detalhes</Text>
          <View className="gap-3">
            <DetailRow label="Tipo" value={formatTipo(contrato.tipo ?? contrato.tipoContrato)} />
            <DetailRow label="Aluguel" value={formatCurrency(contrato.valorAluguel)} />
            <DetailRow label="Período" value={`${formatDate(contrato.dataInicio)} até ${formatDate(contrato.dataFim)}`} />
            <View className="flex-row items-center justify-between">
              <Text className="text-muted" style={{ fontSize: 14 }}>Status</Text>
              <StatusBadge status={contrato.statusAssinatura as 'PENDENTE' | 'ASSINADO'} />
            </View>
          </View>
        </Card>

        <Card>
          <Text className="text-primary mb-4" style={{ fontSize: 15, fontWeight: '700' }}>Assinaturas</Text>
          <View className="gap-4">
            <SignatureRow label="Proprietário" signed={contrato.assinouProprietario} />
            <SignatureRow label="Inquilino" signed={contrato.assinouInquilino} />
          </View>
          {exibirBotaoAssinar ? (
            <View className="gap-3 mt-5">
              {error ? <Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text> : null}
              <Button testID="btn-assinar" label={assinaturaLabel} onPress={assinar} loading={loading} />
            </View>
          ) : null}
          {contrato.statusAssinatura === 'ASSINADO' ? (
            <View className="mt-5 rounded-lg px-4 py-3" style={{ backgroundColor: '#F0FDF4' }}>
              <Text className="text-center" style={{ color: '#16A34A', fontSize: 13, fontWeight: '600' }}>
                Contrato totalmente assinado!
              </Text>
            </View>
          ) : null}
        </Card>
      </View>
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

function SignatureRow({ label, signed }: { label: string; signed: boolean }) {
  const color = signed ? '#16A34A' : '#D97706';
  const text = signed ? 'Assinado' : 'Pendente';

  return (
    <View className="flex-row items-center justify-between">
      <Text style={{ color: '#374151', fontSize: 14 }}>{label}</Text>
      <Text style={{ color, fontSize: 13, fontWeight: '600' }}>{text}</Text>
    </View>
  );
}

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function formatDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString('pt-BR');
}

function formatTipo(value?: string) {
  if (!value) return 'Não informado';
  const normalized = value.replace(/_/g, ' ').toLowerCase();
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}
