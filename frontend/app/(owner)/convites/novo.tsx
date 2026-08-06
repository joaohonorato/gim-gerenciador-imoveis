import { useCallback, useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, FlatList, ScrollView, Text, TextInput, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiFetch, getErrorMessage } from '@/api/client';
import { Imovel } from '@/api/types';
import { Button } from '@/design/Button';
import { Card } from '@/design/Card';
import { StatusBadge } from '@/design/StatusBadge';
import { Pill } from '@/design/Pill';

type UnidadeStatus = 'VAGO' | 'RESERVADO' | 'ALUGADO' | 'MANUTENCAO';
type BadgeStatus = 'VAGO' | 'RESERVADO' | 'ALUGADO';
type TipoImovel = 'CASA' | 'APARTAMENTO' | 'COMERCIAL' | 'OUTRO';

type Filtros = {
  busca: string;
  cidade: string;
  status: UnidadeStatus | '';
  tipoImovel: TipoImovel | '';
};

export default function NovoConviteScreen() {
  const insets = useSafeAreaInsets();
  const [imoveis, setImoveis] = useState<Imovel[]>([]);
  const [todosImoveis, setTodosImoveis] = useState<Imovel[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [filtros, setFiltros] = useState<Filtros>({
    busca: '',
    cidade: '',
    status: 'VAGO',
    tipoImovel: '',
  });

  useEffect(() => {
    apiFetch<Imovel[]>('/imoveis').then(setTodosImoveis).catch(() => {});
  }, []);

  const cidades = useMemo(
    () => [...new Set(todosImoveis.map((imovel) => imovel.cidade).filter(Boolean))].sort(),
    [todosImoveis],
  );

  const carregarImoveis = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = new URLSearchParams();
      if (filtros.busca.trim()) params.set('busca', filtros.busca.trim());
      if (filtros.cidade.trim()) params.set('cidade', filtros.cidade.trim());
      if (filtros.status) params.set('status', filtros.status);
      if (filtros.tipoImovel) params.set('tipoImovel', filtros.tipoImovel);

      const query = params.toString();
      const data = await apiFetch<Imovel[]>(`/imoveis${query ? `?${query}` : ''}`);
      setImoveis(data);
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível carregar os imóveis'));
    } finally {
      setLoading(false);
    }
  }, [filtros]);

  const totalVagos = useMemo(() => imoveis.filter((i) => getStatus(i) === 'VAGO').length, [imoveis]);

  return (
    <View className="flex-1 bg-surface" style={{ paddingTop: insets.top }}>
      <ScrollView className="max-h-96" contentContainerClassName="p-6 pb-3">
        <View className="flex-row items-center justify-between mb-3 gap-3">
          <Text className="text-primary" style={{ fontSize: 24, fontWeight: '800' }}>Novo convite de locação</Text>
          <Button label="Voltar" variant="outline" onPress={() => router.back()} />
        </View>
        <Text className="text-muted mb-4">Filtre seus imóveis e selecione um para continuar o convite.</Text>

        <Card className="gap-3">
          <TextInput
            placeholder="Buscar por endereço, matrícula, bairro ou número"
            placeholderTextColor="#9CA3AF"
            value={filtros.busca}
            onChangeText={(busca) => setFiltros((atual) => ({ ...atual, busca }))}
            className="bg-card px-4 py-3 text-primary rounded-xl"
            style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
          />

          <View className="gap-2">
            <Text className="text-muted">Cidade</Text>
            <View className="flex-row gap-2" style={{ flexWrap: 'wrap' }}>
              <Pill label="Todas" selected={filtros.cidade === ''} onPress={() => setFiltros((atual) => ({ ...atual, cidade: '' }))} />
              {cidades.map((cidade) => (
                <Pill
                  key={cidade}
                  label={cidade}
                  selected={filtros.cidade === cidade}
                  onPress={() => setFiltros((atual) => ({ ...atual, cidade }))}
                />
              ))}
            </View>
          </View>

          <View className="gap-2">
            <Text className="text-muted">Status</Text>
            <View className="flex-row gap-2" style={{ flexWrap: 'wrap' }}>
              {(['', 'VAGO', 'RESERVADO', 'ALUGADO'] as const).map((status) => (
                <Pill
                  key={status || 'TODOS'}
                  label={status || 'Todos'}
                  selected={filtros.status === status}
                  onPress={() => setFiltros((atual) => ({ ...atual, status }))}
                />
              ))}
            </View>
          </View>

          <View className="gap-2">
            <Text className="text-muted">Tipo do imóvel</Text>
            <View className="flex-row gap-2" style={{ flexWrap: 'wrap' }}>
              {(['', 'CASA', 'APARTAMENTO', 'COMERCIAL', 'OUTRO'] as const).map((tipoImovel) => (
                <Pill
                  key={tipoImovel || 'TODOS'}
                  label={tipoImovel || 'Todos'}
                  selected={filtros.tipoImovel === tipoImovel}
                  onPress={() => setFiltros((atual) => ({ ...atual, tipoImovel }))}
                />
              ))}
            </View>
          </View>

          <View className="flex-row gap-2">
            <Button label="Buscar imóveis" variant="dark" onPress={carregarImoveis} loading={loading} />
            <Button
              label="Limpar"
              variant="outline"
              onPress={() => setFiltros({ busca: '', cidade: '', status: '', tipoImovel: '' })}
            />
          </View>

          <Text className="text-muted" style={{ fontSize: 12.5 }}>Resultado: {imoveis.length} imóveis ({totalVagos} vagos)</Text>
          {error ? <Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text> : null}
        </Card>
      </ScrollView>

      {loading ? (
        <View className="flex-1 items-center justify-center">
          <ActivityIndicator color="#2563EB" />
        </View>
      ) : (
        <FlatList
          data={imoveis}
          keyExtractor={(item) => item.id}
          contentContainerClassName="px-6 pb-6 gap-3"
          ListEmptyComponent={<Text className="text-center text-muted mt-8">Nenhum imóvel encontrado para os filtros atuais.</Text>}
          renderItem={({ item }) => (
            <Card className="gap-3">
              <View className="flex-row items-start justify-between gap-3">
                <View className="flex-1">
                  <Text className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>
                    {item.enderecoCompleto ?? montarEndereco(item)}
                  </Text>
                  <Text className="text-muted mt-1" style={{ fontSize: 14 }}>
                    Matrícula: {item.matricula}
                  </Text>
                  <Text className="text-muted mt-1" style={{ fontSize: 14 }}>
                    Tipo: {item.tipoImovel ?? 'Não informado'}
                  </Text>
                </View>
                <StatusBadge status={getStatus(item)} />
              </View>

              <Button label="Selecionar e continuar" onPress={() => router.push(`/imoveis/${item.id}/convite`)} />
            </Card>
          )}
        />
      )}
    </View>
  );
}

function getStatus(imovel: Imovel): BadgeStatus {
  const unidadePadrao = imovel.unidades?.find((unidade) => unidade.padrao) ?? imovel.unidades?.[0];
  if (unidadePadrao?.status === 'ALUGADO') return 'ALUGADO';
  if (unidadePadrao?.status === 'RESERVADO' || unidadePadrao?.status === 'MANUTENCAO') return 'RESERVADO';
  return 'VAGO';
}

function montarEndereco(imovel: Imovel) {
  const parts = [imovel.endereco];
  if (imovel.numero) parts.push(imovel.numero);
  if (imovel.complemento) parts.push(imovel.complemento);
  if (imovel.bairro) parts.push(imovel.bairro);
  parts.push(imovel.cidade);
  return parts.join(' - ');
}
