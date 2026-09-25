package rentalmarketplace.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListingMenuTest {
  private final MenuFixture fixture = new MenuFixture();

  @BeforeEach
  void setUp() {
    fixture.owner();
  }

  private String run(String... lines) {
    ListingMenu menu = new ListingMenu(fixture.listings, fixture.users, fixture.input(lines));
    return fixture.capture(menu::run);
  }

  @Test
  void createListing_repeatsPriceQuestionOnNonNumber() {
    String output = run("3", "1", "Дрель", "мощная", "abc", "100", "Инструменты", "1", "0");

    assertTrue(output.contains("нужно ввести число"));
    assertTrue(output.contains("Создан объект аренды"));
    assertTrue(output.contains("Дрель"));
  }

  @Test
  void updateListing_changesAvailability() {
    fixture.listing(fixture.users.getUserById(1));

    String output = run("4", "1", "1", "Новая", "д", "200", "Кат", "нет", "0");

    assertTrue(output.contains("Обновлено"));
    assertFalse(fixture.listings.getListingById(1).isAvailable());
    assertEquals("Новая", fixture.listings.getListingById(1).getTitle());
  }

  @Test
  void deleteListing_removesIt() {
    fixture.listing(fixture.users.getUserById(1));

    String output = run("5", "1", "0");

    assertTrue(output.contains("Объект аренды удалён"));
    assertTrue(fixture.listings.getAllListings().isEmpty());
  }

  @Test
  void emptyList_printsMessage() {
    assertTrue(run("1", "0").contains("Объектов аренды нет"));
  }

  @Test
  void findById_showsListing() {
    fixture.listing(fixture.users.getUserById(1));

    assertTrue(run("2", "1", "0").contains("Дрель"));
  }
}
