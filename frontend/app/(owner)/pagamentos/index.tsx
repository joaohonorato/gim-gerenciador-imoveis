import { useCallback, useMemo, useState } from 'react';
import { ActivityIndicator, Platform, Text, View } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { apiFetch, getErrorMessage } from '@/api/client';
import { Button } from '@/design/Button';
import { Card } from '@/design/Card';
import { StatusBadge } from '@/design/StatusBadge';
import { HubHeader } from '@/design/HubHeader';
import { HubScrollView } from '@/design/HubScrollView';
import { Pill } from '@/design/Pill';
import { StatCard } from '@/design/StatCard';
import { Contrato, Imovel, Pagamento } from '@/api/types';
import { isFuturo, isMesAtual } from '@/utils/pagamentos';

export default function PagamentosScreen() {
  const [pagamentos, setPagamentos] = useState<Pagamento[]>([]);
  const [contratos, setContratos] = useState<Contrato[]>([]);
  const [imoveis, setImoveis] = useState<Imovel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [somenteAcao, setSomenteAcao] = useState(true);

  const carregar = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [pagamentosData, contratosData, imoveisData] = await Promise.all([
        apiFetch<Pagamento[]>('/pagamentos'),
        apiFetch<Contrato[]>('/contratos'),
        apiFetch<Imovel[]>('/imoveis'),
      ]);
      setPagamentos(pagamentosData);
      setContratos(contratosData);
      setImoveis(imoveisData);
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível carregar os pagamentos'));
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { void carregar(); }, [carregar]));

  const contratoPorId = useMemo(() => {
    const map = new Map<string, Contrato>();
    for (const c of contratos) map.set(c.id, c);
    return map;
  }, [contratos]);

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
    return [...pagamentos].sort((a, b) => a.vencimento.localeCompare(b.vencimento));
  }, [pagamentos]);

  const exibidos = useMemo(() => {
    if (!somenteAcao) return ordenados;
    return ordenados.filter((p) => p.status === 'ATRASADO' || (p.status === 'PENDENTE' && !isFuturo(p.vencimento)));
  }, [ordenados, somenteAcao]);

  const stats = useMemo(() => {
    const doMes = pagamentos.filter((p) => isMesAtual(p.vencimento));
    const recebidoMes = doMes.filter((p) => p.status === 'PAGO').reduce((soma, p) => soma + p.valor, 0);
    const pendenteMes = doMes.filter((p) => p.status !== 'PAGO').reduce((soma, p) => soma + p.valor, 0);
    return { recebidoMes, pendenteMes };
  }, [pagamentos]);

  // Exporta exatamente o que está na tela (respeita o toggle "Precisam de
  // atenção"/"Todos") — gerado 100% no client porque a tela já carregou
  // todos os pagamentos via GET /pagamentos, sem paginação; não precisa de
  // endpoint novo só pra reformatar dado que já está na memória.
  function exportarCsv() {
    const linhas = [
      ['Vencimento', 'Valor', 'Status', 'Imóvel', 'Cidade'],
      ...exibidos.map((p) => {
        const contrato = contratoPorId.get(p.contratoId);
        const imovel = contrato ? imovelPorUnidade.get(contrato.unidadeId) : undefined;
        const agendado = p.status === 'PENDENTE' && isFuturo(p.vencimento);
        return [
          p.vencimento,
          p.valor.toFixed(2).replace('.', ','),
          agendado ? 'Agendado' : formatTipo(p.status),
          imovel?.endereco ?? '',
          imovel?.cidade ?? '',
        ];
      }),
    ];
    // Delimitador ; (não ,) porque o valor já usa vírgula decimal — convenção
    // padrão de CSV pt-BR, o Excel local abre direto sem assistente de importação.
    const csv = linhas.map((linha) => linha.map(escapeCsvField).join(';')).join('\r\n');
    // BOM garante que o Excel detecte UTF-8 e não estrague acentos (São Paulo etc).
    const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `pagamentos-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }

  if (loading) {
    return (
      <View className="flex-1 items-center justify-center bg-surface gap-3">
        <ActivityIndicator color="#2563EB" />
        <Text className="text-muted">Carregando pagamentos...</Text>
      </View>
    );
  }

  return (
    <View className="flex-1 bg-surface">
      <HubHeader title="Pagamentos" subtitle="Aluguéis de todos os seus contratos." />
      <View className="px-6 pb-4 flex-row gap-3" style={{ flexWrap: 'wrap' }}>
        <StatCard label="Recebido no mês" value={formatCurrency(stats.recebidoMes)} color="#16A34A" />
        <StatCard label="Pendente no mês" value={formatCurrency(stats.pendenteMes)} color="#D97706" />
      </View>
      <HubScrollView contentContainerClassName="px-6 pb-6 gap-3">
        {error ? <Card><Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text></Card> : null}

        <View className="flex-row items-center justify-between gap-2" style={{ flexWrap: 'wrap' }}>
          <View className="flex-row gap-2">
            <Pill label="Precisam de atenção" selected={somenteAcao} onPress={() => setSomenteAcao(true)} />
            <Pill label="Todos" selected={!somenteAcao} onPress={() => setSomenteAcao(false)} />
          </View>
          {Platform.OS === 'web' ? (
            <Button
              testID="btn-exportar-csv"
              label="Exportar CSV"
              variant="outline"
              onPress={exportarCsv}
              disabled={exibidos.length === 0}
            />
          ) : null}
        </View>

        {exibidos.length === 0 ? (
          <Card><Text className="text-muted">Nenhum pagamento {somenteAcao ? 'pendente de atenção' : 'encontrado'}.</Text></Card>
        ) : (
          exibidos.map((p) => {
            const contrato = contratoPorId.get(p.contratoId);
            const imovel = contrato ? imovelPorUnidade.get(contrato.unidadeId) : undefined;
            const agendado = p.status === 'PENDENTE' && isFuturo(p.vencimento);
            return (
              <Card key={p.id} className="gap-2">
                <View className="flex-row items-center justify-between gap-2">
                  <Text className="text-primary" style={{ fontWeight: '700' }}>Venc. {formatDate(p.vencimento)}</Text>
                  {agendado
                    ? <Text style={{ color: '#6B7280', fontSize: 12, fontWeight: '700' }}>Agendado</Text>
                    : <StatusBadge status={p.status} />}
                </View>
                {imovel ? <Text className="text-muted" style={{ fontSize: 13 }}>{imovel.endereco} — {imovel.cidade}</Text> : null}
                <Text className="text-primary" style={{ fontSize: 14, fontWeight: '600' }}>{formatCurrency(p.valor)}</Text>
                {contrato ? (
                  <Text
                    className="text-muted"
                    style={{ fontSize: 12, textDecorationLine: 'underline' }}
                    onPress={() => router.push(`/${contrato.id}/revisar`)}
                  >
                    Ver contrato
                  </Text>
                ) : null}
              </Card>
            );
          })
        )}
      </HubScrollView>
    </View>
  );
}

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function formatDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString('pt-BR');
}

function formatTipo(value: string) {
  const normalized = value.replace(/_/g, ' ').toLowerCase();
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}

function escapeCsvField(value: string) {
  if (/[;"\n]/.test(value)) {
    return `"${value.replace(/"/g, '""')}"`;
  }
  return value;
}
