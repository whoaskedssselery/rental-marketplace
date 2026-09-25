package rentalmarketplace.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.model.UserRole;

class UserMenuTest {
  private final MenuFixture fixture = new MenuFixture();

  private String run(String... lines) {
    UserMenu menu = new UserMenu(fixture.users, fixture.input(lines));
    return fixture.capture(menu::run);
  }

  @Test
  void createUser_thenListShowsIt() {
    String output = run("3", "Иван Петров", "ivan@example.com", "", "owner", "1", "0");

    assertTrue(output.contains("Создан пользователь"));
    assertTrue(output.contains("ivan@example.com"));
    assertEquals(1, fixture.users.getAllUsers().size());
  }

  @Test
  void createUser_repeatsRoleQuestionOnUnknownRole() {
    String output = run("3", "Иван", "ivan@example.com", "", "boss", "renter", "0");

    assertTrue(output.contains("допустимые значения"));
    assertEquals(UserRole.RENTER, fixture.users.getAllUsers().get(0).getRole());
  }

  @Test
  void updateUser_changesFields() {
    fixture.owner();

    String output = run("4", "1", "Новое имя", "new@example.com", "", "admin", "0");

    assertTrue(output.contains("Обновлено"));
    assertEquals("Новое имя", fixture.users.getUserById(1).getFullName());
    assertEquals(UserRole.ADMIN, fixture.users.getUserById(1).getRole());
  }

  @Test
  void deleteUser_removesUser() {
    fixture.owner();

    String output = run("5", "1", "0");

    assertTrue(output.contains("Пользователь удалён"));
    assertTrue(fixture.users.getAllUsers().isEmpty());
  }

  @Test
  void emptyList_printsMessage() {
    assertTrue(run("1", "0").contains("Пользователей нет"));
  }

  @Test
  void unknownChoice_printsMessageAndStays() {
    assertTrue(run("42", "0").contains("Неизвестный пункт меню"));
  }

  @Test
  void findById_ofMissingUser_propagatesNotFound() {
    assertThrows(EntityNotFoundException.class, () -> run("2", "99", "0"));
  }
}
