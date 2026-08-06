import { View, Text } from 'react-native';
import { colors } from './tokens';

type Status = 'VAGO' | 'RESERVADO' | 'ALUGADO' | 'PENDENTE' | 'ASSINADO' | 'PAGO' | 'ATRASADO';

const config: Record<Status, { label: string; color: string }> = {
  VAGO:      { label: 'Vago', color: colors.success },
  RESERVADO: { label: 'Reservado', color: colors.warning },
  ALUGADO:   { label: 'Alugado', color: colors.accent },
  PENDENTE:  { label: 'Pendente', color: colors.warning },
  ASSINADO:  { label: 'Assinado', color: colors.success },
  PAGO:      { label: 'Pago', color: colors.success },
  ATRASADO:  { label: 'Atrasado', color: colors.danger },
};

export function StatusBadge({ status }: { status: Status }) {
  const { label, color } = config[status] ?? { label: status, color: colors.muted };
  return (
    <View className="flex-row items-center gap-2">
      <View style={{ width: 8, height: 8, borderRadius: 4, backgroundColor: color }} />
      <Text style={{ color }} className="text-xs font-semibold">{label}</Text>
    </View>
  );
}
