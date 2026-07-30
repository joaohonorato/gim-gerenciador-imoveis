import { View, Text } from 'react-native';

type Status = 'VAGO' | 'RESERVADO' | 'ALUGADO' | 'PENDENTE' | 'ASSINADO' | 'PAGO' | 'ATRASADO';

const config: Record<Status, { label: string; color: string }> = {
  VAGO:      { label: 'Vago', color: '#16A34A' },
  RESERVADO: { label: 'Reservado', color: '#D97706' },
  ALUGADO:   { label: 'Alugado', color: '#2563EB' },
  PENDENTE:  { label: 'Pendente', color: '#D97706' },
  ASSINADO:  { label: 'Assinado', color: '#16A34A' },
  PAGO:      { label: 'Pago', color: '#16A34A' },
  ATRASADO:  { label: 'Atrasado', color: '#DC2626' },
};

export function StatusBadge({ status }: { status: Status }) {
  const { label, color } = config[status] ?? { label: status, color: '#6B7280' };
  return (
    <View className="flex-row items-center gap-2">
      <View style={{ width: 8, height: 8, borderRadius: 4, backgroundColor: color }} />
      <Text style={{ color }} className="text-xs font-semibold">{label}</Text>
    </View>
  );
}
