import { Pressable, Text } from 'react-native';

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
        backgroundColor: selected ? '#2563EB' : '#FFFFFF',
        borderWidth: 1.5,
        borderColor: selected ? '#2563EB' : '#E5E7EB',
      }}
    >
      <Text style={{ color: selected ? '#FFFFFF' : '#374151', fontSize: 12.5, fontWeight: '600' }}>{label}</Text>
    </Pressable>
  );
}
