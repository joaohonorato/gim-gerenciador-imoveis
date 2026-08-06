import { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, ScrollView, Text, TextInput, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiFetch, getErrorMessage } from '@/api/client';
import { escolherEEnviarDocumentoGarantiaPorConvite, listarDocumentosGarantiaPorConvite } from '@/api/documentos';
import { Button } from '@/design/Button';
import { Pill } from '@/design/Pill';
import { ArquivoInfo, Convite, ConviteInquilino, MeResponse } from '@/api/types';

type GarantiaTipo = 'CAUCAO' | 'FIADOR' | 'SEGURO_FIANCA' | 'TITULO_CAPITALIZACAO';

export default function ConviteLocacaoScreen() {
  const insets = useSafeAreaInsets();
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

  const [documentosGarantia, setDocumentosGarantia] = useState<ArquivoInfo[]>([]);
  const [uploadingDocumento, setUploadingDocumento] = useState(false);
  const [documentosError, setDocumentosError] = useState('');

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
        setError(getErrorMessage(e, 'Não foi possível carregar o convite de locação'));
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
    return convite?.status === 'PENDENTE' && !convite?.expirado && me?.tipoConta === 'INQUILINO' && !conviteVinculado;
  }, [convite?.status, convite?.expirado, me?.tipoConta, conviteVinculado]);

  const precisaCadastro = useMemo(() => {
    return convite?.status === 'PENDENTE' && !convite?.expirado && !me && !conviteVinculado;
  }, [convite?.status, convite?.expirado, me, conviteVinculado]);

  // Convite existe mas não dá pra agir nele: expirado (ainda PENDENTE, mas
  // fora do prazo), ou já saiu de PENDENTE por um caminho que não é
  // "candidatura em andamento" (revogado/recusado). CONSUMIDO não entra
  // aqui — nesse caso o fluxo segue para contratoPendenteAssinatura/tela de
  // "sem ações pendentes", que já orienta a acessar a área do inquilino.
  const mensagemConviteMorto = useMemo(() => {
    if (!convite || conviteVinculado) return null;
    if (convite.status === 'PENDENTE' && convite.expirado) {
      return 'Este link de convite expirou. Peça ao proprietário para reenviar — reenviar já renova o prazo automaticamente.';
    }
    if (convite.status === 'REVOGADO') {
      return 'Este convite foi revogado pelo proprietário. Peça um novo convite a ele.';
    }
    if (convite.status === 'RECUSADO') {
      return 'Este convite foi recusado. Se ainda tiver interesse, peça um novo convite ao proprietário.';
    }
    return null;
  }, [convite, conviteVinculado]);

  const precisaGarantia = useMemo(() => {
    const exigeGarantia = conviteVinculado?.garantiaAceita != null && conviteVinculado.garantiaAceita !== 'NENHUMA';
    if (conviteVinculado && exigeGarantia && conviteVinculado.garantiaEscolhida == null) return true;
    return false;
  }, [conviteVinculado]);

  const candidaturaAguardandoAprovacao = useMemo(() => {
    return !!conviteVinculado && conviteVinculado.candidaturaStatus === 'PENDENTE' && !precisaGarantia;
  }, [conviteVinculado, precisaGarantia]);

  // Documento comprobatório (RG, comprovante de renda, apólice) fica
  // disponível pra envio assim que existe candidatura exigindo garantia, e
  // continua disponível enquanto a candidatura estiver pendente de análise —
  // não só na primeira tela de "enviar garantia" — pra dar tempo do
  // proprietário pedir mais um documento antes de aprovar.
  const mostrarDocumentosGarantia = useMemo(() => {
    const exigeGarantia = conviteVinculado?.garantiaAceita != null && conviteVinculado.garantiaAceita !== 'NENHUMA';
    return !!conviteVinculado && exigeGarantia && conviteVinculado.candidaturaStatus === 'PENDENTE';
  }, [conviteVinculado]);

  useEffect(() => {
    if (!mostrarDocumentosGarantia || !token) return;
    listarDocumentosGarantiaPorConvite(token).then(setDocumentosGarantia).catch(() => {});
  }, [mostrarDocumentosGarantia, token]);

  async function enviarDocumentoGarantia() {
    if (!token) return;
    setUploadingDocumento(true);
    setDocumentosError('');
    try {
      const enviado = await escolherEEnviarDocumentoGarantiaPorConvite(token);
      if (enviado) setDocumentosGarantia((atual) => [...atual, enviado]);
    } catch (e: any) {
      setDocumentosError(getErrorMessage(e, 'Não foi possível enviar o documento'));
    } finally {
      setUploadingDocumento(false);
    }
  }

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
      setError(getErrorMessage(e, 'Não foi possível aceitar o convite com sua conta atual'));
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
        enderecoImovel: null,
        unidadeId: convite?.unidadeId ?? '',
        proprietarioId: convite?.proprietarioId ?? '',
        nomeProprietario: null,
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
      setError(getErrorMessage(e, 'Não foi possível concluir o cadastro por convite'));
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
      setError(getErrorMessage(e, 'Não foi possível enviar a garantia'));
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
    <ScrollView className="flex-1 bg-surface" contentContainerClassName="p-6" contentContainerStyle={{ paddingTop: insets.top + 24 }}>
      <View
        className="w-full self-center bg-card rounded-xl p-6"
        style={{ maxWidth: 620, borderWidth: 1, borderColor: '#E5E7EB' }}
      >
        <Text className="text-primary mb-2" style={{ fontSize: 24, fontWeight: '800' }}>
          Convite de locação
        </Text>
        <Text className="text-muted mb-5" style={{ fontSize: 14 }}>
          Este convite vincula proprietário, imóvel e inquilino em uma nova relação de locação.
        </Text>

        {convite ? (
          <View
            className="mb-5 gap-2 rounded-xl px-4 py-4"
            style={{ borderWidth: 1, borderColor: '#E5E7EB', backgroundColor: '#F5F6F8' }}
          >
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

        {mensagemConviteMorto ? (
          <View className="gap-3 mb-4">
            <View className="rounded-xl px-4 py-4" style={{ borderWidth: 1, borderColor: '#FBBF24', backgroundColor: '#FFFBEB' }}>
              <Text style={{ color: '#92400E', fontWeight: '700', marginBottom: 4 }}>Este convite não está mais disponível</Text>
              <Text style={{ color: '#92400E', fontSize: 13 }}>{mensagemConviteMorto}</Text>
            </View>
            <Button label="Já tenho conta, ir para login" variant="outline" onPress={() => router.push('/login')} />
          </View>
        ) : null}

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
              placeholderTextColor="#9CA3AF"
              value={username}
              onChangeText={setUsername}
              className="bg-card px-4 py-3 text-primary rounded-xl"
              style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
            />
            <TextInput
              placeholder="CPF"
              placeholderTextColor="#9CA3AF"
              value={cpf}
              onChangeText={setCpf}
              keyboardType="numeric"
              className="bg-card px-4 py-3 text-primary rounded-xl"
              style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
            />
            <TextInput
              placeholder="E-mail"
              placeholderTextColor="#9CA3AF"
              value={email}
              onChangeText={setEmail}
              keyboardType="email-address"
              autoCapitalize="none"
              className="bg-card px-4 py-3 text-primary rounded-xl"
              style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
            />
            <TextInput
              placeholder="Senha"
              placeholderTextColor="#9CA3AF"
              value={senha}
              onChangeText={setSenha}
              secureTextEntry
              className="bg-card px-4 py-3 text-primary rounded-xl"
              style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
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
            <View className="flex-row gap-2" style={{ flexWrap: 'wrap' }}>
              {(['CAUCAO', 'FIADOR', 'SEGURO_FIANCA', 'TITULO_CAPITALIZACAO'] as GarantiaTipo[]).map((tipo) => (
                <Pill
                  key={tipo}
                  label={formatTipo(tipo)}
                  selected={garantiaTipo === tipo}
                  onPress={() => setGarantiaTipo(tipo)}
                />
              ))}
            </View>
            <TextInput
              placeholder='Dados da garantia em JSON (ex: {"valor":4000})'
              placeholderTextColor="#9CA3AF"
              value={garantiaDados}
              onChangeText={setGarantiaDados}
              multiline
              numberOfLines={4}
              className="bg-card px-4 py-3 text-primary rounded-xl"
              style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14, minHeight: 100, textAlignVertical: 'top' }}
            />
            <Button label="Enviar garantia" onPress={enviarGarantia} loading={loadingAction} />
          </View>
        ) : null}

        {mostrarDocumentosGarantia ? (
          <View className="gap-3 mb-4">
            <Text className="text-primary" style={{ fontWeight: '700' }}>
              Documento comprobatório da garantia
            </Text>
            <Text className="text-muted">
              Envie RG, comprovante de renda, apólice ou outro documento que comprove a garantia — o proprietário
              confere isso antes de aprovar. Pode enviar mais de um.
            </Text>
            {documentosGarantia.length > 0 ? (
              <View className="gap-2">
                {documentosGarantia.map((arquivo) => (
                  <View
                    key={arquivo.id}
                    className="rounded-lg px-4 py-3"
                    style={{ borderWidth: 1, borderColor: '#E5E7EB', backgroundColor: '#F5F6F8' }}
                  >
                    <Text className="text-primary" style={{ fontSize: 13, fontWeight: '600' }} numberOfLines={1}>
                      {arquivo.nomeOriginal}
                    </Text>
                  </View>
                ))}
              </View>
            ) : null}
            {documentosError ? <Text style={{ color: '#DC2626', fontSize: 13 }}>{documentosError}</Text> : null}
            <Button
              label={documentosGarantia.length > 0 ? 'Enviar outro documento' : 'Enviar documento'}
              variant="outline"
              onPress={enviarDocumentoGarantia}
              loading={uploadingDocumento}
            />
          </View>
        ) : null}

        {candidaturaAguardandoAprovacao ? (
          <View className="rounded-xl px-4 py-4 mb-4" style={{ borderWidth: 1, borderColor: '#E5E7EB', backgroundColor: '#F5F6F8' }}>
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

        {!podeAceitarComConta && !precisaCadastro && !precisaGarantia && !candidaturaAguardandoAprovacao && !contratoPendenteAssinatura && !error && !mensagemConviteMorto ? (
          <View className="rounded-xl px-4 py-4" style={{ borderWidth: 1, borderColor: '#E5E7EB', backgroundColor: '#F5F6F8' }}>
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
