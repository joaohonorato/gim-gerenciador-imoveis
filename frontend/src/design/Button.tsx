import { Pressable, Text, ActivityIndicator } from 'react-native';

interface Props {
  label: string;
  onPress: () => void;
  loading?: boolean;
  disabled?: boolean;
  variant?: 'primary' | 'outline';
  testID?: string;
}

export function Button({ label, onPress, loading, disabled, variant = 'primary', testID }: Props) {
  const isPrimary = variant === 'primary';
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled || loading}
      testID={testID}
      className={`flex-row items-center justify-center px-4 py-[13px] rounded-xl ${disabled || loading ? 'opacity-50' : ''}`}
      style={{
        backgroundColor: isPrimary ? '#2563EB' : '#FFFFFF',
        borderWidth: isPrimary ? 0 : 1.5,
        borderColor: '#E5E7EB',
      }}
    >
      {loading
        ? <ActivityIndicator color={isPrimary ? '#fff' : '#111827'} />
        : <Text className={`text-[15px] font-bold ${isPrimary ? 'text-white' : 'text-primary'}`}>{label}</Text>
      }
    </Pressable>
  );
}
