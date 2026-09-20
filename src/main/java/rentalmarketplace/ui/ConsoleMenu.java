package rentalmarketplace.ui;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.DatabaseAccessException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.service.UserService;
import rentalmarketplace.util.DatabaseManager;

public class ConsoleMenu {
  protected final Scanner scanner = new Scanner(System.in);
  private final UserService userService;

  public ConsoleMenu(UserService userService) {
    this.userService = userService;
  }

  public void run() {
    boolean running = true;
    while (running) {
      printMainMenu();
      int choice = readInt("Выберите действие: ");
      try {
        switch (choice) {
          case 1 -> manageUsers();
          case 2 -> notImplemented("Объекты аренды");
          case 3 -> notImplemented("Заявки на аренду");
          case 4 -> notImplemented("Поиск");
          case 5 -> notImplemented("Фильтрация");
          case 6 -> notImplemented("Сортировка");
          case 7 -> notImplemented("Статистика");
          case 8 -> notImplemented("Экспорт данных");
          case 9 -> printDatabaseTables();
          case 0 -> running = false;
          default -> System.out.println("Неизвестный пункт меню");
        }
      } catch (BusinessRuleException | EntityNotFoundException e) {
        System.out.println("Ошибка: " + e.getMessage());
      } catch (DatabaseAccessException e) {
        System.out.println("Ошибка базы данных: " + e.getMessage());
      }
    }
    System.out.println("До свидания!");
  }

  private void printMainMenu() {
    System.out.println("========================================");
    System.out.println("МАРКЕТПЛЕЙС АРЕНДЫ");
    System.out.println("========================================");
    System.out.println("1. Пользователи");
    System.out.println("2. Объекты аренды");
    System.out.println("3. Заявки на аренду");
    System.out.println("4. Поиск");
    System.out.println("5. Фильтрация");
    System.out.println("6. Сортировка");
    System.out.println("7. Статистика");
    System.out.println("8. Экспорт данных");
    System.out.println("9. Вывести таблицы базы данных");
    System.out.println("0. Выход");
  }

  private void printDatabaseTables() {
    String sql =
        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name";
    try (Connection connection = DatabaseManager.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      System.out.println("Таблицы в базе данных: ");
      while (resultSet.next()) {
        System.out.println(" - " + resultSet.getString("table_name"));
      }
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось получить список таблиц: " + e.getMessage(), e);
    }
  }

  private void manageUsers() {
    boolean back = false;
    while (!back) {
      System.out.println("--- Пользователи ---");
      System.out.println("1. Показать всех");
      System.out.println("2. Найти по id");
      System.out.println("3. Создать");
      System.out.println("4. Обновить");
      System.out.println("5. Удалить");
      System.out.println("0. Назад");
      int choice = readInt("Выберите действие: ");
      switch (choice) {
        case 1 -> printUsers(userService.getAllUsers());
        case 2 -> printUsers(List.of(userService.getUserById(readInt("id: "))));
        case 3 -> createUser();
        case 4 -> updateUser();
        case 5 -> deleteUser();
        case 0 -> back = true;
        default -> System.out.println("Неизвестный пункт меню");
      }
    }
  }

  private void createUser() {
    String fullName = readString("Имя: ");
    String email = readString("Email: ");
    String phone = readString("Телефон: ");
    UserRole role = readUserRole();
    User created = userService.createUser(fullName, email, phone, role);
    System.out.println("Создан пользователь: " + created.toTableRow());
  }

  private void updateUser() {
    int id = readInt("id пользователя для обновления: ");
    String fullName = readString("Новое имя: ");
    String email = readString("Новый email: ");
    String phone = readString("Новый телефон: ");
    UserRole role = readUserRole();
    User updated = userService.updateUser(id, fullName, email, phone, role);
    System.out.println("Обновлено: " + updated.toTableRow());
  }

  private void deleteUser() {
    int id = readInt("id пользователя для удаления: ");
    userService.deleteUser(id);
    System.out.println("Пользователь удалён");
  }

  private void printUsers(List<User> users) {
    if (users.isEmpty()) {
      System.out.println("Пользователей нет");
      return;
    }
    users.forEach(user -> System.out.println(user.toTableRow()));
  }

  private UserRole readUserRole() {
    while (true) {
      String input = readString("Роль (RENTER/OWNER/ADMIN): ").toUpperCase();
      try {
        return UserRole.valueOf(input);
      } catch (IllegalArgumentException e) {
        System.out.println("Ошибка: роль должна быть RENTER, OWNER или ADMIN");
      }
    }
  }

  private void notImplemented(String section) {
    System.out.println("[" + section + "] еще не реализовано");
  }

  protected int readInt(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = scanner.nextLine().trim();
      try {
        return Integer.parseInt(input);
      } catch (NumberFormatException e) {
        System.out.println("Ошибка: нужно ввести целое число");
      }
    }
  }

  protected String readString(String prompt) {
    System.out.print(prompt);
    return scanner.nextLine().trim();
  }

  protected BigDecimal readBigDecimal(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = scanner.nextLine().trim();
      try {
        return new java.math.BigDecimal(input);
      } catch (NumberFormatException e) {
        System.out.println("Ошибка: нужно ввести число (например, 500.00)");
      }
    }
  }

  protected LocalDate readDate(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = scanner.nextLine().trim();
      try {
        return LocalDate.parse(input);
      } catch (DateTimeParseException e) {
        System.out.println("Ошибка: формат даты гггг-мм-дд, например 2026-09-15");
      }
    }
  }
}
