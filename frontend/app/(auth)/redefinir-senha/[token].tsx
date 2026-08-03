import { useState } from 'react';
import { View, Text, TextInput, KeyboardAvoidingView, Platform, ScrollView } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { apiFetch } from '@/api/client';
import { Button } from '@/design/Button';

export default function RedefinirSenhaScreen() {
  const { token } = useLocalSearchParams<{ token: string }>();
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmarSenha, setConfirmarSenha] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [sucesso, setSucesso] = useState(false);

  async function redefinir() {
    if (novaSenha.length < 8) {
      setError('A senha precisa ter pelo menos 8 caracteres.');
      return;
    }
    if (novaSenha !== confirmarSenha) {
      setError('As senhas não coincidem.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await apiFetch(`/auth/senha/redefinir`, {
        method: 'POST',
        body: JSON.stringify({ token, novaSenha }),
        auth: false,
      });
      setSucesso(true);
    } catch (e: any) {
      setError(e.message ?? 'Não foi possível redefinir a senha — o link pode ter expirado.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      className="flex-1"
      style={{ backgroundColor: '#FFFFFF' }}
    >
      <ScrollView contentContainerClassName="flex-grow" contentContainerStyle={{ justifyContent: 'center' }}>
        <View className="px-6" style={{ width: '100%', maxWidth: 420, alignSelf: 'center' }}>
          <Text className="mb-1 text-primary" style={{ fontSize: 22, fontWeight: '700' }}>Redefinir senha</Text>
          <Text className="mb-8 text-muted" style={{ fontSize: 14 }}>Escolha uma nova senha para sua conta.</Text>

          {sucesso ? (
            <View className="gap-4">
              <Text className="text-primary" style={{ fontSize: 14 }}>Senha redefinida com sucesso.</Text>
              <Button testID="btn-ir-login" label="Ir para o login" onPress={() => router.replace('/login')} />
            </View>
          ) : (
            <View className="gap-4">
              <TextInput
                testID="input-nova-senha"
                placeholder="Nova senha"
                placeholderTextColor="#9CA3AF"
                value={novaSenha}
                onChangeText={setNovaSenha}
                secureTextEntry
                className="bg-card px-4 py-[13px] text-primary rounded-xl"
                style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 15 }}
              />
              <TextInput
                testID="input-confirmar-senha"
                placeholder="Confirmar nova senha"
                placeholderTextColor="#9CA3AF"
                value={confirmarSenha}
                onChangeText={setConfirmarSenha}
                secureTextEntry
                className="bg-card px-4 py-[13px] text-primary rounded-xl"
                style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 15 }}
              />
              {error ? <Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text> : null}
              <Button testID="btn-redefinir" label="Redefinir senha" onPress={redefinir} loading={loading} />
            </View>
          )}
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
