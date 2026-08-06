import { useState } from 'react';
import { View, Text, TextInput, KeyboardAvoidingView, Platform, ScrollView } from 'react-native';
import { router } from 'expo-router';
import { apiFetch, getErrorMessage } from '@/api/client';
import { session } from '@/api/session';
import { Button } from '@/design/Button';
import { PasswordChecklist } from '@/design/PasswordChecklist';
import { PasswordInput } from '@/design/PasswordInput';
import { formatCpfCnpj, validateCpfCnpj } from '@/utils/cpfCnpj';
import { isPasswordValid } from '@/utils/password';

export default function RegisterScreen() {
  const [username, setUsername] = useState('');
  const [cpfCnpj, setCpfCnpj] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const documentoDigits = cpfCnpj.replace(/\D/g, '');
  const senhaValida = isPasswordValid(senha);
  const emailValido = /\S+@\S+\.\S+/.test(email.trim());
  // CPF/CNPJ é opcional no cadastro básico — se preenchido, precisa ser válido;
  // vazio é permitido (fica pendente até assinar o primeiro contrato).
  const documentoValido = documentoDigits.length === 0 || validateCpfCnpj(documentoDigits);
  const usernameValido = username.trim().length >= 3;
  const canSubmit = usernameValido && documentoValido && emailValido && senhaValida && !loading;

  async function register() {
    if (!canSubmit) {
      setError('Revise username, CPF/CNPJ, e-mail e senha antes de continuar.');
      return;
    }

    setLoading(true);
    setError('');
    try {
      const res = await apiFetch<{ sessionToken: string }>('/auth/register/proprietario', {
        method: 'POST',
        body: JSON.stringify({
          username: username.trim(),
          cpfCnpj: documentoDigits || undefined,
          email: email.trim(),
          senha,
        }),
        auth: false,
      });
      await session.set(res.sessionToken);
      router.replace('/imoveis');
    } catch (e: any) {
      setError(getErrorMessage(e, 'Falha ao cadastrar'));
    } finally {
      setLoading(false);
    }
  }

  function onChangeCpfCnpj(value: string) {
    setCpfCnpj(formatCpfCnpj(value));
  }

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} className="flex-1 bg-surface">
      <ScrollView contentContainerClassName="flex-1 justify-center p-6">
        <Text className="text-primary mb-1" style={{ fontSize: 21, fontWeight: '800' }}>Criar conta de proprietário</Text>
        <Text className="text-muted mb-2" style={{ fontSize: 13.5 }}>Convites ficam somente para inquilinos.</Text>

        <View className="gap-3">
          <TextInput
            testID="input-register-name"
            placeholder="Username"
            placeholderTextColor="#9CA3AF"
            value={username}
            onChangeText={setUsername}
            className="bg-card px-4 py-3 text-primary rounded-xl"
            style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
          />
          <TextInput
            testID="input-register-cpf"
            placeholder="CPF/CNPJ (opcional)"
            placeholderTextColor="#9CA3AF"
            value={cpfCnpj}
            onChangeText={onChangeCpfCnpj}
            keyboardType="numeric"
            className="bg-card px-4 py-3 text-primary rounded-xl"
            style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
          />
          <Text className="text-muted" style={{ fontSize: 12.5 }}>
            Opcional agora — você pode completar depois no seu perfil. Necessário só para assinar contratos.
          </Text>
          <TextInput
            testID="input-register-email"
            placeholder="E-mail"
            placeholderTextColor="#9CA3AF"
            value={email}
            onChangeText={setEmail}
            keyboardType="email-address"
            autoCapitalize="none"
            textContentType="emailAddress"
            autoComplete="email"
            className="bg-card px-4 py-3 text-primary rounded-xl"
            style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
          />
          <PasswordInput
            testID="input-register-password"
            placeholder="Senha (mínimo 8 caracteres)"
            value={senha}
            onChangeText={setSenha}
            textContentType="newPassword"
            autoComplete="new-password"
            invalid={senha.length > 0 && !senhaValida}
          />
          <PasswordChecklist value={senha} />
          {error ? <Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text> : null}
          <Button testID="btn-register-owner" label="Criar conta" onPress={register} loading={loading} disabled={!canSubmit} />
          <Button label="Voltar ao login" onPress={() => router.replace('/login')} variant="outline" />
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
