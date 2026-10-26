package rentalmarketplace.security;

import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;

/** Explicit privileged actor for tests of rules unrelated to authentication. */
public final class TestAccess {
  private TestAccess() {}

  public static AccessPolicy admin() {
    User admin = new User("Test administrator", "test-admin@example.com", null, UserRole.ADMIN);
    admin.setId(-1);
    return new AccessPolicy(() -> admin);
  }
}
