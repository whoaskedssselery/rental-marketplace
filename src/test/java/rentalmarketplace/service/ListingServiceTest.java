package rentalmarketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.FakeListingRepository;
import rentalmarketplace.repository.FakeUserRepository;

class ListingServiceTest {
  private ListingService listingService;
  private Integer ownerId;

  @BeforeEach
  void setUp() {
    UserService userService = new UserService(new FakeUserRepository());
    listingService = new ListingService(new FakeListingRepository(), userService);
    User owner = userService.createUser("Владелец", "owner@example.com", null, UserRole.OWNER);
    ownerId = owner.getId();
  }

  @Test
  void createListing_withBlankTitle_throwsBusinessRuleException() {
    assertThrows(
        BusinessRuleException.class,
        () -> listingService.createListing(ownerId, "   ", "d", BigDecimal.valueOf(100), "cat"));
  }

  @Test
  void createListing_withNonPositivePrice_throwsBusinessRuleException() {
    assertThrows(
        BusinessRuleException.class,
        () -> listingService.createListing(ownerId, "Т", "d", BigDecimal.ZERO, "cat"));
  }

  @Test
  void createListing_withNonExistentOwner_throwsEntityNotFoundException() {
    assertThrows(
        EntityNotFoundException.class,
        () -> listingService.createListing(9999, "Т", "d", BigDecimal.valueOf(100), "cat"));
  }

  @Test
  void createListing_withValidData_savesAndReturnsListingWithGeneratedId() {
    Listing saved =
        listingService.createListing(
            ownerId, "Квартира", "уютная", BigDecimal.valueOf(2500), "Недвижимость");

    assertNotNull(saved.getId());
    assertEquals(ownerId, saved.getOwnerId());
    assertEquals("Квартира", saved.getTitle());
  }

  @Test
  void createListing_withBlankCategory_throwsBusinessRuleException() {
    assertThrows(
        BusinessRuleException.class,
        () -> listingService.createListing(ownerId, "Т", "d", BigDecimal.valueOf(100), "  "));
  }

  @Test
  void createListing_withTooLongTitle_throwsBusinessRuleException() {
    String title = "a".repeat(201);

    assertThrows(
        BusinessRuleException.class,
        () -> listingService.createListing(ownerId, title, "d", BigDecimal.valueOf(100), "cat"));
  }

  @Test
  void updateListing_changesFields() {
    Listing saved =
        listingService.createListing(ownerId, "Старое", "d", BigDecimal.valueOf(100), "cat");

    Listing updated =
        listingService.updateListing(
            saved.getId(), ownerId, "Новое", "d2", BigDecimal.valueOf(200), "cat2", false);

    assertEquals("Новое", updated.getTitle());
    assertEquals(0, BigDecimal.valueOf(200).compareTo(updated.getPricePerDay()));
    assertFalse(updated.isAvailable());
  }

  @Test
  void getListingById_withNonExistentId_throwsEntityNotFoundException() {
    assertThrows(EntityNotFoundException.class, () -> listingService.getListingById(999));
  }

  @Test
  void deleteListing_withNonExistentId_throwsEntityNotFoundException() {
    assertThrows(EntityNotFoundException.class, () -> listingService.deleteListing(999));
  }

  @Test
  void getDetails_addsOwnerName() {
    listingService.createListing(ownerId, "Дрель", "d", BigDecimal.valueOf(100), "Инструменты");

    ListingDetails details = listingService.getDetails().get(0);

    assertEquals("Владелец", details.ownerName());
    assertEquals("Дрель", details.title());
    assertEquals(ownerId, details.ownerId());
  }

  @Test
  void getCategories_returnsDistinctSortedNames() {
    listingService.createListing(ownerId, "А", "d", BigDecimal.TEN, "Спорт");
    listingService.createListing(ownerId, "Б", "d", BigDecimal.TEN, "Инструменты");
    listingService.createListing(ownerId, "В", "d", BigDecimal.TEN, "Спорт");

    assertEquals(java.util.List.of("Инструменты", "Спорт"), listingService.getCategories());
  }

  @Test
  void createListing_canStartUnavailable() {
    Listing saved =
        listingService.createListing(ownerId, "Дрель", "d", BigDecimal.TEN, "Инструменты", false);

    assertFalse(saved.isAvailable());
  }

  @Test
  void getDetailsByOwner_returnsOnlyHisListings() {
    listingService.createListing(ownerId, "Дрель", "d", BigDecimal.TEN, "Инструменты");

    assertEquals(1, listingService.getDetailsByOwner(ownerId).size());
    assertTrue(listingService.getDetailsByOwner(ownerId + 100).isEmpty());
  }
}
