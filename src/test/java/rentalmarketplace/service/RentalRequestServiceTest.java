package rentalmarketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.FakeListingRepository;
import rentalmarketplace.repository.FakeRentalRequestRepository;
import rentalmarketplace.repository.FakeUserRepository;

class RentalRequestServiceTest {
  private UserService userService;
  private ListingService listingService;
  private RentalRequestService service;
  private User owner;
  private User otherOwner;
  private User renter;
  private Listing listing;
  private Listing scooter;

  @BeforeEach
  void setUp() {
    userService = new UserService(new FakeUserRepository());
    listingService = new ListingService(new FakeListingRepository(), userService);
    service =
        new RentalRequestService(new FakeRentalRequestRepository(), listingService, userService);
    owner = userService.createUser("Owner", "owner@example.com", null, UserRole.OWNER);
    otherOwner = userService.createUser("Other", "other@example.com", null, UserRole.OWNER);
    renter = userService.createUser("Иван Петров", "ivan@example.com", null, UserRole.RENTER);
    listing =
        listingService.createListing(
            owner.getId(), "Дрель", "мощная", new BigDecimal("1000.00"), "Инструменты");
    scooter =
        listingService.createListing(
            otherOwner.getId(), "Электросамокат", "быстрый", new BigDecimal("400"), "Транспорт");
  }

  private RentalRequest book(Listing target, User user, int startDay, int endDay) {
    return service.create(
        target.getId(),
        user.getId(),
        LocalDate.of(2026, 10, startDay),
        LocalDate.of(2026, 10, endDay));
  }

  @Test
  void create_success_computesTotalPriceAndSetsNewStatus() {
    RentalRequest created = book(listing, renter, 1, 5);

    assertEquals(RentalRequestStatus.NEW, created.getStatus());
    assertEquals(0, new BigDecimal("4000.00").compareTo(created.getTotalPrice()));
  }

  @Test
  void create_throwsWhenEndDateNotAfterStartDate() {
    assertThrows(BusinessRuleException.class, () -> book(listing, renter, 5, 5));
  }

  @Test
  void create_throwsWhenDatesAreMissing() {
    assertThrows(
        BusinessRuleException.class,
        () -> service.create(listing.getId(), renter.getId(), null, LocalDate.of(2026, 10, 5)));
  }

  @Test
  void create_throwsWhenRenterIsOwner() {
    assertThrows(BusinessRuleException.class, () -> book(listing, owner, 1, 5));
  }

  @Test
  void create_throwsWhenListingNotFound() {
    LocalDate start = LocalDate.of(2026, 10, 1);
    LocalDate end = LocalDate.of(2026, 10, 5);

    assertThrows(
        EntityNotFoundException.class, () -> service.create(999, renter.getId(), start, end));
  }

  @Test
  void create_throwsWhenRenterNotFound() {
    LocalDate start = LocalDate.of(2026, 10, 1);
    LocalDate end = LocalDate.of(2026, 10, 5);

    assertThrows(
        EntityNotFoundException.class, () -> service.create(listing.getId(), 999, start, end));
  }

  @Test
  void create_throwsWhenListingIsNotAvailable() {
    listingService.updateListing(
        listing.getId(),
        owner.getId(),
        listing.getTitle(),
        listing.getDescription(),
        listing.getPricePerDay(),
        listing.getCategory(),
        false);

    assertThrows(BusinessRuleException.class, () -> book(listing, renter, 1, 5));
  }

  @Test
  void create_throwsWhenDatesOverlapActiveRequest() {
    book(listing, renter, 1, 10);

    assertThrows(BusinessRuleException.class, () -> book(listing, renter, 5, 12));
  }

  @Test
  void create_allowsBackToBackBookings() {
    book(listing, renter, 1, 5);

    book(listing, renter, 5, 8);
    book(listing, renter, 15, 20);
    book(listing, renter, 20, 25);
  }

  @Test
  void create_allowsDatesOverlappingCancelledRequest() {
    RentalRequest first = book(listing, renter, 1, 5);
    service.changeStatus(first.getId(), RentalRequestStatus.CANCELLED);

    book(listing, renter, 3, 7);
  }

  @Test
  void updateDates_success_recomputesTotal() {
    RentalRequest request = book(listing, renter, 1, 3);

    RentalRequest updated =
        service.updateDates(request.getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 6));

