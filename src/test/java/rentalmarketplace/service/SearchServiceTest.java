package rentalmarketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.InMemoryListingRepository;
import rentalmarketplace.repository.InMemoryRentalRequestRepository;
import rentalmarketplace.repository.InMemoryUserRepository;

class SearchServiceTest {
  private SearchService searchService;
  private RentalRequest scooterRequest;
  private RentalRequest laptopRequest;

  @BeforeEach
  void setUp() {
    UserService userService = new UserService(new InMemoryUserRepository());
    ListingService listingService =
        new ListingService(new InMemoryListingRepository(), userService);
    RentalRequestService rentalRequestService =
        new RentalRequestService(
            new InMemoryRentalRequestRepository(), listingService, userService);
    searchService = new SearchService(rentalRequestService, listingService, userService);

    var owner = userService.createUser("Владелец", "owner@example.com", null, UserRole.OWNER);
    var renter =
        userService.createUser("Иван Петров", "ivan.petrov@example.com", null, UserRole.RENTER);

    var scooter =
        listingService.createListing(
            owner.getId(),
            "Электросамокат Xiaomi",
            "быстрый",
            BigDecimal.valueOf(400),
            "Транспорт");
    var laptop =
        listingService.createListing(
            owner.getId(), "Ноутбук Dell", "для работы", BigDecimal.valueOf(600), "Электроника");

    scooterRequest =
        rentalRequestService.create(
            scooter.getId(), renter.getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));
    laptopRequest =
        rentalRequestService.create(
            laptop.getId(), renter.getId(), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 7));
  }

  @Test
  void searchByListing_matchesTitleCaseInsensitive() {
    var found = searchService.searchByListing("самокат");

    assertEquals(1, found.size());
    assertEquals(scooterRequest.getId(), found.get(0).getId());
  }

  @Test
  void searchByListing_withNoMatch_returnsEmptyList() {
    var found = searchService.searchByListing("автомобиль");

    assertTrue(found.isEmpty());
  }

  @Test
  void searchByRenter_matchesEmailFragment() {
    var found = searchService.searchByRenter("ivan.petrov");

    assertEquals(2, found.size());
  }

  @Test
  void searchByRenter_matchesFullNameCaseInsensitive() {
    var found = searchService.searchByRenter("иван");

    assertEquals(2, found.size());
    assertTrue(found.stream().anyMatch(r -> r.getId().equals(laptopRequest.getId())));
  }
}
