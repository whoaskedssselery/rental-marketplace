package rentalmarketplace.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.DatabaseAccessException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.service.ListingService;
import rentalmarketplace.service.RentalRequestService;
import rentalmarketplace.service.SearchService;
import rentalmarketplace.service.UserService;
import rentalmarketplace.util.DatabaseManager;

public class ConsoleMenu {
  protected final Scanner scanner = new Scanner(System.in);
  private final UserService userService;
  private final ListingService listingService;
  private final RentalRequestService rentalRequestService;
  private final SearchService searchService;

  public ConsoleMenu(
      UserService userService,
      ListingService listingService,
      RentalRequestService rentalRequestService,
      SearchService searchService) {
    this.userService = userService;
    this.listingService = listingService;
    this.rentalRequestService = rentalRequestService;
    this.searchService = searchService;
  }

  public void run() {
    boolean running = true;
    while (running) {
      printMainMenu();
      int choice = readInt("Выберите действие: ");
      try {
        switch (choice) {
          case 1 -> manageUsers();
          case 2 -> manageListings();
          case 3 -> manageRentalRequests();
          case 4 -> search();
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
    System.out.println("Таблицы в базе данных: ");
    DatabaseManager.listTableNames().forEach(tableName -> System.out.println(" - " + tableName));
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

  private void manageListings() {
    boolean back = false;
    while (!back) {
      System.out.println("--- Объекты аренды ---");
      System.out.println("1. Показать все");
      System.out.println("2. Найти по id");
      System.out.println("3. Создать");
      System.out.println("4. Обновить");
      System.out.println("5. Удалить");
      System.out.println("0. Назад");
      int choice = readInt("Выберите действие: ");
      switch (choice) {
        case 1 -> printListings(listingService.getAllListings());
        case 2 -> printListings(List.of(listingService.getListingById(readInt("id: "))));
        case 3 -> createListing();
        case 4 -> updateListing();
        case 5 -> deleteListing();
        case 0 -> back = true;
        default -> System.out.println("Неизвестный пункт меню");
      }
    }
  }

  private void createListing() {
    int ownerId = readInt("id владельца: ");
    String title = readString("Название: ");
    String description = readString("Описание: ");
    BigDecimal pricePerDay = readBigDecimal("Цена за день: ");
    String category = readString("Категория: ");
    Listing created =
        listingService.createListing(ownerId, title, description, pricePerDay, category);
    System.out.println("Создан объект аренды: " + created.toTableRow());
  }

  private void updateListing() {
    int id = readInt("id объекта для обновления: ");
    int ownerId = readInt("id владельца: ");
    String title = readString("Новое название: ");
    String description = readString("Новое описание: ");
    BigDecimal pricePerDay = readBigDecimal("Новая цена за день: ");
    String category = readString("Новая категория: ");
    boolean available = readString("Доступен (да/нет): ").equalsIgnoreCase("да");
    Listing updated =
        listingService.updateListing(
            id, ownerId, title, description, pricePerDay, category, available);
    System.out.println("Обновлено: " + updated.toTableRow());
  }

  private void deleteListing() {
    int id = readInt("id объекта для удаления: ");
    listingService.deleteListing(id);
    System.out.println("Объект аренды удалён");
  }

  private void printListings(List<Listing> listings) {
    if (listings.isEmpty()) {
      System.out.println("Объектов аренды нет");
      return;
    }
    listings.forEach(listing -> System.out.println(listing.toTableRow()));
  }

  private void search() {
    boolean back = false;
    while (!back) {
      System.out.println("--- Поиск ---");
      System.out.println("1. По названию/описанию объекта");
      System.out.println("2. По арендатору (ФИО или email)");
      System.out.println("0. Назад");
      int choice = readInt("Выберите действие: ");
      switch (choice) {
        case 1 -> printRentalRequests(searchService.searchByListing(readString("Запрос: ")));
        case 2 -> printRentalRequests(searchService.searchByRenter(readString("Запрос: ")));
        case 0 -> back = true;
        default -> System.out.println("Неизвестный пункт меню");
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

  private void manageRentalRequests() {
    boolean back = false;
    while (!back) {
      System.out.println("--- Заявки на аренду ---");
      System.out.println("1. Показать все");
      System.out.println("2. Найти по id");
      System.out.println("3. Создать");
      System.out.println("4. Изменить даты");
      System.out.println("5. Сменить статус");
      System.out.println("6. Удалить");
      System.out.println("0. Назад");
      int choice = readInt("Выберите действие: ");
      switch (choice) {
        case 1 -> printRentalRequests(rentalRequestService.getAll());
        case 2 -> printRentalRequests(List.of(rentalRequestService.getById(readInt("id: "))));
        case 3 -> createRentalRequest();
        case 4 -> updateRentalRequestDates();
        case 5 -> changeRentalRequestStatus();
        case 6 -> deleteRentalRequest();
        case 0 -> back = true;
        default -> System.out.println("Неизвестный пункт меню");
      }
    }
  }

  private void createRentalRequest() {
    int listingId = readInt("id объекта: ");
    int renterId = readInt("id арендатора: ");
    LocalDate startDate = readDate("Дата начала (гггг-мм-дд): ");
    LocalDate endDate = readDate("Дата окончания (гггг-мм-дд): ");
    RentalRequest created = rentalRequestService.create(listingId, renterId, startDate, endDate);
    System.out.println("Создана заявка: " + created.toTableRow());
  }

  private void updateRentalRequestDates() {
    int id = readInt("id заявки: ");
    LocalDate startDate = readDate("Новая дата начала (гггг-мм-дд): ");
    LocalDate endDate = readDate("Новая дата окончания (гггг-мм-дд): ");
    RentalRequest updated = rentalRequestService.updateDates(id, startDate, endDate);
    System.out.println("Обновлено: " + updated.toTableRow());
  }

  private void changeRentalRequestStatus() {
    int id = readInt("id заявки: ");
    RentalRequestStatus status = readRentalRequestStatus();
    RentalRequest updated = rentalRequestService.changeStatus(id, status);
    System.out.println("Обновлено: " + updated.toTableRow());
  }

  private void deleteRentalRequest() {
    int id = readInt("id заявки для удаления: ");
    rentalRequestService.delete(id);
    System.out.println("Заявка удалена");
  }

  private void printRentalRequests(List<RentalRequest> requests) {
    if (requests.isEmpty()) {
      System.out.println("Заявок нет");
      return;
    }
    requests.forEach(request -> System.out.println(request.toTableRow()));
  }

  private RentalRequestStatus readRentalRequestStatus() {
    while (true) {
      String input =
          readString("Статус (NEW/CONFIRMED/ACTIVE/COMPLETED/CANCELLED/REJECTED): ").toUpperCase();
      try {
        return RentalRequestStatus.valueOf(input);
      } catch (IllegalArgumentException e) {
        System.out.println(
            "Ошибка: статус должен быть NEW, CONFIRMED, ACTIVE, COMPLETED, CANCELLED или REJECTED");
      }
    }
  }
}
