package br.com.imoveis.infrastructure.auth;

import br.com.imoveis.application.ports.PasswordHasher;
import jakarta.inject.Singleton;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Singleton
public class Pbkdf2PasswordHasher implements PasswordHasher {

    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RNG = new SecureRandom();

    @Override
    public String hash(String senha) {
        validarSenha(senha);
        byte[] salt = new byte[SALT_BYTES];
        RNG.nextBytes(salt);
        byte[] derived = pbkdf2(senha.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        return "pbkdf2$" + ITERATIONS + "$" + Base64.getUrlEncoder().withoutPadding().encodeToString(salt)
            + "$" + Base64.getUrlEncoder().withoutPadding().encodeToString(derived);
    }

    @Override
    public boolean matches(String senha, String senhaHash) {
        validarSenha(senha);
        String[] parts = senhaHash.split("\\$");
        if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
            throw new IllegalArgumentException("formato de hash de senha inválido");
        }
        int iterations = Integer.parseInt(parts[1]);
        byte[] salt = Base64.getUrlDecoder().decode(parts[2]);
        byte[] expected = Base64.getUrlDecoder().decode(parts[3]);
        byte[] actual = pbkdf2(senha.toCharArray(), salt, iterations, expected.length * 8);
        return java.security.MessageDigest.isEqual(expected, actual);
    }

    private static void validarSenha(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("senha obrigatória");
        }
        if (senha.length() < 8) {
            throw new IllegalArgumentException("senha deve ter ao menos 8 caracteres");
        }
    }

    private static byte[] pbkdf2(char[] senha, byte[] salt, int iterations, int keyLength) {
        try {
            PBEKeySpec spec = new PBEKeySpec(senha, salt, iterations, keyLength);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("erro ao gerar hash de senha", e);
        }
    }
}