    assertEquals(0, new BigDecimal("5000.00").compareTo(updated.getTotalPrice()));
  }

  @Test
  void updateDates_throwsWhenStatusIsNotNew() {
    RentalRequest request = book(listing, renter, 1, 3);
    service.changeStatus(request.getId(), RentalRequestStatus.CONFIRMED);
    LocalDate start = LocalDate.of(2026, 10, 5);
    LocalDate end = LocalDate.of(2026, 10, 10);

    assertThrows(
        BusinessRuleException.class, () -> service.updateDates(request.getId(), start, end));
  }

  @Test
  void updateDates_doesNotOverlapWithItself() {
    RentalRequest request = book(listing, renter, 1, 5);

    service.updateDates(request.getId(), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 6));
  }

  @Test
  void updateDates_throwsWhenOverlapsOtherActiveRequest() {
    book(listing, renter, 1, 5);
    RentalRequest second = book(listing, renter, 10, 15);
    LocalDate start = LocalDate.of(2026, 10, 3);
    LocalDate end = LocalDate.of(2026, 10, 12);

    assertThrows(
        BusinessRuleException.class, () -> service.updateDates(second.getId(), start, end));
  }

  @Test
  void changeStatus_allowsValidTransitions() {
    RentalRequest request = book(listing, renter, 1, 5);

    service.changeStatus(request.getId(), RentalRequestStatus.CONFIRMED);
    service.changeStatus(request.getId(), RentalRequestStatus.ACTIVE);
    RentalRequest finished = service.changeStatus(request.getId(), RentalRequestStatus.COMPLETED);

    assertEquals(RentalRequestStatus.COMPLETED, finished.getStatus());
  }

  @Test
  void changeStatus_throwsForbiddenTransition() {
    RentalRequest request = book(listing, renter, 1, 5);

    assertThrows(
        BusinessRuleException.class,
        () -> service.changeStatus(request.getId(), RentalRequestStatus.COMPLETED));
  }

  @Test
  void changeStatus_throwsFromTerminalStatus() {
    RentalRequest request = book(listing, renter, 1, 5);
    service.changeStatus(request.getId(), RentalRequestStatus.CANCELLED);

    assertThrows(
        BusinessRuleException.class,
        () -> service.changeStatus(request.getId(), RentalRequestStatus.CONFIRMED));
  }

  @Test
  void delete_removesRequest() {
    RentalRequest request = book(listing, renter, 1, 5);

    service.delete(request.getId());

    assertThrows(EntityNotFoundException.class, () -> service.getById(request.getId()));
  }

  @Test
  void searchByListing_matchesTitleCaseInsensitive() {
    book(listing, renter, 1, 3);
    RentalRequest scooterRequest = book(scooter, renter, 1, 3);

    List<RentalRequest> found = service.searchByListing("САМОКАТ");

    assertEquals(1, found.size());
    assertEquals(scooterRequest.getId(), found.get(0).getId());
  }

  @Test
  void searchByListing_matchesDescription() {
    RentalRequest drillRequest = book(listing, renter, 1, 3);
    book(scooter, renter, 1, 3);

    List<RentalRequest> found = service.searchByListing("мощн");

    assertEquals(List.of(drillRequest.getId()), found.stream().map(RentalRequest::getId).toList());
  }

  @Test
  void searchByListing_withNoMatch_returnsEmptyList() {
    book(listing, renter, 1, 3);

    assertTrue(service.searchByListing("автомобиль").isEmpty());
  }

  @Test
  void searchByRenter_matchesNameAndEmailCaseInsensitive() {
    User another = userService.createUser("Пётр", "petr@example.com", null, UserRole.RENTER);
    book(listing, renter, 1, 3);
    book(scooter, another, 1, 3);

    assertEquals(1, service.searchByRenter("иван").size());
    assertEquals(1, service.searchByRenter("PETR@").size());
  }

  @Test
  void filterByStatus_returnsOnlyRequestsWithThatStatus() {
    RentalRequest confirmed = book(listing, renter, 1, 3);
    book(scooter, renter, 1, 3);
    service.changeStatus(confirmed.getId(), RentalRequestStatus.CONFIRMED);

    List<RentalRequest> found = service.filterByStatus(RentalRequestStatus.CONFIRMED);

    assertEquals(1, found.size());
    assertEquals(confirmed.getId(), found.get(0).getId());
    assertTrue(service.filterByStatus(RentalRequestStatus.COMPLETED).isEmpty());
  }

  @Test
  void filterByListingOwner_returnsOnlyRequestsForOwnersListings() {
    book(listing, renter, 1, 3);
    RentalRequest scooterRequest = book(scooter, renter, 1, 3);

    List<RentalRequest> found = service.filterByListingOwner(otherOwner.getId());

    assertEquals(1, found.size());
    assertEquals(scooterRequest.getId(), found.get(0).getId());
  }

  @Test
  void sortByTotalPrice_returnsAscendingOrder() {
    RentalRequest expensive = book(listing, renter, 1, 11);
    RentalRequest cheap = book(scooter, renter, 1, 2);
    RentalRequest medium = book(listing, renter, 11, 13);

    List<Integer> ids = service.sortByTotalPrice().stream().map(RentalRequest::getId).toList();

    assertEquals(List.of(cheap.getId(), medium.getId(), expensive.getId()), ids);
  }

  @Test
  void sortByCreatedAt_returnsAscendingOrder() {
    RentalRequest first = book(listing, renter, 1, 3);
    RentalRequest second = book(scooter, renter, 1, 3);
    RentalRequest third = book(listing, renter, 3, 5);
    first.setCreatedAt(first.getCreatedAt().plusDays(2));
    second.setCreatedAt(second.getCreatedAt().plusDays(1));

    List<Integer> ids = service.sortByCreatedAt().stream().map(RentalRequest::getId).toList();

    assertEquals(List.of(third.getId(), second.getId(), first.getId()), ids);
  }
}
