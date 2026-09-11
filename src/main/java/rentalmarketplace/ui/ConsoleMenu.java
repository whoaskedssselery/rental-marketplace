package rentalmarketplace.ui;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.DatabaseAccessException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.util.DatabaseManager;

public class ConsoleMenu {
  protected final Scanner scanner = new Scanner(System.in);

  public void run() {
    boolean running = true;
    while (running) {
      printMainMenu();
      int choice = readInt("Выберите действие: ");
      try {
        switch (choice) {
          case 1 -> notImplemented("Пользователи");
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
