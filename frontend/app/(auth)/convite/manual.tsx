import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Text, TextInput, View } from 'react-native';
import { router } from 'expo-router';
import { Button } from '@/design/Button';

export default function ConviteManualScreen() {
  const [token, setToken] = useState('');

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} className="flex-1 bg-surface">
      <ScrollView contentContainerClassName="flex-1 justify-center p-6">
        <Text className="text-primary mb-1" style={{ fontSize: 21, fontWeight: '800' }}>Abrir convite de inquilino</Text>
        <Text className="text-muted mb-2" style={{ fontSize: 13.5 }}>Cole o token recebido para finalizar seu acesso.</Text>

        <View className="gap-3">
          <TextInput
            testID="input-invite-token"
            placeholder="Token do convite"
            placeholderTextColor="#9CA3AF"
            value={token}
            onChangeText={setToken}
            autoCapitalize="none"
            className="bg-card px-4 py-3 text-primary rounded-xl"
            style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}
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