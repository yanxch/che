package org.eclipse.che.security;

import javax.crypto.SecretKeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static java.util.Objects.requireNonNull;

/**
 * <a href="https://en.wikipedia.org/wiki/PBKDF2">Password-based-Key-Derivative-Function</a>.
 *
 * @author Yevhenii Voevodin
 */
public class PBKDF2PasswordEncryptor implements PasswordEncryptor {

    private static final String       SECRET_KEY_FACTORY_NAME = "PBKDF2WithHmacSHA1";
    private static final SecureRandom SECURE_RANDOM           = new SecureRandom();
    private static final int          ITERATIONS_COUNT        = 1000;
    private static final int          SALT_LENGTH             = 64 / 8;

    @Override
    public String encrypt(String password) {
        requireNonNull(password, "Required non-null password");
        final SecretKeyFactory keyFactory;
        try {
            keyFactory = SecretKeyFactory.getInstance(SECRET_KEY_FACTORY_NAME);
        } catch (NoSuchAlgorithmException x) {
            throw new RuntimeException(x.getMessage(), x);
        }


        return null;
    }

    @Override
    public boolean test(String password, String encryptedPassword) {
        return false;
    }
}
