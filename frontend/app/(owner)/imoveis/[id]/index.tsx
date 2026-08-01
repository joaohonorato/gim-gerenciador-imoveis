import { useCallback, useState } from 'react';
import { ActivityIndicator, Image, Platform, Pressable, ScrollView, Text, View } from 'react-native';
import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import * as ImagePicker from 'expo-image-picker';
import { apiFetch } from '@/api/client';
import { Button } from '@/design/Button';
import { Card } from '@/design/Card';
import { StatusBadge } from '@/design/StatusBadge';
import { Chamado, Contrato, Convite, Imovel, Pagamento } from '@/api/types';
import { isFuturo } from '@/utils/pagamentos';

const CONVITE_STATUS_COLOR: Record<string, string> = {
  PENDENTE: '#D97706',
  EM_ANALISE: '#2563EB',
  CONSUMIDO: '#16A34A',
  EXPIRADO: '#6B7280',
  RECUSADO: '#DC2626',
  REVOGADO: '#6B7280',
};

const CHAMADO_STATUS_COLOR: Record<string, string> = {
  ABERTO: '#D97706',
  EM_ANDAMENTO: '#2563EB',
  RESOLVIDO: '#16A34A',
};

export default function ImovelDetalheScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();

  const [imovel, setImovel] = useState<Imovel | null>(null);
  const [convites, setConvites] = useState<Convite[]>([]);
  const [contrato, setContrato] = useState<Contrato | null>(null);
  const [pagamentos, setPagamentos] = useState<Pagamento[]>([]);
  const [chamados, setChamados] = useState<Chamado[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [revogandoToken, setRevogandoToken] = useState<string | null>(null);
  const [enviandoFoto, setEnviandoFoto] = useState(false);
  const [removendoFotoId, setRemovendoFotoId] = useState<string | null>(null);

  const carregar = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError('');
    try {
      const [imovelData, convitesData, contratosData, chamadosData] = await Promise.all([
        apiFetch<Imovel>(`/imoveis/${id}`),
        apiFetch<Convite[]>(`/imoveis/${id}/convites`),
        apiFetch<Contrato[]>('/contratos'),
        apiFetch<Chamado[]>(`/imoveis/${id}/chamados`),
      ]);
      setImovel(imovelData);
      setConvites(convitesData);
      setChamados(chamadosData);

      const unidadePadraoId = imovelData.unidades?.find((u) => u.padrao)?.id ?? imovelData.unidades?.[0]?.id;
      const contratoDoImovel = contratosData.find((c) => c.unidadeId === unidadePadraoId) ?? null;
      setContrato(contratoDoImovel);
      setPagamentos(contratoDoImovel ? await apiFetch<Pagamento[]>(`/contratos/${contratoDoImovel.id}/pagamentos`) : []);
    } catch (e: any) {
      setError(e.message ?? 'Não foi possível carregar o imóvel');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useFocusEffect(useCallback(() => { void carregar(); }, [carregar]));

  async function adicionarFoto() {
    if (!id) return;
    setEnviandoFoto(true);
    setError('');
    try {
      const permissao = await ImagePicker.requestMediaLibraryPermissionsAsync();
      if (!permissao.granted && Platform.OS !== 'web') {
        throw new Error('Permissão de acesso às fotos negada.');
      }
      const resultado = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 0.9 });
      const asset = resultado.canceled ? null : resultado.assets?.[0];
      if (!asset) return;

      const formData = new FormData();
      if (Platform.OS === 'web' && asset.file) {
        formData.append('foto', asset.file, asset.fileName ?? 'foto.jpg');
      } else {
        formData.append('foto', {
          uri: asset.uri,
          name: asset.fileName ?? 'foto.jpg',
          type: asset.mimeType ?? 'image/jpeg',
        } as unknown as Blob);
      }

      const atualizado = await apiFetch<Imovel>(`/imoveis/${id}/fotos`, { method: 'POST', body: formData });
      setImovel(atualizado);
    } catch (e: any) {
      setError(e.message ?? 'Não foi possível enviar a foto');
    } finally {
      setEnviandoFoto(false);
    }
  }

  async function removerFoto(fotoId: string) {
    if (!id) return;
    setRemovendoFotoId(fotoId);
    setError('');
    try {
      const atualizado = await apiFetch<Imovel>(`/imoveis/${id}/fotos/${fotoId}`, { method: 'DELETE' });
      setImovel(atualizado);
    } catch (e: any) {
      setError(e.message ?? 'Não foi possível remover a foto');
    } finally {
      setRemovendoFotoId(null);
    }
  }

  async function revogarConvite(token: string) {
    setRevogandoToken(token);
    setError('');
    try {
      await apiFetch(`/convites/${token}/revogar`, { method: 'POST' });
      setConvites((atual) => atual.filter((c) => c.token !== token));
    } catch (e: any) {
      setError(e.message ?? 'Não foi possível revogar o convite');
    } finally {
      setRevogandoToken(null);
    }
  }

  if (loading) {
    return (
      <View className="flex-1 items-center justify-center bg-surface gap-3">
        <ActivityIndicator color="#2563EB" />
        <Text className="text-muted">Carregando imóvel...</Text>
      </View>
    );
  }

  if (!imovel) {
    return (
      <View className="flex-1 items-center justify-center bg-surface gap-3 p-6">
        <Text style={{ color: '#DC2626' }}>{error || 'Imóvel não encontrado.'}</Text>
        <Button label="Voltar" variant="outline" onPress={() => router.back()} />
      </View>
    );
  }

  const status = getStatus(imovel);

  return (
    <ScrollView className="flex-1 bg-surface" contentContainerClassName="p-6 gap-4">
      <View className="flex-row items-center gap-4">
        <Button label="← Voltar" variant="outline" onPress={() => router.back()} />
        <View className="flex-1">
          <Text className="text-primary" style={{ fontSize: 22, fontWeight: '800' }}>{imovel.endereco}</Text>
          <Text className="text-muted" style={{ fontSize: 14 }}>{imovel.cidade}</Text>
        </View>
        <StatusBadge status={status} />
      </View>

      {error ? <Card><Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text></Card> : null}

      <Card className="gap-3">
        <Text className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>
          Fotos ({imovel.fotos?.length ?? 0})
        </Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false}>
          <View className="flex-row gap-3">
            {(imovel.fotos ?? []).map((foto) => (
              <View key={foto.id} style={{ width: 120, height: 120 }}>
                <Image
                  source={{ uri: foto.url }}
                  style={{ width: 120, height: 120, borderRadius: 12, borderWidth: 1, borderColor: '#E5E7EB' }}
                />
                <Pressable
                  onPress={() => removerFoto(foto.id)}
                  disabled={removendoFotoId === foto.id}
                  style={{
                    position: 'absolute', top: 6, right: 6,
                    width: 24, height: 24, borderRadius: 12,
                    backgroundColor: 'rgba(17,24,39,0.75)',
                    alignItems: 'center', justifyContent: 'center',
                  }}
                >
                  <Text style={{ color: '#fff', fontSize: 13, fontWeight: '800' }}>×</Text>
                </Pressable>
              </View>
            ))}
            <Pressable
              onPress={adicionarFoto}
              disabled={enviandoFoto}
              style={{
                width: 120, height: 120, borderRadius: 12,
                borderWidth: 1.5, borderColor: '#E5E7EB', borderStyle: 'dashed',
                alignItems: 'center', justifyContent: 'center',
              }}
            >
              {enviandoFoto
                ? <ActivityIndicator color="#2563EB" />
                : <Text style={{ color: '#6B7280', fontSize: 13, fontWeight: '600', textAlign: 'center' }}>+ Adicionar{'\n'}foto</Text>}
            </Pressable>
          </View>
        </ScrollView>
      </Card>

      <Card className="gap-3">
        <Text className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>Dados do imóvel</Text>
        <DetailRow label="Matrícula" value={imovel.matricula} />
        <DetailRow label="Tipo" value={formatTipo(imovel.tipoImovel ?? undefined)} />
        <DetailRow label="Bairro" value={imovel.bairro ?? 'Não informado'} />
        <DetailRow label="Visibilidade" value={imovel.visibilidade === 'PUBLICADO' ? 'Público' : 'Privado'} />
        <Button
          label="Enviar novo convite"
          variant="outline"
          onPress={() => router.push(`/imoveis/${id}/convite`)}
        />
      </Card>

      <Card className="gap-3">
        <Text className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>
          Convites ({convites.length})
        </Text>
        {convites.length === 0 ? (
          <Text className="text-muted">Nenhum convite enviado para este imóvel ainda.</Text>
        ) : (
          convites.map((c) => (
            <View
              key={c.token}
              className="gap-2 rounded-xl px-4 py-3"
              style={{ borderWidth: 1, borderColor: '#E5E7EB' }}
            >
              <View className="flex-row items-center justify-between gap-2">
                <Text className="text-primary" style={{ fontWeight: '700' }}>{formatTipo(c.tipoContrato)}</Text>
                <Text style={{ color: CONVITE_STATUS_COLOR[c.status ?? ''] ?? '#6B7280', fontSize: 12, fontWeight: '700' }}>
                  {formatTipo(c.status ?? '')}
                </Text>
              </View>
              <Text className="text-muted" style={{ fontSize: 13 }}>
                Período: {formatDate(c.dataInicio)} até {formatDate(c.dataFim)} — {formatCurrency(c.valorAluguel)}
              </Text>
              {c.envio ? (
                <Text className="text-muted" style={{ fontSize: 13 }}>
                  Envio: {formatTipo(c.envio.status)} {c.envio.destino ? `(${c.envio.destino})` : ''}
                </Text>
              ) : null}
              <View className="flex-row gap-2">
                <Button label="Ver convite" variant="outline" onPress={() => router.push(`/locacao/${c.token}`)} />
                {c.status === 'PENDENTE' ? (
                  <Button
                    label="Revogar"
                    variant="outline"
                    onPress={() => revogarConvite(c.token)}
                    loading={revogandoToken === c.token}
                    disabled={revogandoToken === c.token}
                  />
                ) : null}
              </View>
            </View>
          ))
        )}
      </Card>

      <Card className="gap-3">
        <Text className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>Contrato e pagamentos</Text>
        {!contrato ? (
          <Text className="text-muted">Nenhum contrato ativo para este imóvel.</Text>
        ) : (
          <View className="gap-3">
            <View className="flex-row items-center justify-between">
              <Text className="text-primary" style={{ fontWeight: '600' }}>{formatTipo(contrato.tipo ?? contrato.tipoContrato)}</Text>
              <StatusBadge status={contrato.statusAssinatura} />
            </View>
            <DetailRow label="Aluguel" value={formatCurrency(contrato.valorAluguel)} />
            <DetailRow label="Período" value={`${formatDate(contrato.dataInicio)} até ${formatDate(contrato.dataFim)}`} />
            <View className="flex-row gap-2">
              {contrato.statusAssinatura === 'PENDENTE' ? (
                <Button label="Revisar e assinar" onPress={() => router.push(`/${contrato.id}/revisar`)} />
              ) : null}
              <Button label="Ver inquilino" variant="outline" onPress={() => router.push(`/inquilinos/${contrato.inquilinoId}`)} />
            </View>

            {pagamentos.length > 0 ? (
              <View className="gap-2 mt-2">
                <Text className="text-primary" style={{ fontSize: 14, fontWeight: '700' }}>Pagamentos ({pagamentos.length})</Text>
                {pagamentos.map((p) => {
                  const agendado = p.status === 'PENDENTE' && isFuturo(p.vencimento);
                  return (
                    <View key={p.id} className="flex-row items-center justify-between rounded-xl px-4 py-3" style={{ borderWidth: 1, borderColor: '#E5E7EB' }}>
                      <Text className="text-muted" style={{ fontSize: 13 }}>Venc. {formatDate(p.vencimento)}</Text>
                      <Text className="text-primary" style={{ fontSize: 13, fontWeight: '600' }}>{formatCurrency(p.valor)}</Text>
                      {agendado
                        ? <Text style={{ color: '#6B7280', fontSize: 12, fontWeight: '700' }}>Agendado</Text>
                        : <StatusBadge status={p.status} />}
                    </View>
                  );
                })}
              </View>
            ) : null}
          </View>
        )}
      </Card>

      <Card className="gap-3">
        <Text className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>
          Chamados ({chamados.length})
        </Text>
        {chamados.length === 0 ? (
          <Text className="text-muted">Nenhum chamado aberto para este imóvel.</Text>
        ) : (
          chamados.map((c) => (
            <View key={c.id} className="gap-1 rounded-xl px-4 py-3" style={{ borderWidth: 1, borderColor: '#E5E7EB' }}>
              <View className="flex-row items-center justify-between gap-2">
                <Text className="text-primary" style={{ fontWeight: '700' }}>{formatTipo(c.categoria)}</Text>
                <Text style={{ color: CHAMADO_STATUS_COLOR[c.status] ?? '#6B7280', fontSize: 12, fontWeight: '700' }}>
                  {formatTipo(c.status)}
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

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <View className="flex-row items-center justify-between gap-4">
      <Text className="text-muted" style={{ fontSize: 14 }}>{label}</Text>
      <Text className="text-primary text-right" style={{ fontSize: 14, fontWeight: '600', flexShrink: 1 }}>{value}</Text>
    </View>
  );
}

function getStatus(imovel: Imovel): 'VAGO' | 'RESERVADO' | 'ALUGADO' {
  const unidadePadrao = imovel.unidades?.find((unidade) => unidade.padrao) ?? imovel.unidades?.[0];
  const status = unidadePadrao?.status;
  if (status === 'ALUGADO' || status === 'RESERVADO') return status;
  return 'VAGO';
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

