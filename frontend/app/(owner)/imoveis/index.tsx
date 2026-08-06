import { useCallback, useEffect, useMemo, useState } from 'react';
import { View, Text, Image, TextInput } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { apiFetch } from '@/api/client';
import { Contrato, Imovel, Inquilino, Pagamento } from '@/api/types';
import { Card } from '@/design/Card';
import { Button } from '@/design/Button';
import { Pill } from '@/design/Pill';
import { StatusBadge } from '@/design/StatusBadge';
import { HubHeader } from '@/design/HubHeader';
import { HubScrollView } from '@/design/HubScrollView';
import { StatCard } from '@/design/StatCard';
import { isMesAtual } from '@/utils/pagamentos';

type StatusFiltro = 'VAGO' | 'RESERVADO' | 'ALUGADO' | 'MANUTENCAO';
type TipoFiltro = 'CASA' | 'APARTAMENTO' | 'COMERCIAL' | 'OUTRO';

const STATUS_OPCOES: { value: StatusFiltro | null; label: string }[] = [
  { value: null, label: 'Todos' },
  { value: 'VAGO', label: 'Vago' },
  { value: 'RESERVADO', label: 'Reservado' },
  { value: 'ALUGADO', label: 'Alugado' },
  { value: 'MANUTENCAO', label: 'Manutenção' },
];

const TIPO_OPCOES: { value: TipoFiltro | null; label: string }[] = [
  { value: null, label: 'Todos' },
  { value: 'CASA', label: 'Casa' },
  { value: 'APARTAMENTO', label: 'Apartamento' },
  { value: 'COMERCIAL', label: 'Comercial' },
  { value: 'OUTRO', label: 'Outro' },
];

