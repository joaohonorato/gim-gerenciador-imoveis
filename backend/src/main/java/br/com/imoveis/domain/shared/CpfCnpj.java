package br.com.imoveis.domain.shared;

public sealed interface CpfCnpj permits Cpf, Cnpj {
    String digits();

    static CpfCnpj parse(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        return switch (digits.length()) {
            case 11 -> new Cpf(digits);
            case 14 -> new Cnpj(digits);
            default -> throw new IllegalArgumentException("cpf/cnpj deve ter 11 ou 14 dígitos");
        };
    }
}
