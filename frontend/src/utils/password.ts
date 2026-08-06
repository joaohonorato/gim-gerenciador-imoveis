export interface PasswordRule {
  label: string;
  test: (senha: string) => boolean;
}

// Regra de senha nova compartilhada por registro, redefinição e troca de
// senha no perfil (Onda 2 item 4, docs/jornadas-e-backlog-tecnico.md). O
// backend só exige 8+ caracteres (Pbkdf2PasswordHasher.validarSenha) — a
// exigência de letra+número é só orientação de UX, mais forte que o mínimo
// do backend, então nunca rejeita uma senha que o backend aceitaria.
export const PASSWORD_RULES: PasswordRule[] = [
  { label: '8+ caracteres', test: (senha) => senha.length >= 8 },
  { label: 'Tem letra', test: (senha) => /[A-Za-z]/.test(senha) },
  { label: 'Tem número', test: (senha) => /\d/.test(senha) },
];

export function isPasswordValid(senha: string): boolean {
  return PASSWORD_RULES.every((rule) => rule.test(senha));
}
