package rentalmarketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.InMemoryRentalRequestRepository;

class RentalRequestServiceTest {

  private InMemoryRentalRequestRepository repository;
  private Map<Integer, Listing> listings;
  private Map<Integer, User> users;
  private RentalRequestService service;

  private static final Integer OWNER_ID = 1;
  private static final Integer RENTER_ID = 2;
  private static final Integer LISTING_ID = 100;
  private static final BigDecimal PRICE = new BigDecimal("1000.00");

  @BeforeEach
  void setUp() {
    repository = new InMemoryRentalRequestRepository();
    listings = new HashMap<>();
    users = new HashMap<>();

    Listing listing =
        new Listing(LISTING_ID, OWNER_ID, "Test listing", "desc", PRICE, "cat", true, null);
    listings.put(LISTING_ID, listing);

    users.put(OWNER_ID, new User(OWNER_ID, "Owner", "o@e.com", null, UserRole.OWNER, null));
    users.put(RENTER_ID, new User(RENTER_ID, "Renter", "r@e.com", null, UserRole.RENTER, null));

    UserService stubUserService =
        new UserService(null) {
          @Override
          public User getUserById(Integer id) {
            User user = users.get(id);
            if (user == null) {
              throw new EntityNotFoundException("Пользователь с id=" + id + " не найден");
            }
            return user;
          }
        };

    ListingService stubListingService =
        new ListingService(null, null) {
          @Override
          public Listing getListingById(Integer id) {
            Listing listing = listings.get(id);
            if (listing == null) {
              throw new EntityNotFoundException("Объект аренды с id=" + id + " не найден");
            }
            return listing;
          }
        };

    service = new RentalRequestService(repository, stubListingService, stubUserService);
  }

  @Test
  void create_success_computesTotalPriceAndSetsNewStatus() {
    LocalDate start = LocalDate.of(2026, 10, 1);
    LocalDate end = LocalDate.of(2026, 10, 5);

    RentalRequest created = service.create(LISTING_ID, RENTER_ID, start, end);

    assertEquals(RentalRequestStatus.NEW, created.getStatus());
    assertEquals(0, new BigDecimal("4000.00").compareTo(created.getTotalPrice()));
  }

  @Test
  void create_throwsWhenEndDateNotAfterStartDate() {
    LocalDate start = LocalDate.of(2026, 10, 5);
    LocalDate end = LocalDate.of(2026, 10, 5);

    assertThrows(
        BusinessRuleException.class, () -> service.create(LISTING_ID, RENTER_ID, start, end));
  }

  @Test
  void create_throwsWhenRenterIsOwner() {
    LocalDate start = LocalDate.of(2026, 10, 1);
    LocalDate end = LocalDate.of(2026, 10, 5);

    assertThrows(
        BusinessRuleException.class, () -> service.create(LISTING_ID, OWNER_ID, start, end));
  }

  @Test
  void create_throwsWhenListingNotFound() {
    assertThrows(
        EntityNotFoundException.class,
        () -> service.create(999, RENTER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5)));
  }

  @Test
  void create_throwsWhenRenterNotFound() {
    assertThrows(
        EntityNotFoundException.class,
        () ->
            service.create(LISTING_ID, 999, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5)));
  }

  @Test
  void create_throwsWhenDatesOverlapActiveRequest() {
    service.create(LISTING_ID, RENTER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10));

    assertThrows(
        BusinessRuleException.class,
        () ->
            service.create(
                LISTING_ID, RENTER_ID, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12)));
  }

  @Test
  void create_allowsDatesNotOverlappingTerminalRequest() {
    RentalRequest first =
        service.create(LISTING_ID, RENTER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
    service.changeStatus(first.getId(), RentalRequestStatus.CANCELLED);

    service.create(LISTING_ID, RENTER_ID, LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 7));
  }

  @Test
  void updateDates_success_recomputesTotal() {
    RentalRequest request =
        service.create(LISTING_ID, RENTER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));

    RentalRequest updated =
        service.updateDates(request.getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 6));

    assertEquals(0, new BigDecimal("5000.00").compareTo(updated.getTotalPrice()));
  }

  @Test
  void updateDates_throwsWhenStatusIsNotNew() {
    RentalRequest request =
        service.create(LISTING_ID, RENTER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));
    service.changeStatus(request.getId(), RentalRequestStatus.CONFIRMED);
    service.changeStatus(request.getId(), RentalRequestStatus.ACTIVE);
    service.changeStatus(request.getId(), RentalRequestStatus.COMPLETED);

    assertThrows(
        BusinessRuleException.class,
        () ->
            service.updateDates(
                request.getId(), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 10)));
  }

  @Test
  void updateDates_doesNotOverlapWithItself() {
    RentalRequest request =
        service.create(LISTING_ID, RENTER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));

    service.updateDates(request.getId(), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 6));
  }

  @Test
  void updateDates_throwsWhenOverlapsOtherActiveRequest() {
    service.create(LISTING_ID, RENTER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
    RentalRequest second =
        service.create(
            LISTING_ID, RENTER_ID, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15));

    assertThrows(
        BusinessRuleException.class,
        () ->
            service.updateDates(
                second.getId(), LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 12)));
  }

  @Test
  void changeStatus_allowsValidTransitions() {
    RentalRequest request =
        service.create(LISTING_ID, RENTER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));

    service.changeStatus(request.getId(), RentalRequestStatus.CONFIRMED);
    service.changeStatus(request.getId(), RentalRequestStatus.ACTIVE);
    RentalRequest finished = service.changeStatus(request.getId(), RentalRequestStatus.COMPLETED);

    assertEquals(RentalRequestStatus.COMPLETED, finished.getStatus());
  }

  @Test
  void changeStatus_throwsForbiddenTransition() {
    RentalRequest request =
        service.create(LISTING_ID, RENTER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));

    assertThrows(
        BusinessRuleException.class,
        () -> service.changeStatus(request.getId(), RentalRequestStatus.COMPLETED));
  }

  @Test
  void changeStatus_throwsFromTerminalStatus() {
    RentalRequest request =
        service.create(LISTING_ID, RENTER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
    service.changeStatus(request.getId(), RentalRequestStatus.CANCELLED);

    assertThrows(
        BusinessRuleException.class,
        () -> service.changeStatus(request.getId(), RentalRequestStatus.CONFIRMED));
  }
}
