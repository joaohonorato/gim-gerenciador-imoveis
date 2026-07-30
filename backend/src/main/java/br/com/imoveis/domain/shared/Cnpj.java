package br.com.imoveis.domain.shared;

import java.util.Objects;

public record Cnpj(String value) implements CpfCnpj {

    private static final int[] WEIGHTS_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public Cnpj {
        Objects.requireNonNull(value, "cnpj obrigatório");
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != 14) {
            throw new IllegalArgumentException("cnpj deve conter 14 dígitos");
        }
        if (digits.chars().distinct().count() == 1) {
            throw new IllegalArgumentException("cnpj inválido");
        }
        if (!checkDigits(digits)) {
            throw new IllegalArgumentException("cnpj inválido");
        }
        value = digits;
    }

    private static boolean checkDigits(String digits) {
        int d1 = calc(digits, WEIGHTS_1);
        int d2 = calc(digits, WEIGHTS_2);
        return d1 == Character.getNumericValue(digits.charAt(12))
            && d2 == Character.getNumericValue(digits.charAt(13));
    }

    private static int calc(String digits, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights[i];
        }
        int mod = sum % 11;
        return mod < 2 ? 0 : 11 - mod;
    }

    @Override
    public String digits() { return value; }
}
