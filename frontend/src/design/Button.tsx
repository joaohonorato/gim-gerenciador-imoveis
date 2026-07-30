import { Pressable, Text, ActivityIndicator } from 'react-native';

interface Props {
  label: string;
  onPress: () => void;
  loading?: boolean;
  disabled?: boolean;
  variant?: 'primary' | 'outline' | 'dark';
  testID?: string;
}

const backgroundByVariant = { primary: '#2563EB', dark: '#111827', outline: '#FFFFFF' } as const;

export function Button({ label, onPress, loading, disabled, variant = 'primary', testID }: Props) {
  const isOutline = variant === 'outline';
  const isDark = !isOutline;
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled || loading}
      testID={testID}
      className={`flex-row items-center justify-center px-4 py-[13px] rounded-xl ${disabled || loading ? 'opacity-50' : ''}`}
      style={{
        backgroundColor: backgroundByVariant[variant],
        borderWidth: isOutline ? 1.5 : 0,
        borderColor: '#E5E7EB',
      }}
    >
      {loading
        ? <ActivityIndicator color={isDark ? '#fff' : '#111827'} />
        : <Text className={`text-[15px] font-bold ${isDark ? 'text-white' : 'text-primary'}`}>{label}</Text>
      }
    </Pressable>
  );
}
