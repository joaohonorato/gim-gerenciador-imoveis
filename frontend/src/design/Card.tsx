import { View, ViewProps } from 'react-native';
import { colors } from './tokens';

export function Card({ children, className, ...props }: ViewProps & { className?: string }) {
  return (
    <View
      className={`bg-card rounded-xl p-4 ${className ?? ''}`}
      style={{ borderWidth: 1, borderColor: colors.border }}
      {...props}
    >
      {children}
    </View>
  );
}
