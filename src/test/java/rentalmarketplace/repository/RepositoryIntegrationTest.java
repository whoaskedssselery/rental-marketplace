package rentalmarketplace.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rentalmarketplace.exception.DatabaseAccessException;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.Review;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.util.DatabaseManager;

class RepositoryIntegrationTest {
  private final UserRepository users = new UserRepository();
  private final ListingRepository listings = new ListingRepository();
  private final RentalRequestRepository requests = new RentalRequestRepository();
  private final ReviewRepository reviews = new ReviewRepository();

  private User owner;
  private User renter;
  private Listing listing;

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(
        DatabaseManager.isAvailable(), "БД недоступна, интеграционные тесты пропущены");
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    owner = users.save(new User("IT Owner", "owner-" + suffix + "@it.test", null, UserRole.OWNER));
    renter =
        users.save(new User("IT Renter", "renter-" + suffix + "@it.test", null, UserRole.RENTER));
    listing =
        listings.save(
            new Listing(owner.getId(), "IT Drill", "d", new BigDecimal("100.00"), "IT", true));
  }

  @AfterEach
  void tearDown() {
    if (listing != null) {
      requests.findAll().stream()
          .filter(request -> request.getListingId().equals(listing.getId()))
          .forEach(request -> requests.deleteById(request.getId()));
      listings.deleteById(listing.getId());
    }
    if (renter != null) {
      users.deleteById(renter.getId());
    }
    if (owner != null) {
      users.deleteById(owner.getId());
    }
  }

  private RentalRequest request(int startDay, int endDay, RentalRequestStatus status) {
    return new RentalRequest(
        listing.getId(),
        renter.getId(),
        LocalDate.of(2030, 1, startDay),
        LocalDate.of(2030, 1, endDay),
        status,
        new BigDecimal("100.00"));
  }

  @Test
  void userRepository_savesFindsUpdatesAndFindsByEmailIgnoringCase() {
    assertTrue(owner.getId() != null && owner.getCreatedAt() != null);

    User byEmail = users.findByEmail(owner.getEmail().toUpperCase()).orElseThrow();
    assertEquals(owner.getId(), byEmail.getId());

    owner.setFullName("IT Owner Updated");
    users.update(owner);
    assertEquals("IT Owner Updated", users.findById(owner.getId()).orElseThrow().getFullName());
    assertTrue(users.findAll().stream().anyMatch(u -> u.getId().equals(owner.getId())));
  }

  @Test
  void listingRepository_updatesAvailability() {
    listing.setAvailable(false);
    listings.update(listing);

    assertFalse(listings.findById(listing.getId()).orElseThrow().isAvailable());
  }

  @Test
  void rentalRequestRepository_findsOnlyActiveRequestsOfListing() {
    RentalRequest active = requests.save(request(1, 3, RentalRequestStatus.NEW));
    requests.save(request(10, 12, RentalRequestStatus.CANCELLED));

    List<RentalRequest> found = requests.findActiveByListingId(listing.getId());

    assertEquals(1, found.size());
    assertEquals(active.getId(), found.get(0).getId());
  }

  @Test
  void database_rejectsOverlappingActiveRequestsButAllowsBackToBackAndCancelled() {
    requests.save(request(1, 5, RentalRequestStatus.NEW));

    DatabaseAccessException error =
        assertThrows(
            DatabaseAccessException.class,
            () -> requests.save(request(3, 7, RentalRequestStatus.NEW)));
    assertTrue(error.getMessage().contains("пересекаются"));

    requests.save(request(5, 8, RentalRequestStatus.NEW));
    requests.save(request(2, 4, RentalRequestStatus.CANCELLED));
  }

  @Test
  void userRepository_deleteWithReferencesGivesFriendlyMessage() {
    DatabaseAccessException error =
        assertThrows(DatabaseAccessException.class, () -> users.deleteById(owner.getId()));

    assertTrue(error.getMessage().startsWith("Нельзя удалить пользователя"));
  }

  @Test
  void reviewRepository_savesFindsUpdatesAndRejectsSecondReviewForRequest() {
    RentalRequest completed = requests.save(request(1, 3, RentalRequestStatus.COMPLETED));

    Review saved = reviews.save(new Review(completed.getId(), 4, "ok"));
    assertTrue(saved.getId() != null && saved.getCreatedAt() != null);
    assertEquals(saved.getId(), reviews.findByRequestId(completed.getId()).orElseThrow().getId());

    saved.setRating(5);
    saved.setComment(null);
    reviews.update(saved);
    Review reloaded = reviews.findById(saved.getId()).orElseThrow();
    assertEquals(5, reloaded.getRating());
    assertEquals(null, reloaded.getComment());
    assertTrue(reviews.findAll().stream().anyMatch(r -> r.getId().equals(saved.getId())));

    assertThrows(
        DatabaseAccessException.class, () -> reviews.save(new Review(completed.getId(), 3, "x")));
    assertThrows(
        DatabaseAccessException.class, () -> reviews.save(new Review(completed.getId(), 9, "x")));
  }

  @Test
  void reviewRepository_isDeletedTogetherWithRequest() {
    RentalRequest completed = requests.save(request(1, 3, RentalRequestStatus.COMPLETED));
    Review saved = reviews.save(new Review(completed.getId(), 4, "ok"));

    requests.deleteById(completed.getId());

    assertTrue(reviews.findById(saved.getId()).isEmpty());
  }

  @Test
  void reviewRepository_deleteRemovesReview() {
    RentalRequest completed = requests.save(request(1, 3, RentalRequestStatus.COMPLETED));
    Review saved = reviews.save(new Review(completed.getId(), 4, "ok"));

    reviews.deleteById(saved.getId());

    assertFalse(reviews.findByRequestId(completed.getId()).isPresent());
  }

  @Test
  void databaseManager_reportsAvailableDatabase() {
    assertTrue(DatabaseManager.isAvailable());
  }

  @Test
  void rentalRequestRepository_findsUpdatesAndDeletes() {
    RentalRequest saved = requests.save(request(1, 3, RentalRequestStatus.NEW));

    saved.setStatus(RentalRequestStatus.CONFIRMED);
    saved.setTotalPrice(new BigDecimal("250.00"));
    requests.update(saved);

    RentalRequest reloaded = requests.findById(saved.getId()).orElseThrow();
    assertEquals(RentalRequestStatus.CONFIRMED, reloaded.getStatus());
    assertEquals(new BigDecimal("250.00"), reloaded.getTotalPrice());
    assertTrue(requests.findAll().stream().anyMatch(r -> r.getId().equals(saved.getId())));

    requests.deleteById(saved.getId());
    assertTrue(requests.findById(saved.getId()).isEmpty());
  }

  @Test
  void listingRepository_deleteWithRequestsGivesFriendlyMessage() {
    requests.save(request(1, 3, RentalRequestStatus.NEW));

    DatabaseAccessException error =
        assertThrows(DatabaseAccessException.class, () -> listings.deleteById(listing.getId()));

    assertTrue(error.getMessage().startsWith("Нельзя удалить объект"));
  }

  @Test
  void userRepository_deletesUserWithoutReferences() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    User lonely =
        users.save(new User("IT Lonely", "lonely-" + suffix + "@it.test", null, UserRole.RENTER));

    users.deleteById(lonely.getId());

    assertTrue(users.findById(lonely.getId()).isEmpty());
  }
}
