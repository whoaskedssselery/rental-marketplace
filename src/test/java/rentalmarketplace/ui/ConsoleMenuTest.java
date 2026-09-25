package rentalmarketplace.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rentalmarketplace.exception.DatabaseAccessException;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.User;
import rentalmarketplace.repository.SchemaRepository;
import rentalmarketplace.service.ReportService;

class ConsoleMenuTest {
  private final MenuFixture fixture = new MenuFixture();
  private User owner;
  private User renter;
  private Listing listing;

  @BeforeEach
  void setUp() {
    owner = fixture.owner();
    renter = fixture.renter();
    listing = fixture.listing(owner);
  }

  private ConsoleMenu mainMenu(ReportService reports, String... lines) {
    ConsoleInput input = fixture.input(lines);
    RentalRequestMenu requestMenu =
        new RentalRequestMenu(fixture.requests, fixture.listings, fixture.users, input);
    return new ConsoleMenu(
        input,
        new UserMenu(fixture.users, input),
        new ListingMenu(fixture.listings, fixture.users, input),
        requestMenu,
        reports);
  }

  private String run(String... lines) {
    return fixture.capture(mainMenu(fixture.reports, lines)::run);
  }

  @Test
  void tablesStatisticsAndUnknownChoice_thenExit() {
    String output = run("9", "7", "99", "0");

    assertTrue(output.contains("rental_requests"));
    assertTrue(output.contains("Пользователей: 2"));
    assertTrue(output.contains("Объектов аренды: 1"));
    assertTrue(output.contains("Неизвестный пункт меню"));
    assertTrue(output.contains("До свидания!"));
  }

  @Test
  void submenus_areReachableAndReturnToMainMenu() {
    String output = run("1", "0", "2", "0", "3", "0", "4", "0", "5", "0", "6", "0", "0");

    assertTrue(output.contains("--- Пользователи ---"));
    assertTrue(output.contains("--- Объекты аренды ---"));
    assertTrue(output.contains("--- Заявки на аренду ---"));
    assertTrue(output.contains("--- Поиск заявок ---"));
    assertTrue(output.contains("--- Фильтрация заявок ---"));
    assertTrue(output.contains("--- Сортировка заявок ---"));
    assertEquals(8, output.split("МАРКЕТПЛЕЙС АРЕНДЫ", -1).length);
  }

  @Test
  void businessRuleViolation_isPrintedAndProgramContinues() {
    String output =
        run(
            "3",
            "3",
            "1",
            "2",
            "2026-10-01",
            "2026-10-03",
            "3",
            "1",
            "2",
            "2026-10-02",
            "2026-10-04",
            "0");

    assertTrue(output.contains("Ошибка: Даты пересекаются"));
    assertTrue(output.contains("До свидания!"));
    assertEquals(1, fixture.requests.getAll().size());
  }

  @Test
  void missingEntity_isPrintedAsError() {
    String output = run("1", "2", "99", "0", "0");

    assertTrue(output.contains("Ошибка: Пользователь с id=99 не найден"));
  }

  @Test
  void databaseFailure_isPrintedAsDatabaseError() {
    SchemaRepository broken =
        new SchemaRepository() {
          @Override
          public List<String> findAllTableNames() {
            throw new DatabaseAccessException("нет соединения", null);
          }
        };
    ReportService reports =
        new ReportService(fixture.users, fixture.listings, fixture.requests, broken);

    String output = fixture.capture(mainMenu(reports, "9", "0")::run);

    assertTrue(output.contains("Ошибка базы данных: нет соединения"));
  }

  @Test
  void endOfInput_exitsGracefully() {
    String output = run("9");

    assertTrue(output.contains("До свидания!"));
  }

  @Test
  void export_reportsCreatedFile() throws Exception {
    String output = run("8", "0");

    String marker = "Данные выгружены в файл: ";
    int start = output.indexOf(marker);
    assertTrue(start >= 0);
    Path file = Path.of(output.substring(start + marker.length()).lines().findFirst().orElse(""));
    try {
      assertTrue(Files.exists(file));
    } finally {
      Files.deleteIfExists(file);
    }
  }
}
