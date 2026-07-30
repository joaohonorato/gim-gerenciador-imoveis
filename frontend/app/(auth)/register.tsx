import { useState } from 'react';
import { View, Text, TextInput, KeyboardAvoidingView, Platform, ScrollView } from 'react-native';
import { router } from 'expo-router';
import { apiFetch } from '@/api/client';
import { session } from '@/api/session';
import { Button } from '@/design/Button';

export default function RegisterScreen() {
  const [username, setUsername] = useState('');
  const [cpfCnpj, setCpfCnpj] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const documentoDigits = cpfCnpj.replace(/\D/g, '');
  const senhaValida = senha.length >= 8 && /[A-Za-z]/.test(senha) && /\d/.test(senha);
  const emailValido = /\S+@\S+\.\S+/.test(email.trim());
  const documentoValido = validateCpfCnpj(documentoDigits);
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
          cpfCnpj: documentoDigits,
          email: email.trim(),
          senha,
        }),
        auth: false,
      });
      await session.set(res.sessionToken);
      router.replace('/imoveis');
    } catch (e: any) {
      setError(e.message ?? 'Falha ao cadastrar');
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
        <Text className="text-3xl font-bold text-primary mb-2">Criar conta de proprietário</Text>
        <Text className="text-muted mb-8">Convites ficam somente para inquilinos.</Text>

        <View className="gap-4">
          <TextInput
            testID="input-register-name"
            placeholder="Username"
            value={username}
            onChangeText={setUsername}
            className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
          />
          <TextInput
            testID="input-register-cpf"
            placeholder="CPF/CNPJ"
            value={cpfCnpj}
            onChangeText={onChangeCpfCnpj}
            keyboardType="numeric"
            className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
          />
          <Text className="text-muted text-sm">CPF/CNPJ válido (com dígitos verificadores).</Text>
          <TextInput
            testID="input-register-email"
            placeholder="E-mail"
            value={email}
            onChangeText={setEmail}
            keyboardType="email-address"
            autoCapitalize="none"
            className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
          />
          <TextInput
            testID="input-register-password"
            placeholder="Senha (mínimo 8 caracteres)"
            value={senha}
            onChangeText={setSenha}
            secureTextEntry
            className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
          />
          <Text className="text-muted text-sm">Use pelo menos 8 caracteres, com letra e número.</Text>
          {error ? <Text className="text-danger text-sm">{error}</Text> : null}
          <Button testID="btn-register-owner" label="Criar conta" onPress={register} loading={loading} disabled={!canSubmit} />
          <Button label="Voltar ao login" onPress={() => router.replace('/login')} variant="outline" />
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

function formatCpfCnpj(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, 14);

  if (digits.length <= 11) {
    return digits
      .replace(/^(\d{3})(\d)/, '$1.$2')
      .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
      .replace(/\.(\d{3})(\d)/, '.$1-$2');
  }

  return digits
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1/$2')
    .replace(/(\/\d{4})(\d)/, '$1-$2');
}

function validateCpfCnpj(digits: string): boolean {
  if (digits.length === 11) return validateCpf(digits);
  if (digits.length === 14) return validateCnpj(digits);
  return false;
}

function validateCpf(digits: string): boolean {
  if (/^(\d)\1{10}$/.test(digits)) return false;

  const d1 = calcCpfDigit(digits, 10);
  const d2 = calcCpfDigit(digits, 11);

  return d1 === Number(digits[9]) && d2 === Number(digits[10]);
}

function calcCpfDigit(digits: string, startWeight: number): number {
  let sum = 0;
  for (let i = 0; i < startWeight - 1; i += 1) {
    sum += Number(digits[i]) * (startWeight - i);
  }
  const mod = (sum * 10) % 11;
  return mod === 10 ? 0 : mod;
}

function validateCnpj(digits: string): boolean {
  if (/^(\d)\1{13}$/.test(digits)) return false;

  const w1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  const w2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

  const d1 = calcCnpjDigit(digits, w1);
  const d2 = calcCnpjDigit(digits, w2);

  return d1 === Number(digits[12]) && d2 === Number(digits[13]);
}

function calcCnpjDigit(digits: string, weights: number[]): number {
  let sum = 0;
  for (let i = 0; i < weights.length; i += 1) {
    sum += Number(digits[i]) * weights[i];
  }
  const mod = sum % 11;
  return mod < 2 ? 0 : 11 - mod;
}
