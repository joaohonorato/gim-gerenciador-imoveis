import { useCallback, useState } from 'react';
import { ActivityIndicator, ScrollView, Text, View } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiFetch, getErrorMessage } from '@/api/client';
import { escolherEEnviarAvatar } from '@/api/avatar';
import { MeResponse } from '@/api/types';
import { Avatar } from '@/design/Avatar';
import { Button } from '@/design/Button';
import { Card } from '@/design/Card';
import { ContaCard } from '@/design/ContaCard';

export default function PerfilInquilinoScreen() {
  const insets = useSafeAreaInsets();
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
      setError(getErrorMessage(e, 'Não foi possível carregar seu perfil'));
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
      setError(getErrorMessage(e, 'Não foi possível enviar a foto'));
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
    <ScrollView className="flex-1 bg-surface" contentContainerClassName="p-6 gap-4" contentContainerStyle={{ paddingTop: insets.top + 24 }}>
      <View className="flex-row items-center gap-4">
        <Button label="← Voltar" variant="outline" onPress={() => router.back()} />
        <Text className="text-primary" style={{ fontSize: 22, fontWeight: '800' }}>Meu perfil</Text>
      </View>

      {error ? <Card><Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text></Card> : null}

      <Card className="items-center gap-4" style={{ paddingVertical: 32 }}>
        <Avatar url={me?.avatarUrl} nome={me?.nome} size={96} />
        <Button label="Trocar foto" variant="outline" onPress={trocarFoto} loading={enviando} disabled={enviando} />
      </Card>

      {me ? <ContaCard me={me} onUpdated={setMe} /> : null}
    </ScrollView>
  );
}
