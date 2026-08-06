import { Platform } from 'react-native';
import * as Haptics from 'expo-haptics';

// Onda 4 item 10 (docs/jornadas-e-backlog-tecnico.md): feedback tátil leve
// em confirmações/erros importantes — expo-haptics não tem efeito no web
// (SDK 57), então cada helper sai cedo em Platform.OS === 'web' em vez de
// deixar a chamada nativa falhar silenciosamente a cada uso.
export function hapticSuccess(): void {
  if (Platform.OS === 'web') return;
  void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
}

export function hapticError(): void {
  if (Platform.OS === 'web') return;
  void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
}

export function hapticDestructiveConfirm(): void {
  if (Platform.OS === 'web') return;
  void Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
}
