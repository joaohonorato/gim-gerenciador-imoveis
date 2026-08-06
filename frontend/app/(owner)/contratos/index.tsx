import { useCallback, useMemo, useState } from 'react';
import { ActivityIndicator, Text, View } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { apiFetch, getErrorMessage } from '@/api/client';
import { Button } from '@/design/Button';
import { Card } from '@/design/Card';
import { StatusBadge } from '@/design/StatusBadge';
import { HubHeader } from '@/design/HubHeader';
import { HubScrollView } from '@/design/HubScrollView';
import { StatCard } from '@/design/StatCard';
import { Pill } from '@/design/Pill';
import { Contrato, Imovel } from '@/api/types';

export default function ContratosScreen() {
  const [contratos, setContratos] = useState<Contrato[]>([]);
  const [imoveis, setImoveis] = useState<Imovel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  // Default false (mostra Todos): o filtro "vencendo em breve" exclui os
  // contratos ASSINADOS que não estão perto do fim — ou seja, a maior parte
  // da carteira saudável do proprietário, não só histórico/futuro. Mesmo
  // raciocínio do hub de Imóveis (rank 8, docs/jornadas-e-backlog-tecnico.md).
  const [somenteAtencao, setSomenteAtencao] = useState(false);

  const carregar = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [contratosData, imoveisData] = await Promise.all([
        apiFetch<Contrato[]>('/contratos'),
        apiFetch<Imovel[]>('/imoveis'),
      ]);
      setContratos(contratosData);
      setImoveis(imoveisData);
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível carregar os contratos'));
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { void carregar(); }, [carregar]));

  const imovelPorUnidade = useMemo(() => {
    const map = new Map<string, Imovel>();
    for (const imovel of imoveis) {
      for (const unidade of imovel.unidades ?? []) {
        map.set(unidade.id, imovel);
      }
    }
    return map;
  }, [imoveis]);

  const ordenados = useMemo(() => {
    return [...contratos].sort((a, b) => {
      if (a.statusAssinatura === b.statusAssinatura) return 0;
      return a.statusAssinatura === 'PENDENTE' ? -1 : 1;
    });
  }, [contratos]);

  const stats = useMemo(() => {
    const assinados = contratos.filter((c) => c.statusAssinatura === 'ASSINADO');
    const pendentes = contratos.filter((c) => c.statusAssinatura === 'PENDENTE');
    const receitaMensal = assinados.reduce((soma, c) => soma + c.valorAluguel, 0);
    return { receitaMensal, assinados: assinados.length, pendentes: pendentes.length };
  }, [contratos]);

  // Regra documentada em docs/especificacao-produto.md: avisar
  // o proprietário 60 dias antes do fim do contrato. Sem scheduler
  // server-side — calculado no client a partir de dataFim, que já vem em
  // ContratoResponse.
  const diasParaVencer = useMemo(() => {
    const hoje = new Date();
    hoje.setHours(0, 0, 0, 0);
    const map = new Map<string, number>();
    for (const contrato of contratos) {
      const fim = new Date(`${contrato.dataFim}T00:00:00`);
      const dias = Math.round((fim.getTime() - hoje.getTime()) / (1000 * 60 * 60 * 24));
      map.set(contrato.id, dias);
    }
    return map;
  }, [contratos]);

  // Critério de "precisa de atenção": contrato assinado vencendo em 60 dias
  // ou menos — mesmo cálculo usado pra decidir se mostra o VencimentoBadge
  // em cada card, então os dois nunca divergem.
  const precisaAtencao = useCallback((contrato: Contrato) => {
    if (contrato.statusAssinatura !== 'ASSINADO') return false;
    const dias = diasParaVencer.get(contrato.id);
    return dias != null && dias <= 60;
  }, [diasParaVencer]);

  const exibidos = useMemo(() => {
    if (!somenteAtencao) return ordenados;
    return ordenados.filter(precisaAtencao);
  }, [ordenados, somenteAtencao, precisaAtencao]);

  if (loading) {
    return (
      <View className="flex-1 items-center justify-center bg-surface gap-3">
        <ActivityIndicator color="#2563EB" />
        <Text className="text-muted">Carregando contratos...</Text>
      </View>
    );
  }

  return (
    <View className="flex-1 bg-surface">
      <HubHeader title="Contratos" subtitle="Todos os contratos da sua carteira." />
      <View className="px-6 pb-4 flex-row gap-3" style={{ flexWrap: 'wrap' }}>
        <StatCard label="Receita mensal recorrente" value={formatCurrency(stats.receitaMensal)} color="#16A34A" />
        <StatCard label="Assinados" value={stats.assinados} color="#2563EB" />
        <StatCard label="Pendentes" value={stats.pendentes} color="#D97706" />
      </View>
      <HubScrollView contentContainerClassName="px-6 pb-6 gap-3">
        {error ? <Card><Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text></Card> : null}

        <View className="flex-row gap-2">
          <Pill label="Vencendo em breve" selected={somenteAtencao} onPress={() => setSomenteAtencao(true)} />
          <Pill label="Todos" selected={!somenteAtencao} onPress={() => setSomenteAtencao(false)} />
        </View>

        {exibidos.length === 0 ? (
          <Card><Text className="text-muted">Nenhum contrato {somenteAtencao ? 'vencendo em breve' : 'encontrado'}.</Text></Card>
        ) : (
          exibidos.map((contrato) => {
            const imovel = imovelPorUnidade.get(contrato.unidadeId);
            const dias = diasParaVencer.get(contrato.id) ?? null;
            // Só faz sentido avisar de vencimento pra contrato ativo — um
            // contrato ainda pendente de assinatura não está "vencendo".
            const vencimento = precisaAtencao(contrato) ? dias : null;
            return (
              <Card key={contrato.id} className="gap-2">
                <View className="flex-row items-center justify-between gap-2">
                  <Text className="text-primary" style={{ fontWeight: '700' }}>
                    {formatTipo(contrato.tipo ?? contrato.tipoContrato)}
                  </Text>
                  <StatusBadge status={contrato.statusAssinatura} />
                </View>
                {vencimento != null ? <VencimentoBadge dias={vencimento} /> : null}
                {imovel ? <Text className="text-muted" style={{ fontSize: 13 }}>{imovel.endereco} — {imovel.cidade}</Text> : null}
                <Text className="text-muted" style={{ fontSize: 13 }}>
                  {formatDate(contrato.dataInicio)} até {formatDate(contrato.dataFim)} — {formatCurrency(contrato.valorAluguel)}
                </Text>
                <View className="flex-row gap-2">
                  <Button
                    label={contrato.statusAssinatura === 'PENDENTE' ? 'Revisar e assinar' : 'Abrir contrato'}
                    onPress={() => router.push(`/${contrato.id}/revisar`)}
                  />
                  <Button label="Ver inquilino" variant="outline" onPress={() => router.push(`/inquilinos/${contrato.inquilinoId}`)} />
                </View>
              </Card>
            );
          })
        )}
      </HubScrollView>
    </View>
  );
}

