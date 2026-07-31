export function isFuturo(vencimento: string) {
  const venc = new Date(`${vencimento}T00:00:00`);
  const hoje = new Date();
  const vencMes = venc.getFullYear() * 12 + venc.getMonth();
  const hojeMes = hoje.getFullYear() * 12 + hoje.getMonth();
  return vencMes > hojeMes;
}
