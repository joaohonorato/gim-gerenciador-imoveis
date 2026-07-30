import { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, ScrollView, Text, TextInput, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { apiFetch } from '@/api/client';
import { Button } from '@/design/Button';
import { Convite, ConviteInquilino, MeResponse } from '@/api/types';

type GarantiaTipo = 'CAUCAO' | 'FIADOR' | 'SEGURO_FIANCA' | 'TITULO_CAPITALIZACAO';

export default function ConviteLocacaoScreen() {
  const { token } = useLocalSearchParams<{ token: string }>();
  const [convite, setConvite] = useState<Convite | null>(null);
  const [me, setMe] = useState<MeResponse | null>(null);
  const [conviteVinculado, setConviteVinculado] = useState<ConviteInquilino | null>(null);
  const [loadingInvite, setLoadingInvite] = useState(true);
  const [loadingAction, setLoadingAction] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [username, setUsername] = useState('');
  const [cpf, setCpf] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');

  const [garantiaTipo, setGarantiaTipo] = useState<GarantiaTipo>('CAUCAO');
  const [garantiaDados, setGarantiaDados] = useState('{"valor":0}');

  useEffect(() => {
    let active = true;

    async function load() {
      setLoadingInvite(true);
      setError('');
      try {
        const conviteData = await apiFetch<Convite>(`/convites/${token}`, { auth: false });
        if (!active) return;
        setConvite(conviteData);

        try {
          const meData = await apiFetch<MeResponse>('/auth/me');
          if (!active) return;
          setMe(meData);

          if (meData.tipoConta === 'INQUILINO') {
            const convites = await apiFetch<ConviteInquilino[]>('/convites/me');
            if (!active) return;
            const vinculado = convites.find((item) => item.token === token) ?? null;
            setConviteVinculado(vinculado);
          }
        } catch {
          if (!active) return;
          setMe(null);
        }
      } catch (e: any) {
        if (!active) return;
        setError(e.message ?? 'Não foi possível carregar o convite de locação');
      } finally {
        if (active) setLoadingInvite(false);
      }
    }

    if (token) {
      void load();
    }

    return () => {
      active = false;
    };
  }, [token]);

  const podeAceitarComConta = useMemo(() => {
    return convite?.status === 'PENDENTE' && me?.tipoConta === 'INQUILINO' && !conviteVinculado;
  }, [convite?.status, me?.tipoConta, conviteVinculado]);

  const precisaCadastro = useMemo(() => {
    return convite?.status === 'PENDENTE' && !me && !conviteVinculado;
  }, [convite?.status, me, conviteVinculado]);

  const precisaGarantia = useMemo(() => {
    const exigeGarantia = conviteVinculado?.garantiaAceita != null && conviteVinculado.garantiaAceita !== 'NENHUMA';
    if (conviteVinculado && exigeGarantia && conviteVinculado.garantiaEscolhida == null) return true;
    return false;
  }, [conviteVinculado]);

  const contratoPendenteAssinatura = useMemo(() => {
    if (!conviteVinculado) return null;
    if (conviteVinculado.statusAssinaturaContrato !== 'PENDENTE') return null;
    return conviteVinculado.contratoId;
  }, [conviteVinculado]);

  async function aceitarComContaExistente() {
    setLoadingAction(true);
    setError('');
    setSuccess('');
    try {
      await apiFetch(`/convites/${token}/aceitar-vinculo`, { method: 'POST' });
      const convites = await apiFetch<ConviteInquilino[]>('/convites/me');
      const vinculado = convites.find((item) => item.token === token) ?? null;
      setConviteVinculado(vinculado);
      const semGarantia = vinculado?.garantiaAceita == null || vinculado?.garantiaAceita === 'NENHUMA';
      setSuccess(semGarantia
        ? 'Convite aceito na sua conta. Agora aguarde a análise do proprietário.'
        : 'Convite aceito na sua conta. Agora envie a garantia para seguir.');
    } catch (e: any) {
      setError(e.message ?? 'Não foi possível aceitar o convite com sua conta atual');
    } finally {
      setLoadingAction(false);
    }
  }

  async function cadastrarPorConvite() {
    if (!username.trim() || !cpf.trim() || !email.trim() || !senha.trim()) {
      setError('Preencha username, CPF, e-mail e senha para continuar.');
      return;
    }

    setLoadingAction(true);
    setError('');
    setSuccess('');
    try {
      await apiFetch(`/convites/${token}/cadastro`, {
        method: 'POST',
        auth: false,
        body: JSON.stringify({ username: username.trim(), cpf: cpf.trim(), email: email.trim(), senha }),
      });
      const semGarantia = convite?.garantiaAceita == null || convite?.garantiaAceita === 'NENHUMA';
      setSuccess(semGarantia
        ? 'Cadastro concluído. Agora aguarde a análise do proprietário.'
        : 'Cadastro concluído. Envie a garantia para seguir com a análise.');
      setConviteVinculado({
        conviteId: convite?.id ?? token,
        token: token ?? '',
        imovelId: convite?.imovelId ?? '',
        unidadeId: convite?.unidadeId ?? '',
        proprietarioId: convite?.proprietarioId ?? '',
        conviteStatus: convite?.status ?? 'EM_ANALISE',
        candidaturaId: '',
        candidaturaStatus: 'PENDENTE',
        tipoContrato: convite?.tipoContrato ?? 'RESIDENCIAL',
        valorAluguel: convite?.valorAluguel ?? 0,
        dataInicio: convite?.dataInicio ?? '',
        dataFim: convite?.dataFim ?? '',
        garantiaAceita: convite?.garantiaAceita ?? null,
        garantiaEscolhida: null,
        criadaEm: new Date().toISOString(),
        contratoId: null,
        statusAssinaturaContrato: null,
      });
    } catch (e: any) {
      setError(e.message ?? 'Não foi possível concluir o cadastro por convite');
    } finally {
      setLoadingAction(false);
    }
  }

  async function enviarGarantia() {
    setLoadingAction(true);
    setError('');
    setSuccess('');
    try {
      await apiFetch(`/convites/${token}/garantia`, {
        method: 'POST',
        auth: false,
        body: JSON.stringify({ tipo: garantiaTipo, dadosEspecificos: garantiaDados || '{}' }),
      });
      setSuccess('Garantia enviada com sucesso. Aguarde a análise do proprietário.');
      if (me?.tipoConta === 'INQUILINO') {
        const convites = await apiFetch<ConviteInquilino[]>('/convites/me');
        const vinculado = convites.find((item) => item.token === token) ?? null;
        setConviteVinculado(vinculado);
      } else {
        setConviteVinculado((atual) => (atual ? { ...atual, garantiaEscolhida: garantiaTipo } : atual));
      }
    } catch (e: any) {
      setError(e.message ?? 'Não foi possível enviar a garantia');
    } finally {
      setLoadingAction(false);
    }
  }

  if (loadingInvite) {
    return (
      <View className="flex-1 items-center justify-center bg-surface gap-3">
        <ActivityIndicator color="#2563EB" />
        <Text className="text-muted">Carregando convite...</Text>
      </View>
    );
  }

  return (
    <ScrollView className="flex-1 bg-surface" contentContainerClassName="p-6">
      <View className="w-full self-center bg-card border-2 border-border rounded p-6" style={{ maxWidth: 620 }}>
        <Text className="text-primary mb-2" style={{ fontSize: 24, fontWeight: '800' }}>
          Convite de locação
        </Text>
        <Text className="text-muted mb-5">
          Este convite vincula proprietário, imóvel e inquilino em uma nova relação de locação.
        </Text>

        {convite ? (
          <View className="mb-5 gap-2 border-2 border-border rounded px-4 py-4 bg-surface">
            <Text className="text-primary" style={{ fontWeight: '700' }}>Tipo: {formatTipo(convite.tipoContrato)}</Text>
            <Text className="text-muted">Imóvel: {convite.imovelId}</Text>
            <Text className="text-muted">Proprietário: {convite.proprietarioId}</Text>
            <Text className="text-muted">Período: {formatDate(convite.dataInicio)} até {formatDate(convite.dataFim)}</Text>
            <Text className="text-muted">Aluguel: {formatCurrency(convite.valorAluguel)}</Text>
            <Text className="text-muted">Garantia aceita: {convite.garantiaAceita == null ? 'Nenhuma' : formatTipo(convite.garantiaAceita)}</Text>
          </View>
        ) : null}

        {error ? <Text className="mb-3" style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text> : null}
        {success ? <Text className="mb-3" style={{ color: '#16A34A', fontSize: 13 }}>{success}</Text> : null}

        {podeAceitarComConta ? (
          <View className="gap-3 mb-4">
            <Text className="text-primary" style={{ fontWeight: '700' }}>
              Conta encontrada: {me?.email}
            </Text>
            <Text className="text-muted">Você já está autenticado como inquilino. Aceite para vincular este convite à sua conta.</Text>
            <Button label="Aceitar convite com minha conta" onPress={aceitarComContaExistente} loading={loadingAction} />
          </View>
        ) : null}

        {precisaCadastro ? (
          <View className="gap-3 mb-4">
            <Text className="text-primary" style={{ fontWeight: '700' }}>
              Novo inquilino
            </Text>
            <Text className="text-muted">Se você ainda não tem conta, faça o cadastro com senha.</Text>
            <TextInput
              placeholder="Username"
              value={username}
              onChangeText={setUsername}
              className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
            />
            <TextInput
              placeholder="CPF"
              value={cpf}
              onChangeText={setCpf}
              keyboardType="numeric"
              className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
            />
            <TextInput
              placeholder="E-mail"
              value={email}
              onChangeText={setEmail}
              keyboardType="email-address"
              autoCapitalize="none"
              className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
            />
            <TextInput
              placeholder="Senha"
              value={senha}
              onChangeText={setSenha}
              secureTextEntry
              className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
            />
            <Button label="Cadastrar e aceitar convite" onPress={cadastrarPorConvite} loading={loadingAction} />
            <Button label="Já tenho conta, ir para login" variant="outline" onPress={() => router.push('/login')} />
          </View>
        ) : null}

        {precisaGarantia ? (
          <View className="gap-3 mb-4">
            <Text className="text-primary" style={{ fontWeight: '700' }}>
              Envio de garantia
            </Text>
            <Text className="text-muted">Selecione o tipo e informe os dados da garantia.</Text>
            <View className="flex-row gap-2">
              {(['CAUCAO', 'FIADOR', 'SEGURO_FIANCA', 'TITULO_CAPITALIZACAO'] as GarantiaTipo[]).map((tipo) => (
                <Button
                  key={tipo}
                  label={formatTipo(tipo)}
                  variant={garantiaTipo === tipo ? 'primary' : 'outline'}
                  onPress={() => setGarantiaTipo(tipo)}
                />
              ))}
            </View>
            <TextInput
              placeholder='Dados da garantia em JSON (ex: {"valor":4000})'
              value={garantiaDados}
              onChangeText={setGarantiaDados}
              multiline
              numberOfLines={4}
              className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
              style={{ minHeight: 100, textAlignVertical: 'top' }}
            />
            <Button label="Enviar garantia" onPress={enviarGarantia} loading={loadingAction} />
          </View>
        ) : null}

        {conviteVinculado && conviteVinculado.candidaturaStatus === 'PENDENTE' && conviteVinculado.garantiaEscolhida != null ? (
          <View className="rounded px-4 py-4 border-2 border-border bg-surface mb-4">
            <Text className="text-primary" style={{ fontWeight: '700' }}>Candidatura enviada</Text>
            <Text className="text-muted">Agora aguarde a aprovação do proprietário.</Text>
          </View>
        ) : null}

        {contratoPendenteAssinatura ? (
          <View className="gap-3">
            <Text className="text-primary" style={{ fontWeight: '700' }}>
              Seu contrato está pronto para assinatura
            </Text>
            <Button label="Revisar e assinar contrato" onPress={() => router.push(`/${contratoPendenteAssinatura}/revisar`)} />
          </View>
        ) : null}

        {!podeAceitarComConta && !precisaCadastro && !precisaGarantia && !contratoPendenteAssinatura && !error ? (
          <View className="rounded px-4 py-4 border-2 border-border bg-surface">
            <Text className="text-primary" style={{ fontWeight: '700' }}>Sem ações pendentes neste convite.</Text>
            <Text className="text-muted">Se você já possui conta, acesse seu painel para acompanhar o andamento.</Text>
            <View className="mt-3">
              <Button label="Ir para área do inquilino" onPress={() => router.push('/tenant')} />
            </View>
          </View>
        ) : null}
      </View>
    </ScrollView>
  );
}

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function formatDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString('pt-BR');
}

function formatTipo(value: string) {
  const normalized = value.replace(/_/g, ' ').toLowerCase();
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}
