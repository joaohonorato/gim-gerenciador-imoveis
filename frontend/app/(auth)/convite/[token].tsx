import { useEffect, useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Text, TextInput, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { apiFetch } from '@/api/client';
import { session } from '@/api/session';
import { Button } from '@/design/Button';

type InviteResponse = {
  token: string;
  email: string;
  nome: string;
  documento: string;
  consumido: boolean;
};

export default function ConviteScreen() {
  const { token } = useLocalSearchParams<{ token: string }>();
  const [invite, setInvite] = useState<InviteResponse | null>(null);
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadingInvite, setLoadingInvite] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    async function loadInvite() {
      try {
        const data = await apiFetch<InviteResponse>(`/auth/convites/${token}`, { auth: false });
        if (!active) return;
        setInvite(data);
        setEmail(data.email);
      } catch (e: any) {
        if (!active) return;
        setError(e.message ?? 'Não foi possível carregar o convite');
      } finally {
        if (active) setLoadingInvite(false);
      }
    }

    if (token) {
      void loadInvite();
    }

    return () => {
      active = false;
    };
  }, [token]);

  async function aceitarConvite() {
    setLoading(true);
    setError('');
    try {
      const res = await apiFetch<{ sessionToken: string }>(`/auth/convites/${token}/aceitar`, {
        method: 'POST',
        body: JSON.stringify({ email, senha }),
        auth: false,
      });
      await session.set(res.sessionToken);
      router.replace('/imoveis');
    } catch (e: any) {
      setError(e.message ?? 'Não foi possível concluir o cadastro');
    } finally {
      setLoading(false);
    }
  }

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} className="flex-1 bg-surface">
      <ScrollView contentContainerClassName="flex-1 justify-center p-6">
        <Text className="text-3xl font-bold text-primary mb-2">Convite da Plataforma</Text>
        <Text className="text-muted mb-8">Finalize seu acesso com e-mail e senha.</Text>

        {loadingInvite ? (
          <Text className="text-muted">Carregando convite...</Text>
        ) : (
          <View className="gap-4">
            {invite ? (
              <View className="gap-2 border-2 border-border rounded px-4 py-4 bg-card">
                <Text className="text-primary font-semibold">{invite.nome}</Text>
                <Text className="text-muted">{invite.email}</Text>
                <Text className="text-muted">Documento: {invite.documento}</Text>
              </View>
            ) : null}
            <TextInput
              testID="input-invite-email"
              placeholder="E-mail"
              value={email}
              onChangeText={setEmail}
              autoCapitalize="none"
              keyboardType="email-address"
              className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
            />
            <TextInput
              testID="input-invite-password"
              placeholder="Crie sua senha"
              value={senha}
              onChangeText={setSenha}
              secureTextEntry
              className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
            />
            {error ? <Text className="text-danger text-sm">{error}</Text> : null}
            <Button testID="btn-accept-invite" label="Concluir cadastro" onPress={aceitarConvite} loading={loading} />
            <Button label="Voltar ao login" onPress={() => router.replace('/login')} variant="outline" />
          </View>
        )}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}