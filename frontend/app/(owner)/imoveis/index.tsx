import { useEffect, useState } from 'react';
import { View, Text, FlatList } from 'react-native';
import { router } from 'expo-router';
import { apiFetch, ApiException } from '@/api/client';
import { Imovel } from '@/api/types';
import { session } from '@/api/session';
import { Card } from '@/design/Card';
import { Button } from '@/design/Button';
import { StatusBadge } from '@/design/StatusBadge';

export default function ImoveisScreen() {
  const [imoveis, setImoveis] = useState<Imovel[]>([]);
  const [loading, setLoading] = useState(true);
  const [loggingOut, setLoggingOut] = useState(false);

  async function load() {
    try {
      const data = await apiFetch<Imovel[]>('/imoveis');
      setImoveis(data);
    } catch (e) {
      if (e instanceof ApiException && e.status === 401) {
        await session.clear();
        router.replace('/login');
      }
    }
    finally { setLoading(false); }
  }

  async function logout() {
    if (loggingOut) return;

    setLoggingOut(true);
    try {
      await apiFetch('/auth/logout', { method: 'POST' });
    } catch {
      // Mesmo com erro de rede/401, garantir logout local.
    } finally {
      await session.clear();
      router.replace('/login');
      setLoggingOut(false);
    }
  }

  useEffect(() => { load(); }, []);

  const stats = imoveis.reduce((acc, imovel) => {
    const status = getStatus(imovel);
    acc.total += 1;
    if (status === 'ALUGADO') acc.alugados += 1;
    if (status === 'VAGO') acc.vagos += 1;
    return acc;
  }, { total: 0, alugados: 0, vagos: 0 });

  return (
    <View className="flex-1 bg-surface">
      <View className="px-6 pt-12 pb-4 bg-surface">
        <View className="flex-row items-start justify-between gap-4">
          <View className="flex-1 pr-2">
            <Text className="text-primary mb-1" style={{ fontSize: 24, fontWeight: '800' }}>Meus imóveis</Text>
            <Text className="text-muted" style={{ fontSize: 14 }}>
              Acompanhe status e dados de cada imóvel cadastrado.
            </Text>
          </View>
          <View style={{ minWidth: 142, gap: 8 }}>
            <Button testID="btn-novo-imovel" label="+ Novo imóvel" onPress={() => router.push('/imoveis/novo')} />
            <Button testID="btn-novo-convite" label="+ Novo convite" variant="outline" onPress={() => router.push('/convites/novo')} />
            <Button testID="btn-logout" label="Sair" onPress={logout} variant="outline" loading={loggingOut} disabled={loggingOut} />
          </View>
        </View>
      </View>

      <View className="px-6 pb-4 gap-3">
        <StatCard label="Total cadastrados" value={stats.total} color="#111827" />
        <StatCard label="Alugados" value={stats.alugados} color="#2563EB" />
        <StatCard label="Vagos" value={stats.vagos} color="#16A34A" />
      </View>

      {loading
        ? <Text className="text-center text-muted mt-8">Carregando...</Text>
        : (
          <FlatList
            data={imoveis}
            keyExtractor={i => i.id}
            contentContainerClassName="px-6 pb-6 gap-3"
            ListEmptyComponent={<Text className="text-center text-muted mt-8">Nenhum imóvel cadastrado.</Text>}
            renderItem={({ item }) => (
              <Card className="gap-3">
                <View className="flex-row items-start justify-between gap-3">
                  <View className="flex-1">
                    <Text testID={`imovel-${item.id}`} className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>{item.endereco}</Text>
                    <Text className="text-muted mt-1" style={{ fontSize: 14 }}>{item.cidade}</Text>
                  </View>
                  <StatusBadge status={getStatus(item)} />
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

                <Button
                  label="Enviar convite"
                  variant="outline"
                  onPress={() => router.push(`/imoveis/${item.id}/convite`)}
                />
              </Card>
            )}
          />
        )
      }
    </View>
  );
}

function StatCard({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <View className="bg-card rounded-xl p-5" style={{ borderWidth: 1, borderColor: '#E5E7EB' }}>
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
  return visibilidade === 'PUBLICO' ? 'Público' : 'Privado';
}
