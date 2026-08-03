import { useState } from 'react';
import { View, Text, TextInput, ScrollView, ActivityIndicator } from 'react-native';
import { router } from 'expo-router';
import { apiFetch } from '@/api/client';
import { Imovel, NovoImovelRequest } from '@/api/types';
import { Card } from '@/design/Card';
import { Button } from '@/design/Button';
import { Pill } from '@/design/Pill';

interface ViaCepResponse {
  erro?: boolean;
  logradouro?: string;
  bairro?: string;
  localidade?: string;
}

export default function NovoImovelScreen() {
  const [cep, setCep] = useState('');
  const [buscandoCep, setBuscandoCep] = useState(false);
  const [endereco, setEndereco] = useState('');
  const [numero, setNumero] = useState('');
  const [bairro, setBairro] = useState('');
  const [complemento, setComplemento] = useState('');
  const [cidade, setCidade] = useState('');
  const [matricula, setMatricula] = useState('');
  const [tipoImovel, setTipoImovel] = useState<'CASA' | 'APARTAMENTO' | 'COMERCIAL' | 'OUTRO'>('CASA');
  const [quartos, setQuartos] = useState('');
  const [banheiros, setBanheiros] = useState('');
  const [vagas, setVagas] = useState('');
  const [areaM2, setAreaM2] = useState('');
  const [iptu, setIptu] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function onChangeCep(valor: string) {
    setCep(valor);
    const digitos = valor.replace(/\D/g, '');
    if (digitos.length !== 8) return;

    setBuscandoCep(true);
    try {
      const res = await fetch(`https://viacep.com.br/ws/${digitos}/json/`);
      const data: ViaCepResponse = await res.json();
      if (!data.erro) {
        if (data.logradouro) setEndereco(data.logradouro);
        if (data.bairro) setBairro(data.bairro);
        if (data.localidade) setCidade(data.localidade);
      }
    } catch {
      // CEP autofill é conveniência, não crítico — falha silenciosa, usuário preenche na mão.
    } finally {
      setBuscandoCep(false);
    }
  }

  function numeroOuIndefinido(valor: string): number | undefined {
    const n = Number(valor.replace(',', '.'));
    return valor.trim() === '' || Number.isNaN(n) ? undefined : n;
  }

  async function salvar() {
    setLoading(true);
    setError('');
    try {
      const body: NovoImovelRequest = {
        endereco,
        cidade,
        matricula,
        numero: numero.trim() || undefined,
        bairro: bairro.trim() || undefined,
        complemento: complemento.trim() || undefined,
        tipoImovel,
        cep: cep.replace(/\D/g, '') || undefined,
        quartos: numeroOuIndefinido(quartos),
        banheiros: numeroOuIndefinido(banheiros),
        vagas: numeroOuIndefinido(vagas),
        areaM2: numeroOuIndefinido(areaM2),
        iptu: numeroOuIndefinido(iptu),
      };
      await apiFetch<Imovel>('/imoveis', {
        method: 'POST',
        body: JSON.stringify(body),
      });
      router.replace('/imoveis');
    } catch (e: any) {
      setError(e.message ?? 'Erro ao salvar imóvel');
    } finally {
      setLoading(false);
    }
  }

  return (
    <ScrollView className="flex-1 bg-surface" contentContainerClassName="p-6">
      <View className="flex-row items-center mb-6 gap-4">
        <Button label="← Voltar" onPress={() => router.back()} variant="outline" />
        <Text className="text-primary" style={{ fontSize: 24, fontWeight: '800' }}>Novo imóvel</Text>
      </View>

      <Card className="p-8 gap-4">
        <Field label="CEP">
          <View className="flex-row items-center gap-3">
            <TextInput
              testID="input-cep"
              placeholder="00000-000"
              placeholderTextColor="#9CA3AF"
              value={cep}
              onChangeText={onChangeCep}
              keyboardType="numeric"
              maxLength={9}
              className="bg-card px-4 py-3 text-primary rounded-xl"
              style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14, flex: 1 }}
            />
            {buscandoCep ? <ActivityIndicator color="#2563EB" /> : null}
          </View>
        </Field>
        <Field label="Endereço">
          <TextInput
            testID="input-endereco"
            placeholder="Rua/Avenida"
            placeholderTextColor="#9CA3AF"
            value={endereco}
            onChangeText={setEndereco}
            className="bg-card px-4 py-3 text-primary rounded-xl"
            style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
          />
        </Field>
        <Field label="Número">
          <TextInput
            placeholder="Ex: 120"
            placeholderTextColor="#9CA3AF"
            value={numero}
            onChangeText={setNumero}
            className="bg-card px-4 py-3 text-primary rounded-xl"
            style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
          />
        </Field>
        <Field label="Bairro">
          <TextInput
            placeholder="Bairro"
            placeholderTextColor="#9CA3AF"
            value={bairro}
            onChangeText={setBairro}
            className="bg-card px-4 py-3 text-primary rounded-xl"
            style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
          />
        </Field>
        <Field label="Complemento">
          <TextInput
            placeholder="Apto, bloco, casa, sala..."
            placeholderTextColor="#9CA3AF"
            value={complemento}
            onChangeText={setComplemento}
            className="bg-card px-4 py-3 text-primary rounded-xl"
            style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
          />
        </Field>
        <Field label="Cidade">
          <TextInput
            testID="input-cidade"
            placeholder="Cidade"
            placeholderTextColor="#9CA3AF"
            value={cidade}
            onChangeText={setCidade}
            className="bg-card px-4 py-3 text-primary rounded-xl"
            style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
          />
        </Field>
        <Field label="Matrícula">
          <TextInput
            testID="input-matricula"
            placeholder="Número da matrícula"
            placeholderTextColor="#9CA3AF"
            value={matricula}
            onChangeText={setMatricula}
            className="bg-card px-4 py-3 text-primary rounded-xl"
            style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
          />
        </Field>

        <Field label="Tipo do imóvel">
          <View className="flex-row gap-2" style={{ flexWrap: 'wrap' }}>
            {(['CASA', 'APARTAMENTO', 'COMERCIAL', 'OUTRO'] as const).map((tipo) => (
              <Pill
                key={tipo}
                label={tipo}
                selected={tipoImovel === tipo}
                onPress={() => setTipoImovel(tipo)}
              />
            ))}
          </View>
        </Field>

        <View className="flex-row gap-4">
          <View style={{ flex: 1 }}>
            <Field label="Quartos">
              <TextInput
                testID="input-quartos"
                placeholder="0"
                placeholderTextColor="#9CA3AF"
                value={quartos}
                onChangeText={setQuartos}
                keyboardType="numeric"
                className="bg-card px-4 py-3 text-primary rounded-xl"
                style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
              />
            </Field>
          </View>
          <View style={{ flex: 1 }}>
            <Field label="Banheiros">
              <TextInput
                testID="input-banheiros"
                placeholder="0"
                placeholderTextColor="#9CA3AF"
                value={banheiros}
                onChangeText={setBanheiros}
                keyboardType="numeric"
                className="bg-card px-4 py-3 text-primary rounded-xl"
                style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
              />
            </Field>
          </View>
          <View style={{ flex: 1 }}>
            <Field label="Vagas">
              <TextInput
                testID="input-vagas"
                placeholder="0"
                placeholderTextColor="#9CA3AF"
                value={vagas}
                onChangeText={setVagas}
                keyboardType="numeric"
                className="bg-card px-4 py-3 text-primary rounded-xl"
                style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
              />
            </Field>
          </View>
        </View>

        <View className="flex-row gap-4">
          <View style={{ flex: 1 }}>
            <Field label="Área (m²)">
              <TextInput
                testID="input-area"
                placeholder="Ex: 75"
                placeholderTextColor="#9CA3AF"
                value={areaM2}
                onChangeText={setAreaM2}
                keyboardType="numeric"
                className="bg-card px-4 py-3 text-primary rounded-xl"
                style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
              />
            </Field>
          </View>
          <View style={{ flex: 1 }}>
            <Field label="IPTU (R$/ano)">
              <TextInput
                testID="input-iptu"
                placeholder="Ex: 1200"
                placeholderTextColor="#9CA3AF"
                value={iptu}
                onChangeText={setIptu}
                keyboardType="numeric"
                className="bg-card px-4 py-3 text-primary rounded-xl"
                style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
              />
            </Field>
          </View>
        </View>

        {error ? <Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text> : null}

        <Button testID="btn-salvar-imovel" label="Salvar imóvel" onPress={salvar} loading={loading} />
      </Card>
    </ScrollView>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <View>
      <Text className="mb-2" style={{ color: '#374151', fontSize: 13, fontWeight: '600' }}>{label}</Text>
      {children}
    </View>
  );
}
