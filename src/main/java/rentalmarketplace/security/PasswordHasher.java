package rentalmarketplace.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Salted password storage; credentials never belong in the public User model. */
public final class PasswordHasher {
  private static final int ITERATIONS = 600_000;
  private final SecureRandom random = new SecureRandom();

  public String hash(String password) {
    byte[] salt = new byte[16];
    random.nextBytes(salt);
    return "pbkdf2$"
        + ITERATIONS
        + "$"
        + encode(salt)
        + "$"
        + encode(derive(password, salt, ITERATIONS));
  }

  public boolean verify(String password, String encoded) {
    if (password == null || encoded == null || encoded.length() > 256) {
      return false;
    }
    try {
      String[] parts = encoded.split("\\$", -1);
      if (parts.length != 4 || !parts[0].equals("pbkdf2")) {
        return false;
      }
      int iterations = Integer.parseInt(parts[1]);
      if (iterations < ITERATIONS || iterations > 1_200_000) {
        return false;
      }
      byte[] salt = Base64.getDecoder().decode(parts[2]);
      byte[] expected = Base64.getDecoder().decode(parts[3]);
      if (salt.length != 16 || expected.length != 32) {
        return false;
      }
      byte[] actual = derive(password, salt, iterations);
      try {
        return MessageDigest.isEqual(expected, actual);
      } finally {
        Arrays.fill(actual, (byte) 0);
      }
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private byte[] derive(String password, byte[] salt, int iterations) {
    char[] chars = password.toCharArray();
    PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 256);
    Arrays.fill(chars, '\0');
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Не удалось обработать пароль", e);
    } finally {
      spec.clearPassword();
    }
  }

  private String encode(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }
}
