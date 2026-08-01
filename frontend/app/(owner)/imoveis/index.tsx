import { useCallback, useMemo, useState } from 'react';
import { View, Text, FlatList } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { apiFetch, ApiException } from '@/api/client';
import { Contrato, Imovel, Inquilino } from '@/api/types';
import { session } from '@/api/session';
import { Card } from '@/design/Card';
import { Button } from '@/design/Button';
import { StatusBadge } from '@/design/StatusBadge';
import { HubHeader } from '@/design/HubHeader';

export default function ImoveisScreen() {
  const [imoveis, setImoveis] = useState<Imovel[]>([]);
  const [contratos, setContratos] = useState<Contrato[]>([]);
  const [inquilinos, setInquilinos] = useState<Record<string, Inquilino>>({});
  const [loading, setLoading] = useState(true);

  async function load() {
    try {
      const [imoveisData, contratosData] = await Promise.all([
        apiFetch<Imovel[]>('/imoveis'),
        apiFetch<Contrato[]>('/contratos'),
      ]);
      setImoveis(imoveisData);
      setContratos(contratosData);

      const inquilinoIds = [...new Set(
        contratosData.filter((c) => c.statusAssinatura === 'ASSINADO').map((c) => c.inquilinoId),
      )];
      const pares = await Promise.all(inquilinoIds.map(async (id) => {
        try {
          return [id, await apiFetch<Inquilino>(`/inquilinos/${id}`)] as const;
        } catch {
          return null;
        }
      }));
      setInquilinos(Object.fromEntries(pares.filter((p): p is readonly [string, Inquilino] => p != null)));
    } catch (e) {
      if (e instanceof ApiException && e.status === 401) {
        await session.clear();
        router.replace('/login');
      }
    }
    finally { setLoading(false); }
  }

  useFocusEffect(useCallback(() => { void load(); }, []));

  const contratosPendentes = useMemo(
    () => contratos.filter((c) => c.statusAssinatura === 'PENDENTE' && !c.assinouProprietario),
    [contratos],
  );

  const contratoAtivoPorUnidade = useMemo(() => {
    const map = new Map<string, Contrato>();
    for (const c of contratos) {
      if (c.statusAssinatura === 'ASSINADO') map.set(c.unidadeId, c);
    }
    return map;
  }, [contratos]);

  const stats = imoveis.reduce((acc, imovel) => {
    const status = getStatus(imovel);
    acc.total += 1;
    if (status === 'ALUGADO') acc.alugados += 1;
    if (status === 'VAGO') acc.vagos += 1;
    return acc;
  }, { total: 0, alugados: 0, vagos: 0 });

  return (
    <View className="flex-1 bg-surface">
      <HubHeader title="Meus imóveis" subtitle="Acompanhe status e dados de cada imóvel cadastrado." />
      <View className="px-6 pb-4">
        <Button testID="btn-novo-imovel" label="+ Novo imóvel" onPress={() => router.push('/imoveis/novo')} />
      </View>

      <View className="px-6 pb-4 flex-row gap-3" style={{ flexWrap: 'wrap' }}>
        <StatCard label="Cadastrados" value={stats.total} color="#111827" />
        <StatCard label="Alugados" value={stats.alugados} color="#2563EB" />
        <StatCard label="Vagos" value={stats.vagos} color="#16A34A" />
      </View>

      {contratosPendentes.length > 0 ? (
        <View className="px-6 pb-4">
          <Card className="gap-3">
            <Text className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>
              Contratos pendentes da sua assinatura ({contratosPendentes.length})
            </Text>
            {contratosPendentes.map((contrato) => (
              <View
                key={contrato.id}
                className="flex-row items-center justify-between gap-3 rounded-xl px-4 py-3"
                style={{ borderWidth: 1, borderColor: '#E5E7EB' }}
              >
                <View className="flex-1">
                  <Text className="text-primary" style={{ fontWeight: '600', fontSize: 14 }}>
                    {formatTipo(contrato.tipo ?? contrato.tipoContrato)}
                  </Text>
                  <Text className="text-muted" style={{ fontSize: 13 }}>{formatCurrency(contrato.valorAluguel)}</Text>
                </View>
                <Button label="Assinar" onPress={() => router.push(`/${contrato.id}/revisar`)} />
              </View>
            ))}
          </Card>
        </View>
      ) : null}

      {loading
        ? <Text className="text-center text-muted mt-8">Carregando...</Text>
        : (
          <FlatList
            data={imoveis}
            keyExtractor={i => i.id}
            contentContainerClassName="px-6 pb-6 gap-3"
            ListEmptyComponent={<Text className="text-center text-muted mt-8">Nenhum imóvel cadastrado.</Text>}
            renderItem={({ item }) => {
              const status = getStatus(item);
              const unidadePadrao = item.unidades?.find((u) => u.padrao) ?? item.unidades?.[0];
              const contratoAtivo = unidadePadrao ? contratoAtivoPorUnidade.get(unidadePadrao.id) : undefined;
              const inquilino = contratoAtivo ? inquilinos[contratoAtivo.inquilinoId] : undefined;

              return (
                <Card className="gap-3">
                  <View className="flex-row items-start justify-between gap-3">
                    <View className="flex-1">
                      <Text testID={`imovel-${item.id}`} className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>{item.endereco}</Text>
                      <Text className="text-muted mt-1" style={{ fontSize: 14 }}>{item.cidade}</Text>
                    </View>
                    <StatusBadge status={status} />
                  </View>

                  <View className="flex-row justify-between gap-3">
                    <View className="flex-1">
                      <Text className="text-muted" style={{ fontSize: 13 }}>Matrícula</Text>
                      <Text className="text-primary mt-1" style={{ fontSize: 14, fontWeight: '600' }}>{item.matricula}</Text>
                    </View>
                    <View className="flex-1">
                      <Text className="text-muted" style={{ fontSize: 13 }}>Visibilidade</Text>
                      <Text className="text-primary mt-1" style={{ fontSize: 14, fontWeight: '600' }}>{formatVisibilidade(item.visibilidade)}</Text>
                    </View>
                  </View>

                  {status === 'ALUGADO' && contratoAtivo ? (
                    <View className="rounded-xl px-4 py-3" style={{ borderWidth: 1, borderColor: '#E5E7EB', backgroundColor: '#F5F6F8' }}>
                      <Text className="text-muted" style={{ fontSize: 13 }}>Inquilino</Text>
                      <Text className="text-primary mt-1" style={{ fontSize: 14, fontWeight: '600' }}>
                        {inquilino?.nome ?? 'Carregando...'}
                      </Text>
                      <View className="mt-2">
                        <Button
                          label="Ver detalhes do inquilino"
                          variant="outline"
                          onPress={() => router.push(`/inquilinos/${contratoAtivo.inquilinoId}`)}
                        />
                      </View>
                    </View>
                  ) : null}

                  <View className="flex-row gap-2">
                    <Button
                      label="Ver detalhes"
                      onPress={() => router.push(`/imoveis/${item.id}`)}
                    />
                    <Button
                      label="Enviar convite"
                      variant="outline"
                      onPress={() => router.push(`/imoveis/${item.id}/convite`)}
                    />
                  </View>
                </Card>
              );
            }}
          />
        )
      }
    </View>
  );
}

function StatCard({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <View
      className="bg-card rounded-xl p-5"
      style={{ borderWidth: 1, borderColor: '#E5E7EB', flexGrow: 1, flexBasis: 110, minWidth: 110 }}
    >
      <Text style={{ color, fontSize: 28, fontWeight: '800' }}>{value}</Text>
      <Text className="text-muted mt-1" style={{ fontSize: 13 }}>{label}</Text>
    </View>
  );
}

function getStatus(imovel: Imovel): 'VAGO' | 'RESERVADO' | 'ALUGADO' {
  const unidadePadrao = imovel.unidades?.find((unidade) => unidade.padrao) ?? imovel.unidades?.[0];
  const status = unidadePadrao?.status;
  if (status === 'ALUGADO' || status === 'RESERVADO') return status;
  return 'VAGO';
}

function formatVisibilidade(visibilidade: Imovel['visibilidade']) {
  return visibilidade === 'PUBLICADO' ? 'Público' : 'Privado';
}

function formatTipo(value?: string) {
  if (!value) return 'Não informado';
  const normalized = value.replace(/_/g, ' ').toLowerCase();
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}
