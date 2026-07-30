package br.com.imoveis.domain.shared;

import java.util.Objects;

public record Cpf(String value) implements CpfCnpj {

    public Cpf {
        Objects.requireNonNull(value, "cpf obrigatório");
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != 11) {
            throw new IllegalArgumentException("cpf deve conter 11 dígitos");
        }
        if (digits.chars().distinct().count() == 1) {
            throw new IllegalArgumentException("cpf inválido");
        }
        if (!checkDigits(digits)) {
            throw new IllegalArgumentException("cpf inválido");
        }
        value = digits;
    }

    private static boolean checkDigits(String digits) {
        int d1 = calc(digits, 10);
        int d2 = calc(digits, 11);
        return d1 == Character.getNumericValue(digits.charAt(9))
            && d2 == Character.getNumericValue(digits.charAt(10));
    }

    private static int calc(String digits, int startWeight) {
        int sum = 0;
        for (int i = 0; i < startWeight - 1; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * (startWeight - i);
        }
        int mod = sum * 10 % 11;
        return mod == 10 ? 0 : mod;
    }

    @Override
    public String digits() { return value; }
}
