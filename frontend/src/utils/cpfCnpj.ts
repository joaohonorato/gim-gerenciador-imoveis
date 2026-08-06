export function formatCpfCnpj(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, 14);

  if (digits.length <= 11) {
    return digits
      .replace(/^(\d{3})(\d)/, '$1.$2')
      .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
      .replace(/\.(\d{3})(\d)/, '.$1-$2');
  }

  return digits
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1/$2')
    .replace(/(\/\d{4})(\d)/, '$1-$2');
}

export function validateCpfCnpj(digits: string): boolean {
  if (digits.length === 11) return validateCpf(digits);
  if (digits.length === 14) return validateCnpj(digits);
  return false;
}

function validateCpf(digits: string): boolean {
  if (/^(\d)\1{10}$/.test(digits)) return false;

  const d1 = calcCpfDigit(digits, 10);
  const d2 = calcCpfDigit(digits, 11);

  return d1 === Number(digits[9]) && d2 === Number(digits[10]);
}

function calcCpfDigit(digits: string, startWeight: number): number {
  let sum = 0;
  for (let i = 0; i < startWeight - 1; i += 1) {
    sum += Number(digits[i]) * (startWeight - i);
  }
  const mod = (sum * 10) % 11;
  return mod === 10 ? 0 : mod;
}

function validateCnpj(digits: string): boolean {
  if (/^(\d)\1{13}$/.test(digits)) return false;

  const w1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  const w2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

  const d1 = calcCnpjDigit(digits, w1);
  const d2 = calcCnpjDigit(digits, w2);

  return d1 === Number(digits[12]) && d2 === Number(digits[13]);
}

function calcCnpjDigit(digits: string, weights: number[]): number {
  let sum = 0;
  for (let i = 0; i < weights.length; i += 1) {
    sum += Number(digits[i]) * weights[i];
  }
  const mod = sum % 11;
  return mod < 2 ? 0 : 11 - mod;
}
