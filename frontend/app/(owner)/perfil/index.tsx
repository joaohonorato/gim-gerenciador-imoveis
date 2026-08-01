import { useCallback, useState } from 'react';
import { ActivityIndicator, ScrollView, Text, View } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { apiFetch } from '@/api/client';
import { escolherEEnviarAvatar } from '@/api/avatar';
import { MeResponse } from '@/api/types';
import { Avatar } from '@/design/Avatar';
import { Button } from '@/design/Button';
import { Card } from '@/design/Card';

export default function PerfilProprietarioScreen() {
  const [me, setMe] = useState<MeResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');

  const carregar = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setMe(await apiFetch<MeResponse>('/auth/me'));
    } catch (e: any) {
      setError(e.message ?? 'Não foi possível carregar seu perfil');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { void carregar(); }, [carregar]));

  async function trocarFoto() {
    setEnviando(true);
    setError('');
    try {
      const atualizado = await escolherEEnviarAvatar();
      if (atualizado) setMe(atualizado);
    } catch (e: any) {
      setError(e.message ?? 'Não foi possível enviar a foto');
    } finally {
      setEnviando(false);
    }
  }

  if (loading) {
    return (
      <View className="flex-1 items-center justify-center bg-surface gap-3">
        <ActivityIndicator color="#2563EB" />
        <Text className="text-muted">Carregando perfil...</Text>
      </View>
    );
  }

  return (
    <ScrollView className="flex-1 bg-surface" contentContainerClassName="p-6 gap-4">
      <View className="flex-row items-center gap-4">
        <Button label="← Voltar" variant="outline" onPress={() => router.back()} />
        <Text className="text-primary" style={{ fontSize: 22, fontWeight: '800' }}>Meu perfil</Text>
      </View>

      {error ? <Card><Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text></Card> : null}

      <Card className="items-center gap-4" style={{ paddingVertical: 32 }}>
        <Avatar url={me?.avatarUrl} nome={me?.nome} size={96} />
        <Button label="Trocar foto" variant="outline" onPress={trocarFoto} loading={enviando} disabled={enviando} />
      </Card>

      <Card className="gap-3">
        <DetailRow label="Nome" value={me?.nome ?? '—'} />
        <DetailRow label="E-mail" value={me?.email ?? '—'} />
      </Card>
    </ScrollView>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <View className="flex-row items-center justify-between gap-4">
      <Text className="text-muted" style={{ fontSize: 14 }}>{label}</Text>
      <Text className="text-primary text-right" style={{ fontSize: 14, fontWeight: '600' }}>{value}</Text>
    </View>
  );
}
