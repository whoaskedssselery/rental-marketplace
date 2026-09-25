package rentalmarketplace.ui;

import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.DatabaseAccessException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.exception.ExportException;
import rentalmarketplace.exception.InputClosedException;
import rentalmarketplace.model.Statistics;
import rentalmarketplace.service.ReportService;

public class ConsoleMenu implements Menu {
  private final ConsoleInput input;
  private final Menu userMenu;
  private final Menu listingMenu;
  private final RentalRequestMenu rentalRequestMenu;
  private final ReportService reportService;

  public ConsoleMenu(
      ConsoleInput input,
      Menu userMenu,
      Menu listingMenu,
      RentalRequestMenu rentalRequestMenu,
      ReportService reportService) {
    this.input = input;
    this.userMenu = userMenu;
    this.listingMenu = listingMenu;
    this.rentalRequestMenu = rentalRequestMenu;
    this.reportService = reportService;
  }

  @Override
  public void run() {
    try {
      boolean running = true;
      while (running) {
        printMainMenu();
        running = handleChoice(input.readInt("Выберите действие: "));
      }
    } catch (InputClosedException e) {
      System.out.println();
    }
    System.out.println("До свидания!");
  }

  private boolean handleChoice(int choice) {
    try {
      switch (choice) {
        case 1 -> userMenu.run();
        case 2 -> listingMenu.run();
        case 3 -> rentalRequestMenu.run();
        case 4 -> rentalRequestMenu.search();
        case 5 -> rentalRequestMenu.filter();
        case 6 -> rentalRequestMenu.sort();
        case 7 -> printStatistics();
        case 8 -> exportData();
        case 9 -> printDatabaseTables();
        case 0 -> {
          return false;
        }
        default -> System.out.println("Неизвестный пункт меню");
      }
    } catch (BusinessRuleException | EntityNotFoundException e) {
      System.out.println("Ошибка: " + e.getMessage());
    } catch (DatabaseAccessException e) {
      System.out.println("Ошибка базы данных: " + e.getMessage());
    } catch (ExportException e) {
      System.out.println("Ошибка экспорта: " + e.getMessage());
    }
    return true;
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

  private void printStatistics() {
    Statistics statistics = reportService.collectStatistics();
    System.out.println("--- Статистика ---");
    System.out.println("Пользователей: " + statistics.totalUsers());
    System.out.println("Объектов аренды: " + statistics.totalListings());
    System.out.println("Заявок всего: " + statistics.totalRequests());
    System.out.println("Активных заявок (NEW/CONFIRMED/ACTIVE): " + statistics.activeRequests());
    System.out.println("Завершённых заявок: " + statistics.completedRequests());
    System.out.println(
        "Отменённых или отклонённых заявок: " + statistics.cancelledOrRejectedRequests());
    System.out.println("Средняя сумма аренды: " + statistics.averageTotalPrice());
  }

  private void exportData() {
    System.out.println("Данные выгружены в файл: " + reportService.exportToExcel());
  }

  private void printDatabaseTables() {
    System.out.println("Таблицы в базе данных:");
    reportService.getTableNames().forEach(name -> System.out.println(" - " + name));
  }
}
