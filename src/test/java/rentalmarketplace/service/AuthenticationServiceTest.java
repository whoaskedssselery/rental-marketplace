package rentalmarketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.FakeUserRepository;

class AuthenticationServiceTest {
  private final FakeUserRepository repository = new FakeUserRepository();
  private final AuthenticationService auth = new AuthenticationService(repository);
  private static final String PASSWORD = "long enough password 2026";

  @Test
  void registersNormalMemberAndPersistsLoginAcrossSessions() {
    var user = auth.register("  Иван  ", "  Ivan@example.com ", PASSWORD);
    assertEquals(UserRole.RENTER, user.getRole());
    assertEquals("Иван", user.getFullName());
    assertEquals(user.getId(), auth.currentUser().getId());
    auth.logout();
    assertThrows(BusinessRuleException.class, auth::currentUser);
    assertEquals(
        user.getId(),
        new AuthenticationService(repository).login("IVAN@EXAMPLE.COM", PASSWORD).getId());
  }

  @Test
  void rejectsDuplicatesIncludingLegacyAccountsAndInvalidInput() {
    auth.register("Иван", "ivan@example.com", PASSWORD);
    assertThrows(
        BusinessRuleException.class, () -> auth.register("Другой", "IVAN@example.com", PASSWORD));
    repository.save(
        new rentalmarketplace.model.User("Legacy", "legacy@example.com", null, UserRole.ADMIN));
    assertThrows(
        BusinessRuleException.class, () -> auth.register("Other", "legacy@example.com", PASSWORD));
    assertThrows(BusinessRuleException.class, () -> auth.register("", "ok@example.com", PASSWORD));
    assertThrows(BusinessRuleException.class, () -> auth.register("Name", "invalid", PASSWORD));
    assertThrows(
        BusinessRuleException.class, () -> auth.register("Name", "ok@example.com", "short"));
    assertThrows(
        BusinessRuleException.class,
        () -> auth.register("Name", "ok@example.com", "a".repeat(129)));
  }

  @Test
  void failedLoginNeverLeavesAnAuthenticatedSession() {
    auth.register("Иван", "ivan@example.com", PASSWORD);
    String message =
        assertThrows(
                BusinessRuleException.class, () -> auth.login("ivan@example.com", "wrong password"))
            .getMessage();
    assertThrows(BusinessRuleException.class, auth::currentUser);
    assertEquals(
        message,
        assertThrows(BusinessRuleException.class, () -> auth.login("missing@example.com", PASSWORD))
            .getMessage());
    assertThrows(BusinessRuleException.class, () -> auth.login(null, null));
  }

  @Test
  void currentUserReloadsRoleAndRejectsDeletedAccount() {
    var user = auth.register("Иван", "ivan@example.com", PASSWORD);
    var access = new rentalmarketplace.security.AccessPolicy(auth::currentUser);
    assertFalse(access.isAdmin());
    user.setRole(UserRole.ADMIN);
    repository.update(user);
    assertTrue(access.isAdmin());
    repository.deleteById(user.getId());
    assertThrows(BusinessRuleException.class, auth::currentUser);
  }
}
