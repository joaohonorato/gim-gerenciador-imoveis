import { useMemo, useState } from 'react';
import { KeyboardAvoidingView, Linking, Platform, ScrollView, Text, TextInput, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { apiFetch } from '@/api/client';
import { Button } from '@/design/Button';

type CanalEnvio = 'EMAIL' | 'WHATSAPP';
type GarantiaTipo = 'CAUCAO' | 'FIADOR' | 'SEGURO_FIANCA' | 'TITULO_CAPITALIZACAO' | 'NENHUMA';
type TipoContrato = 'RESIDENCIAL' | 'COMERCIAL';

type ConviteCriado = {
  token: string;
  id?: string;
  envio?: {
    canal: CanalEnvio;
    status: string;
    destino?: string | null;
    whatsappShareUrl?: string | null;
    detalhe?: string | null;
    enviadoEm?: string | null;
    tentativas?: number | null;
  } | null;
};

export default function NovoConviteImovelScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();

  const [canalEnvio, setCanalEnvio] = useState<CanalEnvio>('EMAIL');
  const [emailInquilino, setEmailInquilino] = useState('');
  const [telefoneInquilino, setTelefoneInquilino] = useState('');
  const [tipoContrato, setTipoContrato] = useState<TipoContrato>('RESIDENCIAL');
  const [dadosContrato, setDadosContrato] = useState<Record<TipoContrato, { valorAluguel: string; dataInicio: string; dataFim: string }>>({
    RESIDENCIAL: { valorAluguel: '1500', dataInicio: '2026-08-01', dataFim: '2027-08-01' },
    COMERCIAL: { valorAluguel: '1500', dataInicio: '2026-08-01', dataFim: '2027-08-01' },
  });
  const [garantiaAceita, setGarantiaAceita] = useState<GarantiaTipo>('CAUCAO');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [whatsappShareUrl, setWhatsappShareUrl] = useState('');
  const [ultimoTokenConvite, setUltimoTokenConvite] = useState('');

  const dadosSelecionados = dadosContrato[tipoContrato];

  const canSubmit = useMemo(() => {
    if (!id) return false;
    if (!dadosSelecionados.valorAluguel.trim() || Number.isNaN(Number(dadosSelecionados.valorAluguel))) return false;
    if (!dadosSelecionados.dataInicio.trim() || !dadosSelecionados.dataFim.trim()) return false;
    if (canalEnvio === 'EMAIL') return emailInquilino.trim().length > 3;
    return telefoneInquilino.trim().length >= 10;
  }, [id, dadosSelecionados, canalEnvio, emailInquilino, telefoneInquilino]);

  function atualizarDadosContrato(campo: 'valorAluguel' | 'dataInicio' | 'dataFim', valor: string) {
    setDadosContrato((anterior) => ({
      ...anterior,
      [tipoContrato]: {
        ...anterior[tipoContrato],
        [campo]: valor,
      },
    }));
  }

  async function enviarConvite() {
    if (!id || !canSubmit) {
      setError('Revise os campos do convite antes de enviar.');
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');
    setWhatsappShareUrl('');

    try {
      const convite = await apiFetch<ConviteCriado>(`/imoveis/${id}/convites`, {
        method: 'POST',
        body: JSON.stringify({
          tipoContrato,
          valorAluguel: Number(dadosSelecionados.valorAluguel),
          dataInicio: dadosSelecionados.dataInicio,
          dataFim: dadosSelecionados.dataFim,
          garantiaAceita: garantiaAceita === 'NENHUMA' ? null : garantiaAceita,
          canalEnvio,
          emailInquilino: emailInquilino.trim() || null,
          telefoneInquilino: telefoneInquilino.trim() || null,
        }),
      });

      const status = convite.envio?.status;
      setUltimoTokenConvite(convite.token);
      if (canalEnvio === 'WHATSAPP' && convite.envio?.whatsappShareUrl) {
        setWhatsappShareUrl(convite.envio.whatsappShareUrl);
      }

      if (status === 'ENVIADO') {
        setSuccess(`Convite ${convite.token} enviado com sucesso por e-mail. Tentativas: ${convite.envio?.tentativas ?? 1}.`);
      } else if (status === 'PRONTO_PARA_ENVIO') {
        setSuccess(`Convite ${convite.token} pronto para envio no WhatsApp. Tentativas: ${convite.envio?.tentativas ?? 1}.`);
      } else if (status === 'PULADO') {
        setSuccess(`Convite ${convite.token} criado. O inquilino já existente recebeu na inbox automaticamente.`);
      } else if (status === 'FALHA') {
        setSuccess(`Convite ${convite.token} criado, mas o envio falhou. Você pode reenviar com outro canal.`);
      } else {
        setSuccess(`Convite ${convite.token} criado com sucesso.`);
      }
    } catch (e: any) {
      setError(e.message ?? 'Não foi possível enviar o convite');
    } finally {
      setLoading(false);
    }
  }

  async function reenviarConvite() {
    if (!ultimoTokenConvite) {
      setError('Crie um convite antes de reenviar.');
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');
    setWhatsappShareUrl('');

    try {
      const convite = await apiFetch<ConviteCriado>(`/convites/${ultimoTokenConvite}/reenviar`, {
        method: 'POST',
        body: JSON.stringify({
          canalEnvio,
          emailInquilino: emailInquilino.trim() || null,
          telefoneInquilino: telefoneInquilino.trim() || null,
        }),
      });

      const status = convite.envio?.status;
      if (canalEnvio === 'WHATSAPP' && convite.envio?.whatsappShareUrl) {
        setWhatsappShareUrl(convite.envio.whatsappShareUrl);
      }

      if (status === 'ENVIADO') {
        setSuccess(`Convite ${convite.token} reenviado por e-mail. Tentativas: ${convite.envio?.tentativas ?? '-'}.`);
      } else if (status === 'PRONTO_PARA_ENVIO') {
        setSuccess(`Convite ${convite.token} pronto para novo envio no WhatsApp. Tentativas: ${convite.envio?.tentativas ?? '-'}.`);
      } else if (status === 'PULADO') {
        setSuccess(`Reenvio registrado para o convite ${convite.token}, mas não foi enviado automaticamente.`);
      } else if (status === 'FALHA') {
        setSuccess(`Reenvio registrado para o convite ${convite.token}, mas houve falha no canal atual.`);
      } else {
        setSuccess(`Reenvio registrado para o convite ${convite.token}.`);
      }
    } catch (e: any) {
      setError(e.message ?? 'Não foi possível reenviar o convite');
    } finally {
      setLoading(false);
    }
  }

  async function abrirWhatsapp() {
    if (!whatsappShareUrl) return;
    await Linking.openURL(whatsappShareUrl);
  }

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} className="flex-1 bg-surface">
      <ScrollView contentContainerClassName="p-6">
        <Text className="text-primary mb-2" style={{ fontSize: 24, fontWeight: '800' }}>Enviar convite de locação</Text>
        <Text className="text-muted mb-6">Escolha o meio de envio do link: e-mail ou WhatsApp.</Text>

        <View className="gap-3 mb-5">
          <Text className="text-primary" style={{ fontWeight: '700' }}>Canal de envio</Text>
          <View className="flex-row gap-2">
            <Button label="E-mail" variant={canalEnvio === 'EMAIL' ? 'primary' : 'outline'} onPress={() => setCanalEnvio('EMAIL')} />
            <Button label="WhatsApp" variant={canalEnvio === 'WHATSAPP' ? 'primary' : 'outline'} onPress={() => setCanalEnvio('WHATSAPP')} />
          </View>

          {canalEnvio === 'EMAIL' ? (
            <TextInput
              placeholder="E-mail do inquilino"
              value={emailInquilino}
              onChangeText={setEmailInquilino}
              autoCapitalize="none"
              keyboardType="email-address"
              className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
            />
          ) : (
            <TextInput
              placeholder="Telefone do inquilino (com DDD)"
              value={telefoneInquilino}
              onChangeText={setTelefoneInquilino}
              keyboardType="phone-pad"
              className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
            />
          )}
        </View>

        <View className="gap-3 mb-5">
          <Text className="text-primary" style={{ fontWeight: '700' }}>Dados do contrato</Text>
          <View className="flex-row gap-2">
            <Button label="Residencial" variant={tipoContrato === 'RESIDENCIAL' ? 'primary' : 'outline'} onPress={() => setTipoContrato('RESIDENCIAL')} />
            <Button label="Comercial" variant={tipoContrato === 'COMERCIAL' ? 'primary' : 'outline'} onPress={() => setTipoContrato('COMERCIAL')} />
          </View>

          <TextInput
            placeholder="Valor do aluguel"
            value={dadosSelecionados.valorAluguel}
            onChangeText={(valor) => atualizarDadosContrato('valorAluguel', valor)}
            keyboardType="numeric"
            className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
          />
          <TextInput
            placeholder="Data início (YYYY-MM-DD)"
            value={dadosSelecionados.dataInicio}
            onChangeText={(valor) => atualizarDadosContrato('dataInicio', valor)}
            className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
          />
          <TextInput
            placeholder="Data fim (YYYY-MM-DD)"
            value={dadosSelecionados.dataFim}
            onChangeText={(valor) => atualizarDadosContrato('dataFim', valor)}
            className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
          />

          <View className="flex-row gap-2" style={{ flexWrap: 'wrap' }}>
            {(['CAUCAO', 'FIADOR', 'SEGURO_FIANCA', 'TITULO_CAPITALIZACAO', 'NENHUMA'] as GarantiaTipo[]).map((tipo) => (
              <Button
                key={tipo}
                label={tipo.replace(/_/g, ' ')}
                variant={garantiaAceita === tipo ? 'primary' : 'outline'}
                onPress={() => setGarantiaAceita(tipo)}
              />
            ))}
          </View>
        </View>

        {error ? <Text style={{ color: '#DC2626', marginBottom: 10 }}>{error}</Text> : null}
        {success ? <Text style={{ color: '#16A34A', marginBottom: 10 }}>{success}</Text> : null}

        <View className="gap-2">
          <Button label="Criar e enviar convite" onPress={enviarConvite} loading={loading} disabled={!canSubmit || loading} />
          {ultimoTokenConvite ? <Button label="Reenviar convite (mesmo token)" variant="outline" onPress={reenviarConvite} loading={loading} disabled={!canSubmit || loading} /> : null}
          {whatsappShareUrl ? <Button label="Abrir WhatsApp" variant="outline" onPress={abrirWhatsapp} /> : null}
          <Button label="Voltar para imóveis" variant="outline" onPress={() => router.back()} />
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
