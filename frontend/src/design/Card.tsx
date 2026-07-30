import { View, ViewProps } from 'react-native';

export function Card({ children, className, ...props }: ViewProps & { className?: string }) {
  return (
    <View
      className={`bg-card rounded-xl p-4 ${className ?? ''}`}
      style={{ borderWidth: 1, borderColor: '#E5E7EB' }}
      {...props}
    >
      {children}
    </View>
  );
}