// Mesmo padrão visual do StatusBadge (forma geométrica colorida + texto
// curto), mas com formas reservadas pra sinalização de urgência no design
// system (docs/especificacao-produto.md §6): quadrado =
// atenção, triângulo = urgente — distintas do círculo usado pelos status
// normais, pra continuar legível sem depender só da cor (daltonismo).
function VencimentoBadge({ dias }: { dias: number }) {
  const vencido = dias < 0;
  const color = vencido ? '#DC2626' : '#D97706';
  const label = vencido
    ? `Vencido há ${Math.abs(dias)} dia${Math.abs(dias) === 1 ? '' : 's'}`
    : dias === 0
      ? 'Vence hoje'
      : `Vence em ${dias} dias`;

  return (
    <View className="flex-row items-center gap-2">
      {vencido ? (
        <View
          style={{
            width: 0,
            height: 0,
            borderLeftWidth: 5,
            borderRightWidth: 5,
            borderBottomWidth: 8,
            borderLeftColor: 'transparent',
            borderRightColor: 'transparent',
            borderBottomColor: color,
          }}
        />
      ) : (
        <View style={{ width: 8, height: 8, backgroundColor: color }} />
      )}
      <Text style={{ color, fontSize: 12, fontWeight: '700' }}>{label}</Text>
    </View>
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
