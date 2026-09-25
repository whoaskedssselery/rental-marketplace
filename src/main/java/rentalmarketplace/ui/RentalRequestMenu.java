package rentalmarketplace.ui;

import java.time.LocalDate;
import java.util.List;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.service.ListingService;
import rentalmarketplace.service.RentalRequestService;
import rentalmarketplace.service.UserService;

public class RentalRequestMenu implements Menu {
  private final RentalRequestService rentalRequestService;
  private final ListingService listingService;
  private final UserService userService;
  private final ConsoleInput input;

  public RentalRequestMenu(
      RentalRequestService rentalRequestService,
      ListingService listingService,
      UserService userService,
      ConsoleInput input) {
    this.rentalRequestService = rentalRequestService;
    this.listingService = listingService;
    this.userService = userService;
    this.input = input;
  }

  @Override
  public void run() {
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
      switch (input.readInt("Выберите действие: ")) {
        case 1 -> printRequests(rentalRequestService.getAll());
        case 2 -> printRequests(List.of(rentalRequestService.getById(input.readInt("id: "))));
        case 3 -> createRequest();
        case 4 -> updateDates();
        case 5 -> changeStatus();
        case 6 -> deleteRequest();
        case 0 -> back = true;
        default -> System.out.println("Неизвестный пункт меню");
      }
    }
  }

  public void search() {
    boolean back = false;
    while (!back) {
      System.out.println("--- Поиск заявок ---");
      System.out.println("1. По названию/описанию объекта");
      System.out.println("2. По арендатору (ФИО или email)");
      System.out.println("0. Назад");
      switch (input.readInt("Выберите действие: ")) {
        case 1 -> printRequests(rentalRequestService.searchByListing(input.readString("Запрос: ")));
        case 2 -> printRequests(rentalRequestService.searchByRenter(input.readString("Запрос: ")));
        case 0 -> back = true;
        default -> System.out.println("Неизвестный пункт меню");
      }
    }
  }

  public void filter() {
    boolean back = false;
    while (!back) {
      System.out.println("--- Фильтрация заявок ---");
      System.out.println("1. По статусу");
      System.out.println("2. По владельцу объекта");
      System.out.println("0. Назад");
      switch (input.readInt("Выберите действие: ")) {
        case 1 -> {
          RentalRequestStatus status = input.readEnum("Статус", RentalRequestStatus.class);
          printRequests(rentalRequestService.filterByStatus(status));
        }
        case 2 -> {
          int ownerId = input.readExistingId("id владельца: ", userService::getUserById);
          printRequests(rentalRequestService.filterByListingOwner(ownerId));
        }
        case 0 -> back = true;
        default -> System.out.println("Неизвестный пункт меню");
      }
    }
  }

  public void sort() {
    boolean back = false;
    while (!back) {
      System.out.println("--- Сортировка заявок ---");
      System.out.println("1. По дате создания (по возрастанию)");
      System.out.println("2. По сумме (по возрастанию)");
      System.out.println("0. Назад");
      switch (input.readInt("Выберите действие: ")) {
        case 1 -> printRequests(rentalRequestService.sortByCreatedAt());
        case 2 -> printRequests(rentalRequestService.sortByTotalPrice());
        case 0 -> back = true;
        default -> System.out.println("Неизвестный пункт меню");
      }
    }
  }

  private void createRequest() {
    int listingId = input.readExistingId("id объекта: ", listingService::getListingById);
    int renterId = input.readExistingId("id арендатора: ", userService::getUserById);
    LocalDate startDate = input.readDate("Дата начала (гггг-мм-дд): ");
    LocalDate endDate = input.readDate("Дата окончания (гггг-мм-дд): ");
    RentalRequest created = rentalRequestService.create(listingId, renterId, startDate, endDate);
    System.out.println("Создана заявка: " + created.toTableRow());
  }

  private void updateDates() {
    int id = input.readExistingId("id заявки: ", rentalRequestService::getById);
    LocalDate startDate = input.readDate("Новая дата начала (гггг-мм-дд): ");
    LocalDate endDate = input.readDate("Новая дата окончания (гггг-мм-дд): ");
    RentalRequest updated = rentalRequestService.updateDates(id, startDate, endDate);
    System.out.println("Обновлено: " + updated.toTableRow());
  }

  private void changeStatus() {
    int id = input.readExistingId("id заявки: ", rentalRequestService::getById);
    RentalRequestStatus status = input.readEnum("Новый статус", RentalRequestStatus.class);
    RentalRequest updated = rentalRequestService.changeStatus(id, status);
    System.out.println("Обновлено: " + updated.toTableRow());
  }

  private void deleteRequest() {
    int id = input.readExistingId("id заявки для удаления: ", rentalRequestService::getById);
    rentalRequestService.delete(id);
    System.out.println("Заявка удалена");
  }

  private void printRequests(List<RentalRequest> requests) {
    TablePrinter.print(requests, RentalRequest.TABLE_HEADER, "Заявок нет");
  }
}
