package rentalmarketplace.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.User;

class RentalRequestMenuTest {
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

  private RentalRequestMenu menu(String... lines) {
    return new RentalRequestMenu(
        fixture.requests, fixture.listings, fixture.users, fixture.input(lines));
  }

  @Test
  void createRequest_printsCreatedRow() {
    String output =
        fixture.capture(menu("3", "1", "2", "abc", "2026-10-01", "2026-10-03", "0")::run);

    assertTrue(output.contains("формат даты"));
    assertTrue(output.contains("Создана заявка"));
    assertEquals(1, fixture.requests.getAll().size());
  }

  @Test
  void createRequest_repeatsIdQuestionUntilEntityExists() {
    String output =
        fixture.capture(menu("3", "99", "1", "2", "2026-10-01", "2026-10-03", "0")::run);

    assertTrue(output.contains("не найден"));
    assertEquals(1, fixture.requests.getAll().size());
  }

  @Test
  void createRequest_withOverlap_propagatesBusinessRuleException() {
    fixture.request(listing, renter, 1, 5);
    RentalRequestMenu menu = menu("3", "1", "2", "2026-10-03", "2026-10-07", "0");

    assertThrows(BusinessRuleException.class, () -> fixture.capture(menu::run));
  }

  @Test
  void updateDates_recomputesPrice() {
    fixture.request(listing, renter, 1, 3);

    String output = fixture.capture(menu("4", "1", "2026-10-01", "2026-10-06", "0")::run);

    assertTrue(output.contains("Обновлено"));
    assertEquals(0, new BigDecimal("500").compareTo(fixture.requests.getById(1).getTotalPrice()));
  }

  @Test
  void changeStatus_movesRequestForward() {
    fixture.request(listing, renter, 1, 3);

    String output = fixture.capture(menu("5", "1", "confirmed", "0")::run);

    assertTrue(output.contains("CONFIRMED"));
    assertEquals(RentalRequestStatus.CONFIRMED, fixture.requests.getById(1).getStatus());
  }

  @Test
  void deleteRequest_removesIt() {
    fixture.request(listing, renter, 1, 3);

    String output = fixture.capture(menu("6", "1", "0")::run);

    assertTrue(output.contains("Заявка удалена"));
    assertTrue(fixture.requests.getAll().isEmpty());
  }

  @Test
  void listAndFindById_showRequests() {
    fixture.request(listing, renter, 1, 3);

    assertTrue(fixture.capture(menu("1", "0")::run).contains("NEW"));
    assertTrue(fixture.capture(menu("2", "1", "0")::run).contains("NEW"));
    assertTrue(fixture.capture(menu("0")::run).contains("Заявки на аренду"));
  }

  @Test
  void emptyList_printsMessage() {
    assertTrue(fixture.capture(menu("1", "0")::run).contains("Заявок нет"));
  }

  @Test
  void search_findsByListingAndByRenter() {
    fixture.request(listing, renter, 1, 3);

    String output =
        fixture.capture(menu("1", "дрель", "2", "иван", "1", "нет-такого", "0")::search);

    assertTrue(output.contains("NEW"));
    assertTrue(output.contains("Заявок нет"));
  }

  @Test
  void filter_byStatusAndByOwner() {
    fixture.request(listing, renter, 1, 3);

    String byStatus = fixture.capture(menu("1", "new", "1", "completed", "0")::filter);
    String byOwner = fixture.capture(menu("2", String.valueOf(owner.getId()), "0")::filter);

    assertTrue(byStatus.contains("NEW"));
    assertTrue(byStatus.contains("Заявок нет"));
    assertTrue(byOwner.contains("NEW"));
  }

  @Test
  void sort_byCreatedAtAndByTotalPrice() {
    fixture.request(listing, renter, 1, 5);
    fixture.request(listing, renter, 5, 6);

    String output = fixture.capture(menu("2", "0")::sort);

    assertTrue(output.contains("400.00"));
    assertTrue(output.indexOf("100.00") < output.lastIndexOf("400.00"));
  }

  @Test
  void unknownChoice_printsMessage() {
    assertTrue(fixture.capture(menu("77", "0")::run).contains("Неизвестный пункт меню"));
    assertTrue(fixture.capture(menu("77", "0")::search).contains("Неизвестный пункт меню"));
    assertTrue(fixture.capture(menu("77", "0")::filter).contains("Неизвестный пункт меню"));
    assertTrue(fixture.capture(menu("77", "0")::sort).contains("Неизвестный пункт меню"));
  }
}
