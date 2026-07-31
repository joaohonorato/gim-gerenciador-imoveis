import { useState } from 'react';
import { View, Text, TextInput, ScrollView } from 'react-native';
import { router } from 'expo-router';
import { apiFetch } from '@/api/client';
import { Imovel } from '@/api/types';
import { Card } from '@/design/Card';
import { Button } from '@/design/Button';
import { Pill } from '@/design/Pill';

export default function NovoImovelScreen() {
  const [endereco, setEndereco] = useState('');
  const [numero, setNumero] = useState('');
  const [bairro, setBairro] = useState('');
  const [complemento, setComplemento] = useState('');
  const [cidade, setCidade] = useState('');
  const [matricula, setMatricula] = useState('');
  const [tipoImovel, setTipoImovel] = useState<'CASA' | 'APARTAMENTO' | 'COMERCIAL' | 'OUTRO'>('CASA');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function salvar() {
    setLoading(true);
    setError('');
    try {
      await apiFetch<Imovel>('/imoveis', {
        method: 'POST',
        body: JSON.stringify({
          endereco,
          cidade,
          matricula,
          numero: numero.trim() || null,
          bairro: bairro.trim() || null,
          complemento: complemento.trim() || null,
          tipoImovel,
        }),
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
