package rentalmarketplace.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {
  @Test
  void saltsHashesAndVerifiesOnlyExactPassword() {
    PasswordHasher hasher = new PasswordHasher();
    String password = "Длинный пароль 🦊 2026";
    String first = hasher.hash(password);
    assertNotEquals(first, hasher.hash(password));
    assertFalse(first.contains(password));
    assertTrue(hasher.verify(password, first));
    assertFalse(hasher.verify(password + " ", first));
  }

  @Test
  void rejectsMissingOrCorruptCredentials() {
    PasswordHasher hasher = new PasswordHasher();
    for (String hash :
        new String[] {"", "broken", "pbkdf2$999999999$abc$abc", "pbkdf2$600000$!$!"}) {
      assertFalse(hasher.verify("anything", hash));
    }
    assertFalse(hasher.verify("anything", null));
    assertFalse(hasher.verify(null, "broken"));
  }
}
