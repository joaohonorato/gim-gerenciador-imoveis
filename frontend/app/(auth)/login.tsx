import { useState } from 'react';
import { View, Text, TextInput, KeyboardAvoidingView, Platform, ScrollView, useWindowDimensions } from 'react-native';
import { router } from 'expo-router';
import { apiFetch } from '@/api/client';
import { session } from '@/api/session';
import { Button } from '@/design/Button';
import { TipoConta } from '@/api/types';

export default function LoginScreen() {
  const { width } = useWindowDimensions();
  const isDesktop = width >= 960;
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function login() {
    setLoading(true);
    setError('');
    try {
      const res = await apiFetch<{ sessionToken: string; tipoConta: TipoConta }>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, senha }),
        auth: false,
      });
      await session.set(res.sessionToken);
      router.replace(res.tipoConta === 'INQUILINO' ? '/tenant' : '/imoveis');
    } catch (e: any) {
      setError(e.message ?? 'Falha ao entrar');
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
      <ScrollView contentContainerClassName="flex-grow">
        <View style={{ flex: 1, flexDirection: isDesktop ? 'row' : 'column' }}>
          <View
            className="px-6"
            style={{
              backgroundColor: '#2563EB',
              paddingTop: isDesktop ? 72 : 56,
              paddingBottom: isDesktop ? 72 : 44,
              justifyContent: 'center',
              minHeight: isDesktop ? '100%' : undefined,
              flexBasis: isDesktop ? '46%' : undefined,
            }}
          >
            <View className="mb-7" style={{ width: 40, height: 40, borderRadius: 10, backgroundColor: '#FFFFFF' }} />
            <Text className="mb-4 text-white" style={{ fontSize: 32, fontWeight: '800', lineHeight: 38, maxWidth: 420 }}>
              Gestão de Imóveis
            </Text>
            <Text style={{ color: 'rgba(255,255,255,0.86)', fontSize: 16, lineHeight: 24, maxWidth: 420 }}>
              Cadastre imóveis, acompanhe contratos e assinaturas em um só lugar.
            </Text>
          </View>

          <View
            className="px-6"
            style={{
              backgroundColor: '#FFFFFF',
              justifyContent: 'center',
              paddingTop: isDesktop ? 56 : 32,
              paddingBottom: 40,
              flexBasis: isDesktop ? '54%' : undefined,
            }}
          >
            <View style={{ width: '100%', maxWidth: 420, alignSelf: 'center' }}>
              <Text className="mb-1 text-primary" style={{ fontSize: 22, fontWeight: '700' }}>Acessar conta</Text>
              <Text className="mb-8 text-muted" style={{ fontSize: 14 }}>Entre com e-mail e senha.</Text>

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
                <TextInput
                  testID="input-password"
                  placeholder="Senha"
                  placeholderTextColor="#9CA3AF"
                  value={senha}
                  onChangeText={setSenha}
                  secureTextEntry
                  className="bg-card px-4 py-[13px] text-primary rounded-xl"
                  style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 15 }}
                />
                {error ? <Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text> : null}
                <Button testID="btn-login" label="Entrar" onPress={login} loading={loading} />
                <Text
                  testID="link-esqueci-senha"
                  onPress={() => router.push('/esqueci-senha')}
                  style={{ color: '#2563EB', fontSize: 13, fontWeight: '600', textAlign: 'center' }}
                >
                  Esqueci minha senha
                </Text>
                <Button label="Sou proprietário, criar conta" onPress={() => router.push('/register')} variant="outline" />
                <Button label="Tenho convite de inquilino" onPress={() => router.push('/convite/manual')} variant="outline" />
              </View>
            </View>
          </View>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
