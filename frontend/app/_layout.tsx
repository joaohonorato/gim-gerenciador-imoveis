import '../global.css';
import { Stack } from 'expo-router';

export default function RootLayout() {
  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Screen name="(auth)" />
      <Stack.Screen name="(owner)" />
      <Stack.Screen name="(tenant)" />
      <Stack.Screen name="(contrato)" />
    </Stack>
  );
}
