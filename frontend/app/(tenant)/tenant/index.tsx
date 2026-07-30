import { useCallback, useMemo, useState } from 'react';
import { RefreshControl, ScrollView, Text, TextInput, View } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { apiFetch } from '@/api/client';
import { session } from '@/api/session';
import { Button } from '@/design/Button';
import { Card } from '@/design/Card';
import { StatusBadge } from '@/design/StatusBadge';
import { Contrato, ConviteInquilino, MeResponse } from '@/api/types';

type ViewMode = 'contrato' | 'proprietario' | 'imovel';

export default function TenantHomeScreen() {
  const [me, setMe] = useState<MeResponse | null>(null);
  const [convites, setConvites] = useState<ConviteInquilino[]>([]);
  const [contratos, setContratos] = useState<Contrato[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [tokenInput, setTokenInput] = useState('');
  const [viewMode, setViewMode] = useState<ViewMode>('contrato');
  const [loggingOut, setLoggingOut] = useState(false);

  const carregar = useCallback(async (isRefresh = false) => {
    if (isRefresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }

    setError('');
    try {
      const [meData, convitesData, contratosData] = await Promise.all([
        apiFetch<MeResponse>('/auth/me'),
        apiFetch<ConviteInquilino[]>('/convites/me'),
        apiFetch<Contrato[]>('/contratos'),
      ]);

      setMe(meData);
      setConvites(convitesData);
      setContratos(contratosData);
    } catch (e: any) {
      setError(e.message ?? 'Não foi possível carregar sua área de inquilino');
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

  const convitesPendentes = convites.filter((item) => item.candidaturaStatus === 'PENDENTE');
  const convitesAprovadosSemAssinatura = convites.filter(
    (item) => item.candidaturaStatus === 'APROVADA' && item.statusAssinaturaContrato === 'PENDENTE' && item.contratoId,
  );

  const contratosAgrupados = useMemo(() => {
    if (viewMode === 'contrato') {
      return [{ titulo: 'Contratos', itens: contratos }];
    }

    const map = new Map<string, Contrato[]>();
    for (const contrato of contratos) {
      const chave = viewMode === 'proprietario'
        ? `Proprietário ${contrato.proprietarioId}`
        : `Unidade ${contrato.unidadeId}`;
      const lista = map.get(chave) ?? [];
      lista.push(contrato);
      map.set(chave, lista);
    }

    return [...map.entries()].map(([titulo, itens]) => ({ titulo, itens }));
  }, [contratos, viewMode]);

  async function logout() {
    setLoggingOut(true);
    try {
      await apiFetch('/auth/logout', { method: 'POST' });
    } catch {
      // Cleanup local sempre deve ocorrer mesmo se backend falhar.
    } finally {
      await session.clear();
      setLoggingOut(false);
      router.replace('/login');
    }
  }

  if (loading) {
    return (
      <View className="flex-1 items-center justify-center bg-surface">
        <Text className="text-muted">Carregando área do inquilino...</Text>
      </View>
    );
  }

  return (
    <ScrollView
      className="flex-1 bg-surface"
      contentContainerClassName="p-6 gap-4"
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void carregar(true)} />}
    >
      <View className="flex-row items-start justify-between gap-3">
        <View className="flex-1">
          <Text className="text-primary" style={{ fontSize: 24, fontWeight: '800' }}>
            Olá{me?.nome ? `, ${me.nome}` : ''}
          </Text>
          <Text className="text-muted" style={{ fontSize: 14 }}>
            Área do inquilino: convites, contratos e assinatura.
          </Text>
        </View>
        <Button label="Sair" variant="outline" onPress={logout} loading={loggingOut} />
      </View>

      {error ? (
        <Card>
          <Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text>
        </Card>
      ) : null}

      <Card>
        <Text className="text-primary mb-3" style={{ fontSize: 16, fontWeight: '700' }}>
          Abrir convite por token
        </Text>
        <View className="gap-3">
          <TextInput
            placeholder="Cole o token de convite"
            value={tokenInput}
            onChangeText={setTokenInput}
            autoCapitalize="none"
            className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
          />
          <Button
            label="Abrir convite"
            onPress={() => router.push(`/locacao/${encodeURIComponent(tokenInput.trim())}`)}
            disabled={!tokenInput.trim()}
          />
        </View>
      </Card>

      <Card>
        <Text className="text-primary mb-3" style={{ fontSize: 16, fontWeight: '700' }}>
          Convites em andamento ({convitesPendentes.length})
        </Text>
        {convites.length === 0 ? (
          <Text className="text-muted">Você ainda não possui convites vinculados à sua conta.</Text>
        ) : (
          <View className="gap-3">
            {convites.map((item) => (
              <View key={item.candidaturaId} className="border-2 border-border rounded px-3 py-3 bg-card gap-2">
                <View className="flex-row items-center justify-between gap-2">
                  <Text className="text-primary" style={{ fontWeight: '700' }}>
                    {formatTipo(item.tipoContrato)}
                  </Text>
                  <StatusBadge status={item.candidaturaStatus === 'APROVADA' ? 'ASSINADO' : 'PENDENTE'} />
                </View>
                <Text className="text-muted">Imóvel: {item.imovelId}</Text>
                <Text className="text-muted">Proprietário: {item.proprietarioId}</Text>
                <Text className="text-muted">Período: {formatDate(item.dataInicio)} até {formatDate(item.dataFim)}</Text>
                <Text className="text-muted">Aluguel: {formatCurrency(item.valorAluguel)}</Text>
                {item.candidaturaStatus === 'PENDENTE' ? (
                  <Button label="Abrir e enviar garantia" onPress={() => router.push(`/locacao/${item.token}`)} />
                ) : null}
                {item.contratoId && item.statusAssinaturaContrato === 'PENDENTE' ? (
                  <Button label="Revisar e assinar contrato" onPress={() => router.push(`/${item.contratoId}/revisar`)} />
                ) : null}
              </View>
            ))}
          </View>
        )}
      </Card>

      <Card>
        <Text className="text-primary mb-3" style={{ fontSize: 16, fontWeight: '700' }}>
          Contratos
        </Text>
        <View className="flex-row gap-2 mb-3">
          <Button label="Por contrato" variant={viewMode === 'contrato' ? 'primary' : 'outline'} onPress={() => setViewMode('contrato')} />
          <Button label="Por proprietário" variant={viewMode === 'proprietario' ? 'primary' : 'outline'} onPress={() => setViewMode('proprietario')} />
          <Button label="Por imóvel" variant={viewMode === 'imovel' ? 'primary' : 'outline'} onPress={() => setViewMode('imovel')} />
        </View>

        {contratos.length === 0 ? (
          <Text className="text-muted">Ainda não há contratos vinculados.</Text>
        ) : (
          <View className="gap-4">
            {contratosAgrupados.map((grupo) => (
              <View key={grupo.titulo} className="gap-2">
                <Text className="text-primary" style={{ fontSize: 14, fontWeight: '700' }}>{grupo.titulo}</Text>
                {grupo.itens.map((contrato) => (
                  <View key={contrato.id} className="border-2 border-border rounded px-3 py-3 bg-card gap-2">
                    <View className="flex-row items-center justify-between">
                      <Text className="text-primary" style={{ fontWeight: '700' }}>Contrato {contrato.id}</Text>
                      <StatusBadge status={contrato.statusAssinatura} />
                    </View>
                    <Text className="text-muted">Tipo: {formatTipo(contrato.tipo ?? contrato.tipoContrato)}</Text>
                    <Text className="text-muted">Período: {formatDate(contrato.dataInicio)} até {formatDate(contrato.dataFim)}</Text>
                    <Text className="text-muted">Aluguel: {formatCurrency(contrato.valorAluguel)}</Text>
                    <Button label="Abrir contrato" onPress={() => router.push(`/${contrato.id}/revisar`)} />
                  </View>
                ))}
              </View>
            ))}
          </View>
        )}
      </Card>

      {convitesAprovadosSemAssinatura.length > 0 ? (
        <Card>
          <Text className="text-primary mb-3" style={{ fontSize: 16, fontWeight: '700' }}>
            Pendentes de assinatura ({convitesAprovadosSemAssinatura.length})
          </Text>
          <View className="gap-2">
            {convitesAprovadosSemAssinatura.map((item) => (
              <Button
                key={item.candidaturaId}
                label={`Assinar contrato ${item.contratoId}`}
                onPress={() => router.push(`/${item.contratoId}/revisar`)}
              />
            ))}
          </View>
        </Card>
      ) : null}
    </ScrollView>
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
