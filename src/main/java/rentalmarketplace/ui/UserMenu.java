package rentalmarketplace.ui;

import java.util.List;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.service.UserService;

public class UserMenu implements Menu {
  private final UserService userService;
  private final ConsoleInput input;

  public UserMenu(UserService userService, ConsoleInput input) {
    this.userService = userService;
    this.input = input;
  }

  @Override
  public void run() {
    boolean back = false;
    while (!back) {
      System.out.println("--- Пользователи ---");
      System.out.println("1. Показать всех");
      System.out.println("2. Найти по id");
      System.out.println("3. Создать");
      System.out.println("4. Обновить");
      System.out.println("5. Удалить");
      System.out.println("0. Назад");
      switch (input.readInt("Выберите действие: ")) {
        case 1 -> printUsers(userService.getAllUsers());
        case 2 -> printUsers(List.of(userService.getUserById(input.readInt("id: "))));
        case 3 -> createUser();
        case 4 -> updateUser();
        case 5 -> deleteUser();
        case 0 -> back = true;
        default -> System.out.println("Неизвестный пункт меню");
      }
    }
  }

  private void createUser() {
    String fullName = input.readString("Имя: ");
    String email = input.readString("Email: ");
    String phone = input.readString("Телефон: ");
    UserRole role = input.readEnum("Роль", UserRole.class);
    User created = userService.createUser(fullName, email, phone, role);
    System.out.println("Создан пользователь: " + created.toTableRow());
  }

  private void updateUser() {
    int id = input.readExistingId("id пользователя для обновления: ", userService::getUserById);
    String fullName = input.readString("Новое имя: ");
    String email = input.readString("Новый email: ");
    String phone = input.readString("Новый телефон: ");
    UserRole role = input.readEnum("Роль", UserRole.class);
    User updated = userService.updateUser(id, fullName, email, phone, role);
    System.out.println("Обновлено: " + updated.toTableRow());
  }

  private void deleteUser() {
    int id = input.readExistingId("id пользователя для удаления: ", userService::getUserById);
    userService.deleteUser(id);
    System.out.println("Пользователь удалён");
  }

  private void printUsers(List<User> users) {
    TablePrinter.print(users, User.TABLE_HEADER, "Пользователей нет");
  }
}
