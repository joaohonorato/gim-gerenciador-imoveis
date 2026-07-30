import AsyncStorage from '@react-native-async-storage/async-storage';

const KEY = 'session_token';

export const session = {
  async get(): Promise<string | null> {
    return AsyncStorage.getItem(KEY);
  },
  async set(token: string): Promise<void> {
    await AsyncStorage.setItem(KEY, token);
  },
  async clear(): Promise<void> {
    await AsyncStorage.removeItem(KEY);
  },
};
