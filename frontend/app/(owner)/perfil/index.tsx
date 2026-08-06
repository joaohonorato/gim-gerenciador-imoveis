import { useCallback, useState } from 'react';
import { ActivityIndicator, ScrollView, Text, TextInput, View } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiFetch, getErrorMessage } from '@/api/client';
import { escolherEEnviarAvatar } from '@/api/avatar';
import { MeResponse } from '@/api/types';
import { Avatar } from '@/design/Avatar';
import { Button } from '@/design/Button';
import { Card } from '@/design/Card';
import { ContaCard } from '@/design/ContaCard';
import { PasswordChecklist } from '@/design/PasswordChecklist';
import { PasswordInput } from '@/design/PasswordInput';
import { formatCpfCnpj, validateCpfCnpj } from '@/utils/cpfCnpj';
import { isPasswordValid } from '@/utils/password';

export default function PerfilProprietarioScreen() {
  const insets = useSafeAreaInsets();
  const [me, setMe] = useState<MeResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [enviando, setEnviando] = useState(false);
  const [cpfCnpjInput, setCpfCnpjInput] = useState('');
  const [salvandoCpfCnpj, setSalvandoCpfCnpj] = useState(false);
  const [telefoneInput, setTelefoneInput] = useState('');
  const [salvandoTelefone, setSalvandoTelefone] = useState(false);
  const [senhaAtualInput, setSenhaAtualInput] = useState('');
  const [novaSenhaInput, setNovaSenhaInput] = useState('');
  const [confirmarSenhaInput, setConfirmarSenhaInput] = useState('');
  const [trocandoSenha, setTrocandoSenha] = useState(false);
  const [senhaTrocada, setSenhaTrocada] = useState(false);
  const [cpfCnpjSalvo, setCpfCnpjSalvo] = useState(false);
  const [telefoneSalvo, setTelefoneSalvo] = useState(false);
  const [error, setError] = useState('');

  const carregar = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const resultado = await apiFetch<MeResponse>('/auth/me');
      setMe(resultado);
      setCpfCnpjInput(resultado.cpfCnpj ? formatCpfCnpj(resultado.cpfCnpj) : '');
      setTelefoneInput(resultado.telefone ? formatTelefone(resultado.telefone) : '');
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

  const cpfCnpjDigits = cpfCnpjInput.replace(/\D/g, '');
  const cpfCnpjValido = validateCpfCnpj(cpfCnpjDigits);

  async function salvarCpfCnpj() {
    if (!cpfCnpjValido) {
      setError('CPF/CNPJ inválido.');
      return;
    }
    setSalvandoCpfCnpj(true);
    setError('');
    setCpfCnpjSalvo(false);
    try {
      const atualizado = await apiFetch<MeResponse>('/auth/me/cpf-cnpj', {
        method: 'PUT',
        body: JSON.stringify({ cpfCnpj: cpfCnpjDigits }),
      });
      setMe(atualizado);
      setCpfCnpjInput(atualizado.cpfCnpj ? formatCpfCnpj(atualizado.cpfCnpj) : '');
      setCpfCnpjSalvo(true);
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível salvar o CPF/CNPJ'));
    } finally {
      setSalvandoCpfCnpj(false);
    }
  }

  const telefoneDigits = telefoneInput.replace(/\D/g, '');
  const telefoneValido = telefoneDigits.length === 10 || telefoneDigits.length === 11;

  async function salvarTelefone() {
    if (!telefoneValido) {
      setError('Telefone inválido — use DDD + número (10 ou 11 dígitos).');
      return;
    }
    setSalvandoTelefone(true);
    setError('');
    setTelefoneSalvo(false);
    try {
      const atualizado = await apiFetch<MeResponse>('/auth/me/telefone', {
        method: 'PUT',
        body: JSON.stringify({ telefone: telefoneDigits }),
      });
      setMe(atualizado);
      setTelefoneInput(atualizado.telefone ? formatTelefone(atualizado.telefone) : '');
      setTelefoneSalvo(true);
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível salvar o telefone'));
    } finally {
      setSalvandoTelefone(false);
    }
  }

  const novaSenhaValida = isPasswordValid(novaSenhaInput);
  const senhasConferem = novaSenhaInput === confirmarSenhaInput;
  const trocaSenhaHabilitada = senhaAtualInput.length > 0 && novaSenhaValida && senhasConferem;

  async function trocarSenha() {
    if (!trocaSenhaHabilitada) {
      setError('Preencha a senha atual e uma nova senha (mín. 8 caracteres, com letra e número) igual nos dois campos.');
      return;
    }
    setTrocandoSenha(true);
    setError('');
    setSenhaTrocada(false);
    try {
      await apiFetch<void>('/auth/senha/trocar', {
        method: 'POST',
        body: JSON.stringify({ senhaAtual: senhaAtualInput, novaSenha: novaSenhaInput }),
        // Senha atual errada também devolve 401/AUTH_INVALID, mas a sessão
        // continua válida — sem isso, digitar a senha atual errada aqui
        // derrubava o usuário pro login sem explicação (ver useLogout.ts e
        // client.ts para o comportamento padrão de 401).
        preserveSessionOn401: true,
      });
      setSenhaAtualInput('');
      setNovaSenhaInput('');
      setConfirmarSenhaInput('');
      setSenhaTrocada(true);
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível trocar a senha'));
    } finally {
      setTrocandoSenha(false);
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

      <Card className="gap-3">
        <Text className="text-primary" style={{ fontSize: 15, fontWeight: '700' }}>Cadastro completo</Text>
        <Text className="text-muted" style={{ fontSize: 12.5 }}>
          {me?.cpfCnpj
            ? 'CPF/CNPJ cadastrado. Você pode corrigir aqui se necessário.'
            : 'Necessário só para assinar contratos — pode ficar em branco até lá.'}
        </Text>
        <TextInput
          testID="input-perfil-cpf-cnpj"
          placeholder="CPF/CNPJ"
          placeholderTextColor="#9CA3AF"
          value={cpfCnpjInput}
          onChangeText={(v) => { setCpfCnpjInput(formatCpfCnpj(v)); setCpfCnpjSalvo(false); }}
          keyboardType="numeric"
          className="bg-card px-4 py-3 text-primary rounded-xl"
          style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
        />
        {cpfCnpjSalvo ? <Text style={{ color: '#16A34A', fontSize: 12.5, fontWeight: '600' }}>CPF/CNPJ salvo com sucesso.</Text> : null}
        <Button
          testID="btn-salvar-cpf-cnpj"
          label={me?.cpfCnpj ? 'Atualizar' : 'Salvar'}
          onPress={salvarCpfCnpj}
          loading={salvandoCpfCnpj}
          disabled={!cpfCnpjValido || salvandoCpfCnpj || cpfCnpjDigits === (me?.cpfCnpj ?? '')}
        />
      </Card>

      <Card className="gap-3">
        <Text className="text-primary" style={{ fontSize: 15, fontWeight: '700' }}>Contato</Text>
        <Text className="text-muted" style={{ fontSize: 12.5 }}>
          Telefone com DDD. CRECI e razão social chegam em breve nesta seção.
        </Text>
        <TextInput
          testID="input-perfil-telefone"
          placeholder="Telefone"
          placeholderTextColor="#9CA3AF"
          value={telefoneInput}
          onChangeText={(v) => { setTelefoneInput(formatTelefone(v)); setTelefoneSalvo(false); }}
          keyboardType="numeric"
          className="bg-card px-4 py-3 text-primary rounded-xl"
          style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
        />
        {telefoneSalvo ? <Text style={{ color: '#16A34A', fontSize: 12.5, fontWeight: '600' }}>Telefone salvo com sucesso.</Text> : null}
        <Button
          testID="btn-salvar-telefone"
          label={me?.telefone ? 'Atualizar' : 'Salvar'}
          onPress={salvarTelefone}
          loading={salvandoTelefone}
          disabled={!telefoneValido || salvandoTelefone || telefoneDigits === (me?.telefone ?? '')}
        />
      </Card>

      <Card className="gap-3">
        <Text className="text-primary" style={{ fontSize: 15, fontWeight: '700' }}>Senha</Text>
        <Text className="text-muted" style={{ fontSize: 12.5 }}>
          Troque sua senha informando a atual — se você esqueceu a senha, saia e use "Esqueci minha senha" na tela de login.
        </Text>
        <PasswordInput
          testID="input-senha-atual"
          placeholder="Senha atual"
          value={senhaAtualInput}
          onChangeText={(v) => { setSenhaAtualInput(v); setSenhaTrocada(false); }}
          textContentType="password"
          autoComplete="current-password"
        />
        <PasswordInput
          testID="input-nova-senha"
          placeholder="Nova senha (mín. 8 caracteres)"
          value={novaSenhaInput}
          onChangeText={(v) => { setNovaSenhaInput(v); setSenhaTrocada(false); }}
          textContentType="newPassword"
          autoComplete="new-password"
          invalid={novaSenhaInput.length > 0 && !novaSenhaValida}
        />
        <PasswordChecklist value={novaSenhaInput} />
        <PasswordInput
          testID="input-confirmar-senha"
          placeholder="Confirmar nova senha"
          value={confirmarSenhaInput}
          onChangeText={(v) => { setConfirmarSenhaInput(v); setSenhaTrocada(false); }}
          textContentType="newPassword"
          autoComplete="new-password"
          invalid={confirmarSenhaInput.length > 0 && !senhasConferem}
        />
        {novaSenhaInput.length > 0 && !senhasConferem ? (
          <Text style={{ color: '#DC2626', fontSize: 12.5 }}>As senhas não conferem.</Text>
        ) : null}
        {senhaTrocada ? (
          <Text style={{ color: '#16A34A', fontSize: 12.5, fontWeight: '600' }}>Senha alterada com sucesso.</Text>
        ) : null}
        <Button
          testID="btn-trocar-senha"
          label="Trocar senha"
          onPress={trocarSenha}
          loading={trocandoSenha}
          disabled={!trocaSenhaHabilitada || trocandoSenha}
        />
      </Card>
    </ScrollView>
  );
}

function formatTelefone(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 11);
  if (digits.length <= 2) return digits;
  if (digits.length <= 6) return `(${digits.slice(0, 2)}) ${digits.slice(2)}`;
  if (digits.length <= 10) return `(${digits.slice(0, 2)}) ${digits.slice(2, 6)}-${digits.slice(6)}`;
  return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
}
