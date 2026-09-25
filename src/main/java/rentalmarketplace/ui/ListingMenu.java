package rentalmarketplace.ui;

import java.math.BigDecimal;
import java.util.List;
import rentalmarketplace.model.Listing;
import rentalmarketplace.service.ListingService;
import rentalmarketplace.service.UserService;

public class ListingMenu implements Menu {
  private final ListingService listingService;
  private final UserService userService;
  private final ConsoleInput input;

  public ListingMenu(ListingService listingService, UserService userService, ConsoleInput input) {
    this.listingService = listingService;
    this.userService = userService;
    this.input = input;
  }

  @Override
  public void run() {
    boolean back = false;
    while (!back) {
      System.out.println("--- Объекты аренды ---");
      System.out.println("1. Показать все");
      System.out.println("2. Найти по id");
      System.out.println("3. Создать");
      System.out.println("4. Обновить");
      System.out.println("5. Удалить");
      System.out.println("0. Назад");
      switch (input.readInt("Выберите действие: ")) {
        case 1 -> printListings(listingService.getAllListings());
        case 2 -> printListings(List.of(listingService.getListingById(input.readInt("id: "))));
        case 3 -> createListing();
        case 4 -> updateListing();
        case 5 -> deleteListing();
        case 0 -> back = true;
        default -> System.out.println("Неизвестный пункт меню");
      }
    }
  }

  private void createListing() {
    int ownerId = input.readExistingId("id владельца: ", userService::getUserById);
    String title = input.readString("Название: ");
    String description = input.readString("Описание: ");
    BigDecimal pricePerDay = input.readBigDecimal("Цена за день: ");
    String category = input.readString("Категория: ");
    Listing created =
        listingService.createListing(ownerId, title, description, pricePerDay, category);
    System.out.println("Создан объект аренды: " + created.toTableRow());
  }

  private void updateListing() {
    int id = input.readExistingId("id объекта для обновления: ", listingService::getListingById);
    int ownerId = input.readExistingId("id владельца: ", userService::getUserById);
    String title = input.readString("Новое название: ");
    String description = input.readString("Новое описание: ");
    BigDecimal pricePerDay = input.readBigDecimal("Новая цена за день: ");
    String category = input.readString("Новая категория: ");
    boolean available = input.readYesNo("Доступен");
    Listing updated =
        listingService.updateListing(
            id, ownerId, title, description, pricePerDay, category, available);
    System.out.println("Обновлено: " + updated.toTableRow());
  }

  private void deleteListing() {
    int id = input.readExistingId("id объекта для удаления: ", listingService::getListingById);
    listingService.deleteListing(id);
    System.out.println("Объект аренды удалён");
  }

  private void printListings(List<Listing> listings) {
    TablePrinter.print(listings, Listing.TABLE_HEADER, "Объектов аренды нет");
  }
}
