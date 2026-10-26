package rentalmarketplace.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.security.PasswordHasher;
import rentalmarketplace.service.AuthenticationService;
import rentalmarketplace.util.DatabaseManager;

class AuthenticationIntegrationTest {
  private final UserRepository users = new UserRepository();
  private final AuthenticationService auth = new AuthenticationService(users);
  private final List<Integer> created = new ArrayList<>();
  private static final String PASSWORD = "integration passphrase 2026";

  @BeforeEach
  void setUp() {
    org.junit.jupiter.api.Assumptions.assumeTrue(DatabaseManager.isAvailable());
    auth.initialize();
  }

  @AfterEach
  void cleanup() {
    created.forEach(users::deleteById);
  }

  @Test
  void migrationIsIdempotentAndCredentialsSurviveNewService() {
    int before = users.findAll().size();
    auth.initialize();
    auth.initialize();
    assertEquals(before, users.findAll().size());
    String email = UUID.randomUUID() + "@test.com";
    User account = auth.register("Integration member", email, PASSWORD);
    created.add(account.getId());
    String hash = users.findPasswordHash(account.getId()).orElseThrow();
    assertFalse(hash.contains(PASSWORD));
    assertTrue(new PasswordHasher().verify(PASSWORD, hash));
    assertEquals(
        account.getId(),
        new AuthenticationService(users).login(email.toUpperCase(), PASSWORD).getId());
    assertThrows(
        BusinessRuleException.class,
        () -> auth.register("Duplicate", email.toUpperCase(), PASSWORD));
    // Bypass the service precheck: the database must still enforce case-insensitive uniqueness.
    assertThrows(
        BusinessRuleException.class,
        () ->
            users.saveWithPassword(
                new User("Race", email.toUpperCase(), null, UserRole.RENTER), hash));
    assertEquals(before + 1, users.findAll().size());
  }

  @Test
  void existingAccountsCannotBeClaimedAndAdminBootstrapCannotPromoteOrReset() {
    String email = UUID.randomUUID() + "@test.com";
    User legacy = users.save(new User("Legacy admin", email, null, UserRole.ADMIN));
    created.add(legacy.getId());
    assertThrows(BusinessRuleException.class, () -> auth.register("Claim", email, PASSWORD));
    assertThrows(BusinessRuleException.class, () -> auth.login(email, PASSWORD));
    auth.initializeAdminPassword(email, PASSWORD);
    assertEquals(legacy.getId(), auth.login(email, PASSWORD).getId());
    auth.initializeAdminPassword(email, "different long password");
    assertEquals(legacy.getId(), auth.login(email, PASSWORD).getId());
    String memberEmail = UUID.randomUUID() + "@test.com";
    User member = auth.register("Member", memberEmail, PASSWORD);
    created.add(member.getId());
    assertThrows(
        BusinessRuleException.class, () -> auth.initializeAdminPassword(memberEmail, PASSWORD));
    assertEquals(UserRole.RENTER, users.findById(member.getId()).orElseThrow().getRole());
    assertThrows(
        BusinessRuleException.class,
        () -> auth.initializeAdminPassword("missing@test.com", PASSWORD));
  }
}
