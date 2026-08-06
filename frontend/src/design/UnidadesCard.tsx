import { useState } from 'react';
import { Text, TextInput, View } from 'react-native';
import { apiFetch, getErrorMessage } from '@/api/client';
import { AdicionarUnidadesRequest, Imovel } from '@/api/types';
import { Button } from './Button';
import { Card } from './Card';
import { StatusBadge } from './StatusBadge';

interface UnidadesCardProps {
  imovel: Imovel;
  onUpdated: (imovel: Imovel) => void;
}

// Lista as unidades do imóvel (a padrão automática + qualquer extra já
// cadastrada) e os dois jeitos de adicionar mais: individual (nome livre) e
// em lote (prefixo + quantidade, gera nomes numerados). Mesmo componente
// usado na tela de detalhe do imóvel e na etapa 2 do cadastro de imóvel novo
// — ver POST /imoveis/{id}/unidades (mesmo endpoint pros dois casos).
export function UnidadesCard({ imovel, onUpdated }: UnidadesCardProps) {
  const [novaUnidadeNome, setNovaUnidadeNome] = useState('');
  const [salvandoUnidadeIndividual, setSalvandoUnidadeIndividual] = useState(false);
  const [lotePrefixo, setLotePrefixo] = useState('');
  const [loteQuantidade, setLoteQuantidade] = useState('');
  const [salvandoUnidadeLote, setSalvandoUnidadeLote] = useState(false);
  const [error, setError] = useState('');

  async function adicionarUnidadeIndividual() {
    if (!novaUnidadeNome.trim()) return;
    setSalvandoUnidadeIndividual(true);
    setError('');
    try {
      const body: AdicionarUnidadesRequest = { nomes: [novaUnidadeNome.trim()] };
      const atualizado = await apiFetch<Imovel>(`/imoveis/${imovel.id}/unidades`, {
        method: 'POST',
        body: JSON.stringify(body),
      });
      onUpdated(atualizado);
      setNovaUnidadeNome('');
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível adicionar a unidade'));
    } finally {
      setSalvandoUnidadeIndividual(false);
    }
  }

  const loteQuantidadeNum = Number(loteQuantidade);
  const loteQuantidadeValida = Number.isInteger(loteQuantidadeNum) && loteQuantidadeNum > 0 && loteQuantidadeNum <= 50;

  async function adicionarUnidadesEmLote() {
    if (!lotePrefixo.trim() || !loteQuantidadeValida) return;
    setSalvandoUnidadeLote(true);
    setError('');
    try {
      const prefixo = lotePrefixo.trim();
      const nomes = Array.from({ length: loteQuantidadeNum }, (_, i) => `${prefixo} ${i + 1}`);
      const body: AdicionarUnidadesRequest = { nomes };
      const atualizado = await apiFetch<Imovel>(`/imoveis/${imovel.id}/unidades`, {
        method: 'POST',
        body: JSON.stringify(body),
      });
      onUpdated(atualizado);
      setLotePrefixo('');
      setLoteQuantidade('');
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível gerar as unidades'));
    } finally {
      setSalvandoUnidadeLote(false);
    }
  }

  return (
    <Card className="gap-3">
      <Text className="text-primary" style={{ fontSize: 16, fontWeight: '700' }}>
        Unidades ({imovel.unidades?.length ?? 0})
      </Text>
      <Text className="text-muted" style={{ fontSize: 12.5 }}>
        Todo imóvel tem 1 unidade padrão automática. Subdivida em mais unidades se for o caso — ex.: terreno com várias casas, cada uma com vários apartamentos.
      </Text>

      {error ? <Text style={{ color: '#DC2626', fontSize: 12.5 }}>{error}</Text> : null}

      {(imovel.unidades ?? []).map((u) => (
        <View
          key={u.id}
          className="flex-row items-center justify-between rounded-xl px-4 py-3"
          style={{ borderWidth: 1, borderColor: '#E5E7EB' }}
        >
          <Text className="text-primary" style={{ fontWeight: '600' }}>
            {u.nome}{u.padrao ? ' (padrão)' : ''}
          </Text>
          {/* StatusBadge não tem um estado dedicado pra MANUTENCAO — mesmo
              fallback usado em (owner)/convites/novo.tsx: mostra como
              RESERVADO (indisponível pra novo aluguel, mas não ALUGADO). */}
          <StatusBadge status={u.status === 'MANUTENCAO' ? 'RESERVADO' : u.status} />
        </View>
      ))}

      <View className="gap-2 mt-2" style={{ borderTopWidth: 1, borderTopColor: '#E5E7EB', paddingTop: 12 }}>
        <Text className="text-primary" style={{ fontSize: 14, fontWeight: '700' }}>Adicionar unidade individual</Text>
        <View className="flex-row gap-2">
          <TextInput
            testID="input-nova-unidade"
            placeholder="Nome (ex. Casa 1 - Apto 2)"
            placeholderTextColor="#9CA3AF"
            value={novaUnidadeNome}
            onChangeText={setNovaUnidadeNome}
            className="bg-card px-4 py-3 text-primary rounded-xl"
            style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14, flex: 1 }}
          />
          <Button
            testID="btn-adicionar-unidade"
            label="Adicionar"
            variant="outline"
            onPress={adicionarUnidadeIndividual}
            loading={salvandoUnidadeIndividual}
            disabled={!novaUnidadeNome.trim() || salvandoUnidadeIndividual}
          />
        </View>
      </View>

      <View className="gap-2 mt-2" style={{ borderTopWidth: 1, borderTopColor: '#E5E7EB', paddingTop: 12 }}>
        <Text className="text-primary" style={{ fontSize: 14, fontWeight: '700' }}>Gerar unidades em lote</Text>
        <Text className="text-muted" style={{ fontSize: 12.5 }}>
          Gera unidades numeradas a partir de um prefixo (ex. prefixo "Casa 1 - Apto" + 3 unidades = "Casa 1 - Apto 1", "Casa 1 - Apto 2", "Casa 1 - Apto 3").
        </Text>
        <View className="flex-row gap-2">
          <TextInput
            testID="input-lote-prefixo"
            placeholder="Prefixo (ex. Casa 1 - Apto)"
            placeholderTextColor="#9CA3AF"
            value={lotePrefixo}
            onChangeText={setLotePrefixo}
            className="bg-card px-4 py-3 text-primary rounded-xl"
            style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14, flex: 2 }}
          />
          <TextInput
            testID="input-lote-quantidade"
            placeholder="Qtd."
            placeholderTextColor="#9CA3AF"
            value={loteQuantidade}
            onChangeText={setLoteQuantidade}
            keyboardType="numeric"
            className="bg-card px-4 py-3 text-primary rounded-xl"
            style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14, flex: 1 }}
          />
        </View>
        <Button
          testID="btn-gerar-unidades-lote"
          label="Gerar unidades"
          variant="outline"
          onPress={adicionarUnidadesEmLote}
          loading={salvandoUnidadeLote}
          disabled={!lotePrefixo.trim() || !loteQuantidadeValida || salvandoUnidadeLote}
        />
      </View>
    </Card>
  );
}
