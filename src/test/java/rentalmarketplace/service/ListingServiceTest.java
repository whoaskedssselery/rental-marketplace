package rentalmarketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.InMemoryListingRepository;
import rentalmarketplace.repository.InMemoryUserRepository;

class ListingServiceTest {
  private ListingService listingService;
  private Integer ownerId;

  @BeforeEach
  void setUp() {
    UserService userService = new UserService(new InMemoryUserRepository());
    listingService = new ListingService(new InMemoryListingRepository(), userService);
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
  void getListingById_withNonExistentId_throwsEntityNotFoundException() {
    assertThrows(EntityNotFoundException.class, () -> listingService.getListingById(999));
  }

  @Test
  void deleteListing_withNonExistentId_throwsEntityNotFoundException() {
    assertThrows(EntityNotFoundException.class, () -> listingService.deleteListing(999));
  }
}
