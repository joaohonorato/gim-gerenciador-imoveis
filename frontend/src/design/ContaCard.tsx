import { useState } from 'react';
import { Text, TextInput, View } from 'react-native';
import { apiFetch, getErrorMessage } from '@/api/client';
import { MeResponse } from '@/api/types';
import { Button } from './Button';
import { Card } from './Card';
import { colors } from './tokens';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

interface ContaCardProps {
  me: MeResponse;
  onUpdated: (me: MeResponse) => void;
}

// Nome + e-mail (com verificação/troca) do proprietário ou inquilino
// autenticado — mesmo componente pros dois perfis, já que a lógica e as
// rotas (/auth/me/nome, /auth/me/email, /auth/email/reenviar) não mudam
// conforme o tipo de conta (ver AuthController.buildMeResponse). Alterar
// e-mail não aplica na hora: fica pendente até o link enviado pro *novo*
// endereço ser confirmado — ver docs/especificacao-produto.md §5.
export function ContaCard({ me, onUpdated }: ContaCardProps) {
  const [editandoNome, setEditandoNome] = useState(false);
  const [nomeInput, setNomeInput] = useState(me.nome ?? '');
  const [salvandoNome, setSalvandoNome] = useState(false);

  const [novoEmailInput, setNovoEmailInput] = useState('');
  const [enviandoEmail, setEnviandoEmail] = useState(false);
  const [emailSolicitado, setEmailSolicitado] = useState(false);

  const [reenviandoVerificacao, setReenviandoVerificacao] = useState(false);
  const [verificacaoReenviada, setVerificacaoReenviada] = useState(false);

  const [error, setError] = useState('');

  function iniciarEdicaoNome() {
    setNomeInput(me.nome ?? '');
    setEditandoNome(true);
  }

  async function salvarNome() {
    const nome = nomeInput.trim();
    if (nome.length < 3) {
      setError('Nome deve ter pelo menos 3 caracteres.');
      return;
    }
    setSalvandoNome(true);
    setError('');
    try {
      const atualizado = await apiFetch<MeResponse>('/auth/me/nome', {
        method: 'PUT',
        body: JSON.stringify({ nome }),
      });
      onUpdated(atualizado);
      setEditandoNome(false);
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível salvar o nome'));
    } finally {
      setSalvandoNome(false);
    }
  }

  const novoEmail = novoEmailInput.trim();
  const novoEmailValido = EMAIL_PATTERN.test(novoEmail) && novoEmail.toLowerCase() !== (me.email ?? '').toLowerCase();

  async function solicitarAlteracaoEmail() {
    if (!novoEmailValido) {
      setError('Informe um e-mail válido, diferente do atual.');
      return;
    }
    setEnviandoEmail(true);
    setError('');
    setEmailSolicitado(false);
    try {
      await apiFetch<void>('/auth/me/email', {
        method: 'POST',
        body: JSON.stringify({ novoEmail }),
      });
      onUpdated({ ...me, emailPendente: novoEmail });
      setNovoEmailInput('');
      setEmailSolicitado(true);
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível solicitar a troca de e-mail'));
    } finally {
      setEnviandoEmail(false);
    }
  }

  async function reenviarVerificacao() {
    setReenviandoVerificacao(true);
    setError('');
    try {
      await apiFetch('/auth/email/reenviar', { method: 'POST' });
      setVerificacaoReenviada(true);
    } catch (e: any) {
      setError(getErrorMessage(e, 'Não foi possível reenviar o e-mail de verificação'));
    } finally {
      setReenviandoVerificacao(false);
    }
  }

  return (
    <Card className="gap-3">
      {error ? <Text style={{ color: '#DC2626', fontSize: 12.5 }}>{error}</Text> : null}

      <View className="flex-row items-center justify-between gap-4">
        <Text className="text-muted" style={{ fontSize: 14 }}>Nome</Text>
        {editandoNome ? null : (
          <View className="flex-row items-center gap-3">
            <Text className="text-primary text-right" style={{ fontSize: 14, fontWeight: '600' }}>{me.nome ?? '—'}</Text>
            <Text
              testID="link-editar-nome"
              onPress={iniciarEdicaoNome}
              accessibilityRole="button"
              style={{ color: colors.accent, fontSize: 13, fontWeight: '700' }}
            >
              Editar
            </Text>
          </View>
        )}
      </View>
      {editandoNome ? (
        <View className="gap-2">
          <TextInput
            testID="input-perfil-nome"
            placeholder="Nome"
            placeholderTextColor="#9CA3AF"
            value={nomeInput}
            onChangeText={setNomeInput}
            className="bg-card px-4 py-3 text-primary rounded-xl"
            style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
          />
          <View className="flex-row gap-3">
            <Button
              testID="btn-salvar-nome"
              label="Salvar"
              onPress={salvarNome}
              loading={salvandoNome}
              disabled={salvandoNome || nomeInput.trim() === (me.nome ?? '')}
            />
            <Button label="Cancelar" variant="outline" onPress={() => setEditandoNome(false)} disabled={salvandoNome} />
          </View>
        </View>
      ) : null}

      <View className="flex-row items-center justify-between gap-4">
        <Text className="text-muted" style={{ fontSize: 14 }}>E-mail</Text>
        <Text className="text-primary text-right" style={{ fontSize: 14, fontWeight: '600' }}>{me.email ?? '—'}</Text>
      </View>
      <View className="flex-row items-center justify-between gap-4">
        <Text className="text-muted" style={{ fontSize: 14 }}>Status do e-mail</Text>
        {me.emailVerificado ? (
          <Text style={{ color: '#16A34A', fontSize: 13, fontWeight: '700' }}>Verificado ✓</Text>
        ) : verificacaoReenviada ? (
          <Text style={{ color: '#6B7280', fontSize: 13, fontWeight: '600' }}>E-mail reenviado</Text>
        ) : (
          <Text
            testID="link-reenviar-verificacao"
            onPress={reenviandoVerificacao ? undefined : reenviarVerificacao}
            accessibilityRole="button"
            accessibilityLabel="Reenviar e-mail de verificação"
            style={{ color: '#D97706', fontSize: 13, fontWeight: '700' }}
          >
            {reenviandoVerificacao ? 'Enviando...' : 'Não verificado — reenviar'}
          </Text>
        )}
      </View>

      {me.emailPendente ? (
        <View className="gap-1" style={{ backgroundColor: '#FEF3C7', borderRadius: 10, padding: 12 }}>
          <Text style={{ color: '#92400E', fontSize: 12.5, fontWeight: '700' }}>Confirmação pendente</Text>
          <Text style={{ color: '#92400E', fontSize: 12.5 }}>
            Enviamos um link de confirmação para {me.emailPendente}. Seu e-mail de login só muda depois de confirmar.
          </Text>
        </View>
      ) : null}

      <View className="gap-2">
        <Text className="text-primary" style={{ fontSize: 13, fontWeight: '700' }}>Alterar e-mail</Text>
        <TextInput
          testID="input-novo-email"
          placeholder="novo@email.com"
          placeholderTextColor="#9CA3AF"
          value={novoEmailInput}
          onChangeText={(v) => { setNovoEmailInput(v); setEmailSolicitado(false); }}
          keyboardType="email-address"
          autoCapitalize="none"
          className="bg-card px-4 py-3 text-primary rounded-xl"
          style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
        />
        {emailSolicitado ? (
          <Text style={{ color: '#16A34A', fontSize: 12.5, fontWeight: '600' }}>Link de confirmação enviado.</Text>
        ) : null}
        <Button
          testID="btn-solicitar-alteracao-email"
          label="Enviar confirmação"
          onPress={solicitarAlteracaoEmail}
          loading={enviandoEmail}
          disabled={!novoEmailValido || enviandoEmail}
        />
      </View>
    </Card>
  );
}
