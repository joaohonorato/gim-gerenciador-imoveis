import { Modal, Text, View } from 'react-native';
import { Button } from './Button';
import { colors, spacing } from './tokens';

interface Props {
  visible: boolean;
  loading: boolean;
  onAccept: () => void;
  onDecline: () => void;
}

export function PushPrimerModal({ visible, loading, onAccept, onDecline }: Props) {
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onDecline}>
      <View
        className="flex-1 items-center justify-center px-6"
        style={{ backgroundColor: 'rgba(17,24,39,0.5)' }}
      >
        <View
          className="w-full rounded-xl bg-card p-5"
          style={{ borderWidth: 1, borderColor: colors.border, maxWidth: 380 }}
          testID="push-primer-modal"
        >
          <Text className="text-lg font-bold text-primary" style={{ marginBottom: spacing.sm }}>
            Ativar notificações?
          </Text>
          <Text className="text-[15px] text-muted" style={{ marginBottom: spacing.lg, lineHeight: 21 }}>
            Avisamos sobre vencimento de contrato, garantia e contas antes que o prazo passe.
            Você pode desativar quando quiser nas configurações do dispositivo.
          </Text>
          <View style={{ gap: spacing.sm }}>
            <Button label="Ativar notificações" onPress={onAccept} loading={loading} testID="push-primer-accept" />
            <Button label="Agora não" onPress={onDecline} variant="outline" disabled={loading} testID="push-primer-decline" />
          </View>
        </View>
      </View>
    </Modal>
  );
}
