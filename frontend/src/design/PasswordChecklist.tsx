import { Text, View } from 'react-native';
import { PASSWORD_RULES } from '@/utils/password';
import { colors } from './tokens';

interface Props {
  value: string;
}

export function PasswordChecklist({ value }: Props) {
  return (
    <View className="gap-1">
      {PASSWORD_RULES.map((rule) => {
        const atendida = rule.test(value);
        return (
          <Text key={rule.label} style={{ fontSize: 12.5, color: atendida ? colors.success : colors.muted }}>
            {atendida ? '✓' : '○'} {rule.label}
          </Text>
        );
      })}
    </View>
  );
}
