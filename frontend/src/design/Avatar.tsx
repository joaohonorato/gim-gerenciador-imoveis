import { Image, Text, View } from 'react-native';
import { colors } from './tokens';

interface Props {
  url?: string | null;
  nome?: string | null;
  size?: number;
}

export function Avatar({ url, nome, size = 48 }: Props) {
  const dimensao = { width: size, height: size, borderRadius: size / 2 };

  if (url) {
    return (
      <Image
        source={{ uri: url }}
        style={[dimensao, { borderWidth: 1, borderColor: colors.border }]}
      />
    );
  }

  const iniciais = (nome ?? '?').trim().charAt(0).toUpperCase() || '?';

  return (
    <View
      style={[
        dimensao,
        {
          backgroundColor: colors.accent,
          alignItems: 'center',
          justifyContent: 'center',
          borderWidth: 1,
          borderColor: colors.border,
        },
      ]}
    >
      <Text style={{ color: '#fff', fontWeight: '800', fontSize: size * 0.4 }}>{iniciais}</Text>
    </View>
  );
}
