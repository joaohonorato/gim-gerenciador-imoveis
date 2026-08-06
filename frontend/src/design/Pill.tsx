import { Pressable, Text } from 'react-native';
import { colors } from './tokens';

interface Props {
  label: string;
  selected: boolean;
  onPress: () => void;
}

export function Pill({ label, selected, onPress }: Props) {
  return (
    <Pressable
      onPress={onPress}
      className="px-[13px] py-2 rounded-lg"
      style={{
        backgroundColor: selected ? colors.accent : colors.card,
        borderWidth: 1.5,
        borderColor: selected ? colors.accent : colors.border,
      }}
    >
      <Text style={{ color: selected ? '#FFFFFF' : '#374151', fontSize: 12.5, fontWeight: '600' }}>{label}</Text>
    </Pressable>
  );
}