export default function ImoveisScreen() {
  const [imoveis, setImoveis] = useState<Imovel[]>([]);
  const [totalImoveis, setTotalImoveis] = useState<Imovel[]>([]);
  const [contratos, setContratos] = useState<Contrato[]>([]);
  const [pagamentos, setPagamentos] = useState<Pagamento[]>([]);
  const [inquilinos, setInquilinos] = useState<Record<string, Inquilino>>({});
  const [loading, setLoading] = useState(true);

  const [buscaInput, setBuscaInput] = useState('');
  const [busca, setBusca] = useState('');
  const [statusFiltro, setStatusFiltro] = useState<StatusFiltro | null>(null);
  const [tipoFiltro, setTipoFiltro] = useState<TipoFiltro | null>(null);
  // Default false (mostra Todos): diferente de Convites/Pagamentos, o filtro
  // "precisa de atenção" aqui exclui os imóveis ALUGADOS — ou seja, o
  // patrimônio ativo e funcionando do proprietário — não só itens
  // concluídos/futuros. Com default true, um proprietário com carteira
  // saudável (tudo alugado, nada vago há >30 dias) via a lista inteira
  // sumir por trás de "Nenhum imóvel vago há mais de 30 dias", mesmo tendo
  // imóveis cadastrados. Ver docs/jornadas-e-backlog-tecnico.md, rank 8.
  const [somenteAtencao, setSomenteAtencao] = useState(false);

  // Debounce só do texto digitado — cliques nos chips de status/tipo já
  // disparam a busca na hora, sem precisar esperar o usuário parar de
  // digitar em outro campo.
  useEffect(() => {
    const timeout = setTimeout(() => setBusca(buscaInput), 350);
    return () => clearTimeout(timeout);
  }, [buscaInput]);

  const carregarContratos = useCallback(async () => {
    try {
      const contratosData = await apiFetch<Contrato[]>('/contratos');
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
    } catch {
      // Falha aqui (incl. sessão expirada — já tratada de forma central em
      // apiFetch) não impede o resto da tela; a lista de imóveis é o que
      // importa carregar, contratos/inquilinos só enriquecem os cards.
    }
  }, []);

  // Sem filtro nenhum — mantém os contadores do topo mostrando o total real
  // cadastrado, mesmo com busca/filtros ativos na lista abaixo.
  const carregarTotais = useCallback(async () => {
    try {
      setTotalImoveis(await apiFetch<Imovel[]>('/imoveis'));
    } catch {
      // Falha aqui não impede o resto da tela; os contadores só ficam
      // desatualizados até o próximo focus.
    }
  }, []);

  // Endpoint já existente (mesmo usado pelo hub de Pagamentos) — só ainda
  // não era buscado nesta tela. Precisa dele pra inadimplência do mês,
  // já que status de pagamento não vem em /imoveis nem /contratos.
  const carregarPagamentos = useCallback(async () => {
    try {
      setPagamentos(await apiFetch<Pagamento[]>('/pagamentos'));
    } catch {
      // Idem carregarTotais — não impede o resto da tela.
    }
  }, []);

  const carregarImoveis = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (busca.trim()) params.set('busca', busca.trim());
      if (statusFiltro) params.set('status', statusFiltro);
      if (tipoFiltro) params.set('tipoImovel', tipoFiltro);
      const query = params.toString();
      const imoveisData = await apiFetch<Imovel[]>(`/imoveis${query ? `?${query}` : ''}`);
      setImoveis(imoveisData);
    } catch {
      // Sessão expirada já é tratada de forma central em apiFetch (limpa e
      // redireciona pro login); outras falhas deixam a lista como estava.
    } finally {
      setLoading(false);
    }
  }, [busca, statusFiltro, tipoFiltro]);

  useFocusEffect(useCallback(() => { void carregarContratos(); }, [carregarContratos]));
  useFocusEffect(useCallback(() => { void carregarTotais(); }, [carregarTotais]));
  useFocusEffect(useCallback(() => { void carregarPagamentos(); }, [carregarPagamentos]));
  useFocusEffect(useCallback(() => { void carregarImoveis(); }, [carregarImoveis]));

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

  const stats = totalImoveis.reduce((acc, imovel) => {
    const status = getStatus(imovel);
    acc.total += 1;
    if (status === 'ALUGADO') acc.alugados += 1;
    if (status === 'VAGO') acc.vagos += 1;
    return acc;
  }, { total: 0, alugados: 0, vagos: 0 });

  // Visão consolidada da carteira — primeira versão (KPIs simples, sem
  // comparativo de rentabilidade por imóvel ainda, que depende de Contas).
  // Calculado sobre totalImoveis/contratos/pagamentos (não filtrados pela
  // busca/chips da lista abaixo), pra representar a carteira inteira.
  const rendaMensalRecorrente = contratos
    .filter((c) => c.statusAssinatura === 'ASSINADO')
    .reduce((soma, c) => soma + c.valorAluguel, 0);

  const { totalUnidades, unidadesAlugadas } = totalImoveis.reduce((acc, imovel) => {
    for (const unidade of imovel.unidades ?? []) {
      acc.totalUnidades += 1;
      if (unidade.status === 'ALUGADO') acc.unidadesAlugadas += 1;
    }
    return acc;
  }, { totalUnidades: 0, unidadesAlugadas: 0 });
  const taxaOcupacao = totalUnidades > 0 ? unidadesAlugadas / totalUnidades : 0;

  const inadimplenciaMes = pagamentos
    .filter((p) => p.status === 'ATRASADO' && isMesAtual(p.vencimento))
    .reduce((soma, p) => soma + p.valor, 0);

  // Sem histórico de quando a unidade ficou VAGO (rastrear isso tocaria em
  // toda transição de status, espalhada por vários use cases) — usa a data
  // de cadastro do imóvel como aproximação, conforme decidido no pedido da
  // feature.
  const diasVago = useCallback((imovel: Imovel) => {
    const criado = new Date(imovel.criadoEm);
    const hoje = new Date();
    return Math.floor((hoje.getTime() - criado.getTime()) / (1000 * 60 * 60 * 24));
  }, []);

  const precisaAtencao = useCallback(
    (imovel: Imovel) => getStatus(imovel) === 'VAGO' && diasVago(imovel) > 30,
    [diasVago],
  );

  const imoveisExibidos = useMemo(() => {
    if (!somenteAtencao) return imoveis;
    return imoveis.filter(precisaAtencao);
  }, [imoveis, somenteAtencao, precisaAtencao]);

  return (
    <View className="flex-1 bg-surface">
      <HubHeader title="Meus imóveis" subtitle="Acompanhe status e dados de cada imóvel cadastrado." />
      <HubScrollView contentContainerClassName="pb-6">
      <View className="px-6 pb-4">
        <Button testID="btn-novo-imovel" label="+ Novo imóvel" onPress={() => router.push('/imoveis/novo')} />
      </View>

      <View className="px-6 pb-4 flex-row gap-3" style={{ flexWrap: 'wrap' }}>
        <StatCard label="Cadastrados" value={stats.total} color="#111827" />
        <StatCard label="Alugados" value={stats.alugados} color="#2563EB" />
        <StatCard label="Vagos" value={stats.vagos} color="#16A34A" />
      </View>

      <View className="px-6 pb-4 flex-row gap-3" style={{ flexWrap: 'wrap' }}>
        <StatCard label="Renda mensal recorrente" value={formatCurrency(rendaMensalRecorrente)} color="#16A34A" />
        <StatCard label="Taxa de ocupação" value={formatPercent(taxaOcupacao)} color="#2563EB" />
        <StatCard label="Inadimplência do mês" value={formatCurrency(inadimplenciaMes)} color="#DC2626" />
      </View>

      <View className="px-6 pb-4 gap-3">
        <TextInput
          testID="input-busca-imoveis"
          placeholder="Buscar por endereço, matrícula, bairro ou cidade"
          placeholderTextColor="#9CA3AF"
          value={buscaInput}
          onChangeText={setBuscaInput}
          className="bg-card px-4 py-3 text-primary rounded-xl"
          style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
        />
        <View className="flex-row gap-2" style={{ flexWrap: 'wrap' }}>
          {STATUS_OPCOES.map((opcao) => (
            <Pill
              key={opcao.label}
              label={opcao.label}
              selected={statusFiltro === opcao.value}
              onPress={() => setStatusFiltro(opcao.value)}
            />
          ))}
        </View>
        <View className="flex-row gap-2" style={{ flexWrap: 'wrap' }}>
          {TIPO_OPCOES.map((opcao) => (
            <Pill
              key={opcao.label}
              label={opcao.label}
              selected={tipoFiltro === opcao.value}
              onPress={() => setTipoFiltro(opcao.value)}
            />
          ))}
        </View>
        <View className="flex-row gap-2">
          <Pill label="Vago há mais de 30 dias" selected={somenteAtencao} onPress={() => setSomenteAtencao(true)} />
          <Pill label="Todos" selected={!somenteAtencao} onPress={() => setSomenteAtencao(false)} />
        </View>
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
        : imoveisExibidos.length === 0
        ? (
          <Text className="text-center text-muted mt-8">
            {somenteAtencao ? 'Nenhum imóvel vago há mais de 30 dias.' : 'Nenhum imóvel cadastrado.'}
          </Text>
        )
        : (
          <View className="px-6 gap-3">
            {imoveisExibidos.map((item) => {
              const status = getStatus(item);
              const unidadePadrao = item.unidades?.find((u) => u.padrao) ?? item.unidades?.[0];
              const contratoAtivo = unidadePadrao ? contratoAtivoPorUnidade.get(unidadePadrao.id) : undefined;
              const inquilino = contratoAtivo ? inquilinos[contratoAtivo.inquilinoId] : undefined;

              const primeiraFoto = item.fotos?.[0];

              return (
                <Card key={item.id} className="gap-3">
                  <View className="flex-row items-start justify-between gap-3">
                    <View className="flex-row items-start gap-3 flex-1">
                      {primeiraFoto ? (
                        <Image
                          source={{ uri: primeiraFoto.url }}
                          style={{ width: 56, height: 56, borderRadius: 8, borderWidth: 1, borderColor: '#E5E7EB' }}
                        />
                      ) : (
                        <View
                          style={{
                            width: 56, height: 56, borderRadius: 8,
                            borderWidth: 1, borderColor: '#E5E7EB', backgroundColor: '#F5F6F8',
                          }}
                        />
                      )}
                      <View className="flex-1">
                        <Text testID={`imovel-${item.id}`} className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>{item.endereco}</Text>
                        <Text className="text-muted mt-1" style={{ fontSize: 14 }}>{item.cidade}</Text>
                      </View>
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

                  <View className="flex-1">
                    <Text className="text-muted" style={{ fontSize: 13 }}>Aluguel vigente</Text>
                    <Text className="text-primary mt-1" style={{ fontSize: 14, fontWeight: '600' }}>
                      {contratoAtivo ? formatCurrency(contratoAtivo.valorAluguel) : 'Sem contrato ativo'}
                    </Text>
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
            })}
          </View>
        )
      }
      </HubScrollView>
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

function formatPercent(value: number) {
  return value.toLocaleString('pt-BR', { style: 'percent', minimumFractionDigits: 0, maximumFractionDigits: 1 });
}
