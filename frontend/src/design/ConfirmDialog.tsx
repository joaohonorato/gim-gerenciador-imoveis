import { Modal, Text, View } from 'react-native';
import { Button } from './Button';
import { colors, spacing } from './tokens';

interface Props {
  visible: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

// Alert.alert do React Native é um no-op em web (react-native-web não
// implementa dialog nativo), e este app roda como portal web-first — por
// isso confirmações de ação destrutiva usam este modal em vez de Alert,
// pra funcionar igual em web/Android/iOS (Onda 1 item 2,
// docs/jornadas-e-backlog-tecnico.md).
export function ConfirmDialog({
  visible,
  title,
  message,
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  destructive = true,
  loading = false,
  onConfirm,
  onCancel,
}: Props) {
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onCancel}>
      <View
        className="flex-1 items-center justify-center px-6"
        style={{ backgroundColor: 'rgba(17,24,39,0.5)' }}
      >
        <View
          className="w-full rounded-xl bg-card p-5"
          style={{ borderWidth: 1, borderColor: colors.border, maxWidth: 380 }}
          testID="confirm-dialog"
        >
          <Text className="text-lg font-bold text-primary" style={{ marginBottom: spacing.sm }}>
            {title}
          </Text>
          <Text className="text-[15px] text-muted" style={{ marginBottom: spacing.lg, lineHeight: 21 }}>
            {message}
          </Text>
          <View style={{ gap: spacing.sm }}>
            <Button
              label={confirmLabel}
              onPress={onConfirm}
              loading={loading}
              variant={destructive ? 'danger' : 'primary'}
              testID="confirm-dialog-confirm"
            />
            <Button label={cancelLabel} onPress={onCancel} variant="outline" disabled={loading} testID="confirm-dialog-cancel" />
          </View>
        </View>
      </View>
    </Modal>
  );
}
