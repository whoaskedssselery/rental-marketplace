package rentalmarketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.InMemoryUserRepository;
import rentalmarketplace.repository.UserRepository;

class UserServiceTest {
  private UserRepository userRepository;
  private UserService userService;

  @BeforeEach
  void setUp() {
    userRepository = new InMemoryUserRepository();
    userService = new UserService(userRepository);
  }

  @Test
  void createUser_withValidData_savesAndReturnsUserWithGeneratedId() {
    User created =
        userService.createUser("Иван Петров", "ivan@example.com", "+7 900 000-00-00", UserRole.OWNER);

    assertNotNull(created.getId());
    assertNotNull(created.getCreatedAt());
    assertEquals("Иван Петров", created.getFullName());
    assertEquals("ivan@example.com", created.getEmail());
    assertEquals(UserRole.OWNER, created.getRole());
  }

  @Test
  void createUser_withBlankFullName_throwsBusinessRuleException() {
    assertThrows(
        BusinessRuleException.class,
        () -> userService.createUser("   ", "ivan@example.com", null, UserRole.RENTER));
  }

  @Test
  void createUser_withInvalidEmail_throwsBusinessRuleException() {
    assertThrows(
        BusinessRuleException.class,
        () -> userService.createUser("Иван Петров", "not-an-email", null, UserRole.RENTER));
  }

  @Test
  void createUser_withDuplicateEmail_throwsBusinessRuleException() {
    userService.createUser("Иван Петров", "ivan@example.com", null, UserRole.RENTER);

    BusinessRuleException exception =
        assertThrows(
            BusinessRuleException.class,
            () -> userService.createUser("Другой Иван", "ivan@example.com", null, UserRole.OWNER));
    assertTrue(exception.getMessage().contains("ivan@example.com"));
  }

  @Test
  void getUserById_withNonExistentId_throwsEntityNotFoundException() {
    assertThrows(EntityNotFoundException.class, () -> userService.getUserById(999));
  }

  @Test
  void updateUser_withNonExistentId_throwsEntityNotFoundException() {
    assertThrows(
        EntityNotFoundException.class,
        () -> userService.updateUser(999, "Имя", "email@example.com", null, UserRole.RENTER));
  }

  @Test
  void deleteUser_withNonExistentId_throwsEntityNotFoundException() {
    assertThrows(EntityNotFoundException.class, () -> userService.deleteUser(999));
  }

  @Test
  void updateUser_allowsKeepingOwnEmail() {
    User created = userService.createUser("Иван Петров", "ivan@example.com", null, UserRole.RENTER);

    User updated =
        userService.updateUser(
            created.getId(), "Иван Петров-Сидоров", "ivan@example.com", "+7 900 111-11-11", UserRole.OWNER);

    assertEquals("Иван Петров-Сидоров", updated.getFullName());
    assertEquals(UserRole.OWNER, updated.getRole());
  }
}
