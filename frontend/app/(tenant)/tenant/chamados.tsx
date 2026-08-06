import { useCallback, useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, ScrollView, Text, TextInput, View } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiFetch, getErrorMessage } from '@/api/client';
import { Button } from '@/design/Button';
import { Card } from '@/design/Card';
import { Pill } from '@/design/Pill';
import { CategoriaChamado, Chamado, Contrato, NovoChamadoRequest } from '@/api/types';

const CHAMADO_STATUS_COLOR: Record<string, string> = {
  ABERTO: '#D97706',
  EM_ANDAMENTO: '#2563EB',
  RESOLVIDO: '#16A34A',
};

export default function TenantChamadosScreen() {
  const insets = useSafeAreaInsets();
  const [chamados, setChamados] = useState<Chamado[]>([]);
  const [contratos, setContratos] = useState<Contrato[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [imovelId, setImovelId] = useState<string | null>(null);
  const [categoriasChamado, setCategoriasChamado] = useState<CategoriaChamado[]>([]);
  const [categoriaId, setCategoriaId] = useState<string | null>(null);
  const [descricao, setDescricao] = useState('');
  const [enviando, setEnviando] = useState(false);

  const carregar = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [chamadosData, contratosData] = await Promise.all([
        apiFetch<Chamado[]>('/chamados'),
        apiFetch<Contrato[]>('/contratos'),
      ]);
      setChamados(chamadosData);
      setContratos(contratosData);
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível carregar seus chamados'));
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { void carregar(); }, [carregar]));

  const imoveisComContratoAtivo = useMemo(() => {
    const vistos = new Set<string>();
    const lista: { imovelId: string; label: string }[] = [];
    for (const c of contratos) {
      if (c.statusAssinatura !== 'ASSINADO' || !c.imovelId || vistos.has(c.imovelId)) continue;
      vistos.add(c.imovelId);
      lista.push({ imovelId: c.imovelId, label: c.enderecoImovel ?? c.imovelId });
    }
    return lista;
  }, [contratos]);

  const imovelAtivo = imovelId ?? imoveisComContratoAtivo[0]?.imovelId ?? null;

  // Catálogo de categorias é por proprietário — resolvido via o imóvel
  // selecionado, já que o inquilino não tem um "catálogo próprio" (é o do
  // proprietário do imóvel que ele está abrindo o chamado).
  useEffect(() => {
    if (!imovelAtivo) {
      setCategoriasChamado([]);
      setCategoriaId(null);
      return;
    }
    let cancelado = false;
    apiFetch<CategoriaChamado[]>(`/imoveis/${imovelAtivo}/categorias-chamado`)
      .then((data) => {
        if (cancelado) return;
        setCategoriasChamado(data);
        setCategoriaId((atual) => (atual && data.some((c) => c.id === atual) ? atual : data[0]?.id ?? null));
      })
      .catch((e: any) => {
        if (!cancelado) setError(getErrorMessage(e, 'Não foi possível carregar as categorias de chamado'));
      });
    return () => { cancelado = true; };
  }, [imovelAtivo]);

  async function enviarChamado() {
    if (!imovelAtivo || !categoriaId || !descricao.trim()) {
      setError('Escolha o imóvel, a categoria e descreva o problema antes de enviar.');
      return;
    }
    setEnviando(true);
    setError('');
    try {
      const body: NovoChamadoRequest = { categoriaId, descricao: descricao.trim() };
      const novo = await apiFetch<Chamado>(`/imoveis/${imovelAtivo}/chamados`, {
        method: 'POST',
        body: JSON.stringify(body),
      });
      setChamados((atual) => [novo, ...atual]);
      setDescricao('');
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível abrir o chamado'));
    } finally {
      setEnviando(false);
    }
  }

  if (loading) {
    return (
      <View className="flex-1 items-center justify-center bg-surface gap-3">
        <ActivityIndicator color="#2563EB" />
        <Text className="text-muted">Carregando chamados...</Text>
      </View>
    );
  }

  return (
    <ScrollView className="flex-1 bg-surface" contentContainerClassName="p-6 gap-4" contentContainerStyle={{ paddingTop: insets.top + 24 }}>
      <View className="flex-row items-center gap-4">
        <Button label="← Voltar" variant="outline" onPress={() => router.back()} />
        <Text className="text-primary" style={{ fontSize: 22, fontWeight: '800' }}>Chamados</Text>
      </View>

      {error ? <Card><Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text></Card> : null}

      <Card className="gap-3">
        <Text className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>Abrir chamado</Text>

        {imoveisComContratoAtivo.length === 0 ? (
          <Text className="text-muted">Sem contrato ativo pra abrir chamado.</Text>
        ) : (
          <>
            {imoveisComContratoAtivo.length > 1 ? (
              <>
                <Text className="text-muted" style={{ fontSize: 12, fontWeight: '600' }}>Imóvel</Text>
                <View className="flex-row gap-2" style={{ flexWrap: 'wrap' }}>
                  {imoveisComContratoAtivo.map((item) => (
                    <Pill
                      key={item.imovelId}
                      label={item.label}
                      selected={imovelAtivo === item.imovelId}
                      onPress={() => setImovelId(item.imovelId)}
                    />
                  ))}
                </View>
              </>
            ) : null}

            <Text className="text-muted" style={{ fontSize: 12, fontWeight: '600' }}>Categoria</Text>
            {categoriasChamado.length === 0 ? (
              <Text className="text-muted" style={{ fontSize: 13 }}>Nenhuma categoria disponível pra esse imóvel.</Text>
            ) : (
              <View className="flex-row gap-2" style={{ flexWrap: 'wrap' }}>
                {categoriasChamado.map((cat) => (
                  <Pill key={cat.id} label={cat.nome} selected={categoriaId === cat.id} onPress={() => setCategoriaId(cat.id)} />
                ))}
              </View>
            )}

            <TextInput
              placeholder="Descreva o problema"
              placeholderTextColor="#9CA3AF"
              value={descricao}
              onChangeText={setDescricao}
              multiline
              numberOfLines={4}
              className="bg-card px-4 py-3 text-primary rounded-xl"
              style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14, minHeight: 90, textAlignVertical: 'top' }}
            />
            <Button label="Enviar chamado" onPress={enviarChamado} loading={enviando} />
          </>
        )}
      </Card>

      <Card className="gap-3">
        <Text className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>
          Meus chamados ({chamados.length})
        </Text>
        {chamados.length === 0 ? (
          <Text className="text-muted">Nenhum chamado aberto ainda.</Text>
        ) : (
          chamados.map((c) => (
            <View key={c.id} className="gap-1 rounded-xl px-4 py-3" style={{ borderWidth: 1, borderColor: '#E5E7EB' }}>
              <View className="flex-row items-center justify-between gap-2">
                <Text className="text-primary" style={{ fontWeight: '700' }}>{c.categoriaNome}</Text>
                <Text style={{ color: CHAMADO_STATUS_COLOR[c.status] ?? '#6B7280', fontSize: 12, fontWeight: '700' }}>
                  {formatStatus(c.status)}
                </Text>
              </View>
              <Text className="text-muted" style={{ fontSize: 13 }}>{c.descricao}</Text>
              <Text className="text-muted" style={{ fontSize: 12 }}>Aberto em {formatDate(c.abertoEm)}</Text>
            </View>
          ))
        )}
      </Card>
    </ScrollView>
  );
}

function formatStatus(status: Chamado['status']) {
  if (status === 'EM_ANDAMENTO') return 'Em andamento';
  if (status === 'RESOLVIDO') return 'Resolvido';
  return 'Aberto';
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString('pt-BR');
}
