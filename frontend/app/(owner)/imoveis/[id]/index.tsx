import { useCallback, useState } from 'react';
import { ActivityIndicator, Image, Platform, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import * as ImagePicker from 'expo-image-picker';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiFetch, getErrorMessage } from '@/api/client';
import { Button } from '@/design/Button';
import { Card } from '@/design/Card';
import { ConfirmDialog } from '@/design/ConfirmDialog';
import { Pill } from '@/design/Pill';
import { StatusBadge } from '@/design/StatusBadge';
import { UnidadesCard } from '@/design/UnidadesCard';
import { useConfirm } from '@/hooks/useConfirm';
import {
  AtualizarChamadoRequest,
  CategoriaChamado,
  Chamado,
  Conta,
  Contrato,
  Convite,
  Imovel,
  NovaContaRequest,
  NovaCategoriaChamadoRequest,
  NovoTipoContaRequest,
  Pagamento,
  TipoContaImovel,
} from '@/api/types';
import { isFuturo, isMesAtual } from '@/utils/pagamentos';

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
  const insets = useSafeAreaInsets();
  const { id } = useLocalSearchParams<{ id: string }>();

  const [imovel, setImovel] = useState<Imovel | null>(null);
  const [convites, setConvites] = useState<Convite[]>([]);
  const [contrato, setContrato] = useState<Contrato | null>(null);
  const [pagamentos, setPagamentos] = useState<Pagamento[]>([]);
  const [chamados, setChamados] = useState<Chamado[]>([]);
  const [contas, setContas] = useState<Conta[]>([]);
  const [tiposConta, setTiposConta] = useState<TipoContaImovel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [revogandoToken, setRevogandoToken] = useState<string | null>(null);
  const [enviandoFoto, setEnviandoFoto] = useState(false);
  const [removendoFotoId, setRemovendoFotoId] = useState<string | null>(null);
  const confirmRemoverFoto = useConfirm();
  const confirmRevogar = useConfirm();

  const [atualizandoChamadoId, setAtualizandoChamadoId] = useState<string | null>(null);
  const [categoriasChamado, setCategoriasChamado] = useState<CategoriaChamado[]>([]);
  const [novaCategoriaNome, setNovaCategoriaNome] = useState('');
  const [criandoCategoria, setCriandoCategoria] = useState(false);

  const [tipoContaId, setTipoContaId] = useState<string | null>(null);
  const [novoTipoContaNome, setNovoTipoContaNome] = useState('');
  const [criandoTipoConta, setCriandoTipoConta] = useState(false);
  const [responsavelConta, setResponsavelConta] = useState<'PROPRIETARIO' | 'INQUILINO'>('PROPRIETARIO');
  const [vencimentoConta, setVencimentoConta] = useState('');
  const [valorConta, setValorConta] = useState('');
  const [salvandoConta, setSalvandoConta] = useState(false);

  const carregar = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError('');
    try {
      const [imovelData, convitesData, contratosData, chamadosData, contasData, tiposContaData, categoriasChamadoData] = await Promise.all([
        apiFetch<Imovel>(`/imoveis/${id}`),
        apiFetch<Convite[]>(`/imoveis/${id}/convites`),
        apiFetch<Contrato[]>('/contratos'),
        apiFetch<Chamado[]>(`/imoveis/${id}/chamados`),
        apiFetch<Conta[]>(`/imoveis/${id}/contas`),
        apiFetch<TipoContaImovel[]>('/tipos-conta'),
        apiFetch<CategoriaChamado[]>('/categorias-chamado'),
      ]);
      setImovel(imovelData);
      setConvites(convitesData);
      setChamados(chamadosData);
      setContas(contasData);
      setTiposConta(tiposContaData);
      setTipoContaId((atual) => atual ?? tiposContaData[0]?.id ?? null);
      setCategoriasChamado(categoriasChamadoData);

      const unidadePadraoId = imovelData.unidades?.find((u) => u.padrao)?.id ?? imovelData.unidades?.[0]?.id;
      const contratoDoImovel = contratosData.find((c) => c.unidadeId === unidadePadraoId) ?? null;
      setContrato(contratoDoImovel);
      setPagamentos(contratoDoImovel ? await apiFetch<Pagamento[]>(`/contratos/${contratoDoImovel.id}/pagamentos`) : []);
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível carregar o imóvel'));
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
      setError(getErrorMessage(e, 'Não foi possível enviar a foto'));
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
      setError(getErrorMessage(e, 'Não foi possível remover a foto'));
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
      setError(getErrorMessage(e, 'Não foi possível revogar o convite'));
    } finally {
      setRevogandoToken(null);
    }
  }

  function confirmarRemoverFoto(fotoId: string) {
    confirmRemoverFoto.confirm({
      title: 'Remover foto',
      message: 'A foto será removida do anúncio do imóvel. Continuar?',
      confirmLabel: 'Remover',
      onConfirm: () => removerFoto(fotoId),
    });
  }

  function confirmarRevogarConvite(token: string) {
    confirmRevogar.confirm({
      title: 'Revogar convite',
      message: 'O convite será revogado e o link parará de funcionar. O inquilino não conseguirá mais aceitá-lo. Continuar?',
      confirmLabel: 'Revogar',
      onConfirm: () => revogarConvite(token),
    });
  }

  async function criarTipoConta() {
    if (!novoTipoContaNome.trim()) return;
    setCriandoTipoConta(true);
    setError('');
    try {
      const body: NovoTipoContaRequest = { nome: novoTipoContaNome.trim() };
      const novo = await apiFetch<TipoContaImovel>('/tipos-conta', {
        method: 'POST',
        body: JSON.stringify(body),
      });
      setTiposConta((atual) => [...atual, novo]);
      setTipoContaId(novo.id);
      setNovoTipoContaNome('');
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível criar o tipo de conta'));
    } finally {
      setCriandoTipoConta(false);
    }
  }

  async function adicionarConta() {
    if (!id) return;
    const valor = Number(valorConta.replace(',', '.'));
    if (!tipoContaId || !vencimentoConta.trim() || !Number.isFinite(valor) || valor <= 0) {
      setError('Escolha o tipo de conta e preencha vencimento (AAAA-MM-DD) e valor antes de adicionar.');
      return;
    }
    setSalvandoConta(true);
    setError('');
    try {
      const body: NovaContaRequest = {
        tipoContaId,
        vencimento: vencimentoConta.trim(),
        valor,
        responsavel: responsavelConta,
        contratoId: responsavelConta === 'INQUILINO' ? contrato?.id : undefined,
      };
      const nova = await apiFetch<Conta>(`/imoveis/${id}/contas`, {
        method: 'POST',
        body: JSON.stringify(body),
      });
      setContas((atual) => [...atual, nova]);
      setVencimentoConta('');
      setValorConta('');
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível adicionar a conta'));
    } finally {
      setSalvandoConta(false);
    }
  }

  async function atualizarChamado(chamadoId: string, status: AtualizarChamadoRequest['status']) {
    setAtualizandoChamadoId(chamadoId);
    setError('');
    try {
      const body: AtualizarChamadoRequest = { status };
      const atualizado = await apiFetch<Chamado>(`/chamados/${chamadoId}`, {
        method: 'PATCH',
        body: JSON.stringify(body),
      });
      setChamados((atual) => atual.map((c) => (c.id === atualizado.id ? atualizado : c)));
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível atualizar o chamado'));
    } finally {
      setAtualizandoChamadoId(null);
    }
  }

  async function criarCategoriaChamado() {
    if (!novaCategoriaNome.trim()) return;
    setCriandoCategoria(true);
    setError('');
    try {
      const body: NovaCategoriaChamadoRequest = { nome: novaCategoriaNome.trim() };
      const nova = await apiFetch<CategoriaChamado>('/categorias-chamado', {
        method: 'POST',
        body: JSON.stringify(body),
      });
      setCategoriasChamado((atual) => [...atual, nova]);
      setNovaCategoriaNome('');
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível criar a categoria de chamado'));
    } finally {
      setCriandoCategoria(false);
    }
  }

  const totalInquilinoMes =
    pagamentos.filter((p) => isMesAtual(p.vencimento)).reduce((soma, p) => soma + p.valor, 0) +
    contas
      .filter((c) => c.responsavel === 'INQUILINO' && c.contratoId === contrato?.id && isMesAtual(c.vencimento))
      .reduce((soma, c) => soma + c.valor, 0);

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
    <ScrollView className="flex-1 bg-surface" contentContainerClassName="p-6 gap-4" contentContainerStyle={{ paddingTop: insets.top + 24 }}>
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
                  onPress={() => confirmarRemoverFoto(foto.id)}
                  disabled={removendoFotoId === foto.id}
                  accessibilityRole="button"
                  accessibilityLabel="Remover foto"
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
        <DetailRow label="CEP" value={imovel.cep ?? 'Não informado'} />
        <DetailRow label="Quartos" value={imovel.quartos != null ? String(imovel.quartos) : 'Não informado'} />
        <DetailRow label="Banheiros" value={imovel.banheiros != null ? String(imovel.banheiros) : 'Não informado'} />
        <DetailRow label="Vagas" value={imovel.vagas != null ? String(imovel.vagas) : 'Não informado'} />
        <DetailRow label="Área" value={imovel.areaM2 != null ? `${imovel.areaM2} m²` : 'Não informado'} />
        <DetailRow label="IPTU" value={imovel.iptu != null ? formatCurrency(imovel.iptu) : 'Não informado'} />
        <DetailRow label="Visibilidade" value={imovel.visibilidade === 'PUBLICADO' ? 'Público' : 'Privado'} />
        <Button
          label="Enviar novo convite"
          variant="outline"
          onPress={() => router.push(`/imoveis/${id}/convite`)}
        />
      </Card>

      <UnidadesCard imovel={imovel} onUpdated={setImovel} />

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
                    onPress={() => confirmarRevogarConvite(c.token)}
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

            <View
              className="flex-row items-center justify-between rounded-xl px-4 py-3 mt-1"
              style={{ borderWidth: 1.5, borderColor: '#2563EB', backgroundColor: '#EFF6FF' }}
            >
              <Text className="text-primary" style={{ fontSize: 13, fontWeight: '700' }}>Total do inquilino este mês</Text>
              <Text style={{ color: '#2563EB', fontSize: 15, fontWeight: '800' }}>{formatCurrency(totalInquilinoMes)}</Text>
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
          Contas ({contas.length})
        </Text>
        {contas.length === 0 ? (
          <Text className="text-muted">Nenhuma conta registrada para este imóvel ainda.</Text>
        ) : (
          contas.map((c) => (
            <View
              key={c.id}
              className="flex-row items-center justify-between rounded-xl px-4 py-3"
              style={{ borderWidth: 1, borderColor: '#E5E7EB' }}
            >
              <View>
                <Text className="text-primary" style={{ fontWeight: '600' }}>{c.tipoContaNome}</Text>
                <Text className="text-muted" style={{ fontSize: 13 }}>
                  Ref. {formatDate(c.vencimento)} · {c.responsavel === 'INQUILINO' ? 'Inquilino' : 'Proprietário'}
                </Text>
              </View>
              <View className="items-end gap-1">
                <Text className="text-primary" style={{ fontSize: 13, fontWeight: '600' }}>{formatCurrency(c.valor)}</Text>
                <StatusBadge status={c.status} />
              </View>
            </View>
          ))
        )}

        <View className="gap-2 mt-2" style={{ borderTopWidth: 1, borderTopColor: '#E5E7EB', paddingTop: 12 }}>
          <Text className="text-primary" style={{ fontSize: 14, fontWeight: '700' }}>Adicionar conta</Text>

          <Text className="text-muted" style={{ fontSize: 12, fontWeight: '600' }}>Tipo de conta</Text>
          <View className="flex-row gap-2" style={{ flexWrap: 'wrap' }}>
            {tiposConta.map((tipo) => (
              <Pill key={tipo.id} label={tipo.nome} selected={tipoContaId === tipo.id} onPress={() => setTipoContaId(tipo.id)} />
            ))}
          </View>
          <View className="flex-row gap-2">
            <TextInput
              placeholder="Novo tipo de conta (ex. IPTU)"
              placeholderTextColor="#9CA3AF"
              value={novoTipoContaNome}
              onChangeText={setNovoTipoContaNome}
              className="bg-card px-4 py-3 text-primary rounded-xl"
              style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14, flex: 1 }}
            />
            <Button label="Criar tipo" variant="outline" onPress={criarTipoConta} loading={criandoTipoConta} />
          </View>

          <Text className="text-muted" style={{ fontSize: 12, fontWeight: '600', marginTop: 8 }}>Responsável</Text>
          <View className="flex-row gap-2">
            <Pill
              label="Proprietário"
              selected={responsavelConta === 'PROPRIETARIO'}
              onPress={() => setResponsavelConta('PROPRIETARIO')}
            />
            {contrato ? (
              <Pill
                label="Inquilino"
                selected={responsavelConta === 'INQUILINO'}
                onPress={() => setResponsavelConta('INQUILINO')}
              />
            ) : (
              <Text className="text-muted" style={{ fontSize: 12, alignSelf: 'center' }}>
                Sem contrato ativo pra atribuir ao inquilino
              </Text>
            )}
          </View>

          <TextInput
            placeholder="Referência (AAAA-MM-DD)"
            placeholderTextColor="#9CA3AF"
            value={vencimentoConta}
            onChangeText={setVencimentoConta}
            className="bg-card px-4 py-3 text-primary rounded-xl"
            style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
          />
          <TextInput
            placeholder="Valor"
            placeholderTextColor="#9CA3AF"
            value={valorConta}
            onChangeText={setValorConta}
            keyboardType="numeric"
            className="bg-card px-4 py-3 text-primary rounded-xl"
            style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
          />
          <Button label="Adicionar conta" onPress={adicionarConta} loading={salvandoConta} />
        </View>
      </Card>

      <Card className="gap-3">
        <Text className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>
          Chamados ({chamados.length})
        </Text>
        {chamados.length === 0 ? (
          <Text className="text-muted">Nenhum chamado aberto para este imóvel.</Text>
        ) : (
          chamados.map((c) => (
            <View key={c.id} className="gap-2 rounded-xl px-4 py-3" style={{ borderWidth: 1, borderColor: '#E5E7EB' }}>
              <View className="flex-row items-center justify-between gap-2">
                <Text className="text-primary" style={{ fontWeight: '700' }}>{c.categoriaNome}</Text>
                <Text style={{ color: CHAMADO_STATUS_COLOR[c.status] ?? '#6B7280', fontSize: 12, fontWeight: '700' }}>
                  {formatTipo(c.status)}
                </Text>
              </View>
              <Text className="text-muted" style={{ fontSize: 13 }}>{c.descricao}</Text>
              <Text className="text-muted" style={{ fontSize: 12 }}>
                {c.unidadeNome ? `${c.unidadeNome} · ` : ''}Aberto em {formatDate(c.abertoEm)}
              </Text>
              {c.status !== 'RESOLVIDO' ? (
                <View className="flex-row gap-2 mt-1">
                  {c.status === 'ABERTO' ? (
                    <Button
                      label="Marcar em andamento"
                      variant="outline"
                      onPress={() => atualizarChamado(c.id, 'EM_ANDAMENTO')}
                      loading={atualizandoChamadoId === c.id}
                      disabled={atualizandoChamadoId === c.id}
                    />
                  ) : null}
                  <Button
                    label="Marcar resolvido"
                    onPress={() => atualizarChamado(c.id, 'RESOLVIDO')}
                    loading={atualizandoChamadoId === c.id}
                    disabled={atualizandoChamadoId === c.id}
                  />
                </View>
              ) : null}
            </View>
          ))
        )}

        <View className="gap-2 mt-2" style={{ borderTopWidth: 1, borderTopColor: '#E5E7EB', paddingTop: 12 }}>
          <Text className="text-primary" style={{ fontSize: 14, fontWeight: '700' }}>Categorias de chamado</Text>
          <View className="flex-row gap-2" style={{ flexWrap: 'wrap' }}>
            {categoriasChamado.map((cat) => (
              <View
                key={cat.id}
                className="px-[13px] py-2 rounded-lg"
                style={{ borderWidth: 1.5, borderColor: '#E5E7EB', backgroundColor: '#FFFFFF' }}
              >
                <Text style={{ color: '#374151', fontSize: 12.5, fontWeight: '600' }}>{cat.nome}</Text>
              </View>
            ))}
          </View>
          <View className="flex-row gap-2">
            <TextInput
              placeholder="Nova categoria (ex. Pintura)"
              placeholderTextColor="#9CA3AF"
              value={novaCategoriaNome}
              onChangeText={setNovaCategoriaNome}
              className="bg-card px-4 py-3 text-primary rounded-xl"
              style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14, flex: 1 }}
            />
            <Button label="Criar categoria" variant="outline" onPress={criarCategoriaChamado} loading={criandoCategoria} />
          </View>
        </View>
      </Card>
      <ConfirmDialog
        visible={confirmRemoverFoto.visible}
        title={confirmRemoverFoto.title}
        message={confirmRemoverFoto.message}
        confirmLabel={confirmRemoverFoto.confirmLabel}
        loading={removendoFotoId !== null}
        onConfirm={confirmRemoverFoto.accept}
        onCancel={confirmRemoverFoto.cancel}
      />
      <ConfirmDialog
        visible={confirmRevogar.visible}
        title={confirmRevogar.title}
        message={confirmRevogar.message}
        confirmLabel={confirmRevogar.confirmLabel}
        loading={revogandoToken !== null}
        onConfirm={confirmRevogar.accept}
        onCancel={confirmRevogar.cancel}
      />
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

