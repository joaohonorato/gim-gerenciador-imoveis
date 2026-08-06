import { Text, View } from 'react-native';
import { colors } from './tokens';

interface Props {
  label: string;
  value: number | string;
  color: string;
}

export function StatCard({ label, value, color }: Props) {
  return (
    <View
      className="bg-card rounded-xl p-5"
      style={{ borderWidth: 1, borderColor: colors.border, flexGrow: 1, flexBasis: 110, minWidth: 110 }}
    >
      <Text style={{ color, fontSize: 28, fontWeight: '800' }}>{value}</Text>
      <Text className="text-muted mt-1" style={{ fontSize: 13 }}>{label}</Text>
    </View>
  );
}
