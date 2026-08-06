import { useEffect, useState } from 'react';
import { View, Text, ScrollView, Linking } from 'react-native';
import { useLocalSearchParams, router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { apiFetch, ApiException, getErrorMessage } from '@/api/client';
import { escolherEEnviarDocumentoContrato, escolherEEnviarDocumentoGarantia } from '@/api/documentos';
import { ArquivoInfo, Contrato, DocumentosContrato, MeResponse } from '@/api/types';
import { Card } from '@/design/Card';
import { Button } from '@/design/Button';
import { StatusBadge } from '@/design/StatusBadge';
import { hapticError, hapticSuccess } from '@/utils/haptics';

export default function RevisarContratoScreen() {
  const insets = useSafeAreaInsets();
  const { id } = useLocalSearchParams<{ id: string }>();
  const [contrato, setContrato] = useState<Contrato | null>(null);
  const [me, setMe] = useState<MeResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [cadastroIncompleto, setCadastroIncompleto] = useState(false);

  const [documentos, setDocumentos] = useState<DocumentosContrato | null>(null);
  const [uploadingDocumento, setUploadingDocumento] = useState(false);
  const [uploadingGarantia, setUploadingGarantia] = useState(false);
  const [documentosError, setDocumentosError] = useState('');

  useEffect(() => {
    Promise.all([
      apiFetch<Contrato>(`/contratos/${id}`),
      apiFetch<MeResponse>('/auth/me'),
    ])
      .then(([contratoData, meData]) => {
        setContrato(contratoData);
        setMe(meData);
      })
      .catch(() => {});
    carregarDocumentos();
  }, [id]);

  function carregarDocumentos() {
    apiFetch<DocumentosContrato>(`/contratos/${id}/documentos`)
      .then(setDocumentos)
      .catch(() => {});
  }

  async function abrirArquivo(arquivo: ArquivoInfo) {
    try {
      const { url } = await apiFetch<{ url: string }>(`/arquivos/${arquivo.id}/url`);
      Linking.openURL(url);
    } catch (e: any) {
      setDocumentosError(getErrorMessage(e, 'Não foi possível abrir o arquivo'));
    }
  }

  async function enviarDocumentoContrato() {
    if (!id) return;
    setUploadingDocumento(true);
    setDocumentosError('');
    try {
      await escolherEEnviarDocumentoContrato(id);
      carregarDocumentos();
    } catch (e: any) {
      setDocumentosError(getErrorMessage(e, 'Não foi possível enviar o documento'));
    } finally {
      setUploadingDocumento(false);
    }
  }

  async function enviarDocumentoGarantia() {
    if (!id) return;
    setUploadingGarantia(true);
    setDocumentosError('');
    try {
      await escolherEEnviarDocumentoGarantia(id);
      carregarDocumentos();
    } catch (e: any) {
      setDocumentosError(getErrorMessage(e, 'Não foi possível enviar o documento'));
    } finally {
      setUploadingGarantia(false);
    }
  }

  async function assinar() {
    if (!me) return;

    const parte = me.tipoConta === 'INQUILINO' ? 'INQUILINO' : 'PROPRIETARIO';
    setLoading(true);
    setError('');
    setCadastroIncompleto(false);
    try {
      const updated = await apiFetch<Contrato>(`/contratos/${id}/assinar`, {
        method: 'POST',
        body: JSON.stringify({ parte }),
      });
      setContrato(updated);
      hapticSuccess();
    } catch (e: any) {
      hapticError();
      if (e instanceof ApiException && e.error.code === 'CADASTRO_INCOMPLETO') {
        setCadastroIncompleto(true);
      } else {
        setError(getErrorMessage(e, 'Erro ao assinar'));
      }
    } finally {
      setLoading(false);
    }
  }

  if (!contrato) {
    return <View className="flex-1 bg-surface justify-center items-center"><Text className="text-muted">Carregando...</Text></View>;
  }

  const podeAssinarComoProprietario = me?.tipoConta === 'PROPRIETARIO' && !contrato.assinouProprietario;
  const podeAssinarComoInquilino = me?.tipoConta === 'INQUILINO' && !contrato.assinouInquilino;
  const exibirBotaoAssinar = podeAssinarComoProprietario || podeAssinarComoInquilino;
  const assinaturaLabel = me?.tipoConta === 'INQUILINO' ? 'Assinar como inquilino' : 'Assinar como proprietário';

  return (
    <ScrollView className="flex-1 bg-surface" contentContainerClassName="p-6" contentContainerStyle={{ paddingTop: insets.top + 24 }}>
      <View className="flex-row items-center gap-4 mb-6">
        <Button label="← Voltar" onPress={() => router.back()} variant="outline" />
        <Text className="text-primary" style={{ fontSize: 24, fontWeight: '800' }}>Revisar contrato</Text>
      </View>

      <View className="gap-4">
        <Card>
          <Text className="text-primary mb-4" style={{ fontSize: 15, fontWeight: '700' }}>Detalhes</Text>
          <View className="gap-3">
            <DetailRow label="Tipo" value={formatTipo(contrato.tipo ?? contrato.tipoContrato)} />
            <DetailRow label="Aluguel" value={formatCurrency(contrato.valorAluguel)} />
            <DetailRow label="Período" value={`${formatDate(contrato.dataInicio)} até ${formatDate(contrato.dataFim)}`} />
            <View className="flex-row items-center justify-between">
              <Text className="text-muted" style={{ fontSize: 14 }}>Status</Text>
              <StatusBadge status={contrato.statusAssinatura as 'PENDENTE' | 'ASSINADO'} />
            </View>
          </View>
        </Card>

        <Card>
          <Text className="text-primary mb-4" style={{ fontSize: 15, fontWeight: '700' }}>Assinaturas</Text>
          <View className="gap-4">
            <SignatureRow label="Proprietário" signed={contrato.assinouProprietario} />
            <SignatureRow label="Inquilino" signed={contrato.assinouInquilino} />
          </View>
          {exibirBotaoAssinar ? (
            <View className="gap-3 mt-5">
              {cadastroIncompleto ? (
                <View className="gap-2 rounded-lg px-4 py-3" style={{ backgroundColor: '#FFFBEB' }}>
                  <Text style={{ color: '#B45309', fontSize: 13, fontWeight: '600' }}>
                    Complete seu CPF/CNPJ no perfil antes de assinar contratos.
                  </Text>
                  <Text
                    testID="link-completar-cadastro"
                    onPress={() => router.push('/perfil')}
                    accessibilityRole="link"
                    style={{ color: '#2563EB', fontSize: 13, fontWeight: '700' }}
                  >
                    Ir para o perfil
                  </Text>
                </View>
              ) : error ? (
                <Text style={{ color: '#DC2626', fontSize: 13 }}>{error}</Text>
              ) : null}
              <Button testID="btn-assinar" label={assinaturaLabel} onPress={assinar} loading={loading} />
            </View>
          ) : null}
          {contrato.statusAssinatura === 'ASSINADO' ? (
            <View className="mt-5 rounded-lg px-4 py-3" style={{ backgroundColor: '#F0FDF4' }}>
              <Text className="text-center" style={{ color: '#16A34A', fontSize: 13, fontWeight: '600' }}>
                Contrato totalmente assinado!
              </Text>
            </View>
          ) : null}
        </Card>

        <Card>
          <Text className="text-primary mb-4" style={{ fontSize: 15, fontWeight: '700' }}>Documentos</Text>
          {documentosError ? <Text className="mb-3" style={{ color: '#DC2626', fontSize: 13 }}>{documentosError}</Text> : null}

          <View className="gap-2 mb-5">
            <Text className="text-muted" style={{ fontSize: 13, fontWeight: '600' }}>Documento do contrato</Text>
            {documentos?.documentoContrato ? (
              <ArquivoRow arquivo={documentos.documentoContrato} onAbrir={abrirArquivo} />
            ) : (
              <Text className="text-muted" style={{ fontSize: 13 }}>Nenhum documento enviado.</Text>
            )}
            <Button
              label={documentos?.documentoContrato ? 'Substituir documento' : 'Enviar documento'}
              variant="outline"
              onPress={enviarDocumentoContrato}
              loading={uploadingDocumento}
            />
          </View>

          {contrato.garantiaTipo ? (
            <View className="gap-2">
              <Text className="text-muted" style={{ fontSize: 13, fontWeight: '600' }}>Documentos da garantia</Text>
              {documentos?.documentosGarantia?.length ? (
                <View className="gap-2">
                  {documentos.documentosGarantia.map((arquivo) => (
                    <ArquivoRow key={arquivo.id} arquivo={arquivo} onAbrir={abrirArquivo} />
                  ))}
                </View>
              ) : (
                <Text className="text-muted" style={{ fontSize: 13 }}>Nenhum documento enviado.</Text>
              )}
              <Button label="Adicionar documento" variant="outline" onPress={enviarDocumentoGarantia} loading={uploadingGarantia} />
            </View>
          ) : null}
        </Card>
      </View>
    </ScrollView>
  );
}

function ArquivoRow({ arquivo, onAbrir }: { arquivo: ArquivoInfo; onAbrir: (arquivo: ArquivoInfo) => void }) {
  return (
    <View
      className="flex-row items-center justify-between rounded-lg px-4 py-3"
      style={{ borderWidth: 1, borderColor: '#E5E7EB', backgroundColor: '#F5F6F8' }}
    >
      <Text className="text-primary" style={{ fontSize: 13, fontWeight: '600', flexShrink: 1 }} numberOfLines={1}>
        {arquivo.nomeOriginal}
      </Text>
      <Text
        onPress={() => onAbrir(arquivo)}
        accessibilityRole="link"
        accessibilityLabel={`Abrir documento ${arquivo.nomeOriginal}`}
        style={{ color: '#2563EB', fontSize: 13, fontWeight: '700' }}
      >
        Abrir
      </Text>
    </View>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <View className="flex-row items-center justify-between gap-4">
      <Text className="text-muted" style={{ fontSize: 14 }}>{label}</Text>
      <Text className="text-primary text-right" style={{ fontSize: 14, fontWeight: '600', flexShrink: 1 }}>{value}</Text>
    </View>
  );
}

function SignatureRow({ label, signed }: { label: string; signed: boolean }) {
  const color = signed ? '#16A34A' : '#D97706';
  const text = signed ? 'Assinado' : 'Pendente';

  return (
    <View className="flex-row items-center justify-between">
      <Text style={{ color: '#374151', fontSize: 14 }}>{label}</Text>
      <Text style={{ color, fontSize: 13, fontWeight: '600' }}>{text}</Text>
    </View>
  );
}

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function formatDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString('pt-BR');
}

function formatTipo(value?: string) {
  if (!value) return 'Não informado';
  const normalized = value.replace(/_/g, ' ').toLowerCase();
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}
