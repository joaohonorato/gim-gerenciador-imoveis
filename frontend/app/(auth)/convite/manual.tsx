import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Text, TextInput, View } from 'react-native';
import { router } from 'expo-router';
import { Button } from '@/design/Button';

export default function ConviteManualScreen() {
  const [token, setToken] = useState('');

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} className="flex-1 bg-surface">
      <ScrollView contentContainerClassName="flex-1 justify-center p-6">
        <Text className="text-3xl font-bold text-primary mb-2">Abrir Convite de Inquilino</Text>
        <Text className="text-muted mb-8">Cole o token recebido para finalizar o acesso do inquilino.</Text>

        <View className="gap-4">
          <TextInput
            testID="input-invite-token"
            placeholder="Token do convite"
            value={token}
            onChangeText={setToken}
            autoCapitalize="none"
            className="border-2 border-border rounded px-3 py-3 bg-card text-primary"
          />
          <Button
            testID="btn-open-invite"
            label="Abrir convite"
            onPress={() => router.push(`/locacao/${encodeURIComponent(token.trim())}`)}
            disabled={!token.trim()}
          />
          <Button label="Voltar" onPress={() => router.back()} variant="outline" />
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}