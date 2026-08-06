import { useState } from 'react';
import { Pressable, Text, TextInput, TextInputProps, View } from 'react-native';
import { colors } from './tokens';

interface Props extends Omit<TextInputProps, 'secureTextEntry' | 'style'> {
  invalid?: boolean;
}

// Campo de senha compartilhado por login/registro/redefinição/troca de senha
// (Onda 2 item 4, docs/jornadas-e-backlog-tecnico.md): autofill correto
// (textContentType/autoComplete passados pelo chamador conforme o caso —
// 'password'/'current-password' pra senha existente, 'newPassword'/
// 'new-password' pra senha nova) + alternar mostrar/ocultar. Não há
// biblioteca de ícones no projeto (design system usa glifos de texto, ex.
// "←"/"×"), e emoji de olho renderiza de forma inconsistente entre
// plataformas — por isso o toggle usa rótulo de texto ("Mostrar"/"Ocultar"),
// no mesmo padrão dos links de texto já usados em outras telas (ex. "Ver
// convite", "Abrir").
export function PasswordInput({ invalid, testID, ...props }: Props) {
  const [visible, setVisible] = useState(false);

  return (
    <View
      className="flex-row items-center bg-card rounded-xl"
      style={{ borderWidth: 1.5, borderColor: invalid ? colors.danger : colors.border }}
    >
      <TextInput
        {...props}
        testID={testID}
        placeholderTextColor="#9CA3AF"
        secureTextEntry={!visible}
        className="flex-1 px-4 py-3 text-primary"
        style={{ fontSize: 14 }}
      />
      <Pressable
        onPress={() => setVisible((atual) => !atual)}
        hitSlop={8}
        accessibilityRole="button"
        accessibilityLabel={visible ? 'Ocultar senha' : 'Mostrar senha'}
        testID={testID ? `${testID}-toggle-visibility` : undefined}
        style={{ paddingHorizontal: 14 }}
      >
        <Text style={{ fontSize: 13, fontWeight: '700', color: colors.accent }}>
          {visible ? 'Ocultar' : 'Mostrar'}
        </Text>
      </Pressable>
    </View>
  );
}
