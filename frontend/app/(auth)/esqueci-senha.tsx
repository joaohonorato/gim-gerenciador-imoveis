import { useState } from 'react';
import { View, Text, TextInput, KeyboardAvoidingView, Platform, ScrollView } from 'react-native';
import { router } from 'expo-router';
import { apiFetch } from '@/api/client';
import { Button } from '@/design/Button';

export default function EsqueciSenhaScreen() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [enviado, setEnviado] = useState(false);

  async function enviar() {
    if (!email.trim()) {
      setError('Informe seu e-mail.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await apiFetch('/auth/senha/esqueci', {
        method: 'POST',
        body: JSON.stringify({ email: email.trim() }),
        auth: false,
      });
      setEnviado(true);
    } catch (e: any) {
      setError(e.message ?? 'Não foi possível enviar o e-mail');
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
          <Text className="mb-1 text-primary" style={{ fontSize: 22, fontWeight: '700' }}>Esqueci minha senha</Text>
          <Text className="mb-8 text-muted" style={{ fontSize: 14 }}>
            Informe o e-mail da sua conta — se ele existir, enviamos um link de redefinição.
          </Text>

          {enviado ? (
            <View className="gap-4">
              <Text className="text-primary" style={{ fontSize: 14 }}>
                Se {email.trim()} tiver uma conta, um link de redefinição foi enviado. Confira sua caixa de entrada.
              </Text>
              <Button testID="btn-voltar-login" label="Voltar para o login" onPress={() => router.replace('/login')} />
            </View>
          ) : (
            <View className="gap-4">
              <TextInput
                testID="input-email"
                placeholder="E-mail"
                placeholderTextColor="#9CA3AF"
                value={email}
                onChangeText={setEmail}
                keyboardType="email-address"
                autoCapitalize="none"
                className="bg-card px-4 py-[13px] text-primary rounded-xl"
                style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 15 }}
              />
              {error ? <Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text> : null}
              <Button testID="btn-enviar" label="Enviar link de redefinição" onPress={enviar} loading={loading} />
              <Button label="Voltar para o login" variant="outline" onPress={() => router.replace('/login')} />
            </View>
          )}
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
