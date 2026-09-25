package rentalmarketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.Statistics;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.FakeListingRepository;
import rentalmarketplace.repository.FakeRentalRequestRepository;
import rentalmarketplace.repository.FakeUserRepository;

class ReportServiceTest {
  private RentalRequestService requests;
  private ReportService reportService;
  private User renter;
  private Listing listing;

  @BeforeEach
  void setUp() {
    UserService userService = new UserService(new FakeUserRepository());
    ListingService listingService = new ListingService(new FakeListingRepository(), userService);
    requests =
        new RentalRequestService(new FakeRentalRequestRepository(), listingService, userService);
    reportService = new ReportService(userService, listingService, requests, null);
    User owner = userService.createUser("Владелец", "owner@example.com", null, UserRole.OWNER);
    renter = userService.createUser("Арендатор", "renter@example.com", null, UserRole.RENTER);
    listing =
        listingService.createListing(owner.getId(), "Дрель", "d", new BigDecimal("100"), "cat");
  }

  private RentalRequest book(int startDay, int endDay) {
    return requests.create(
        listing.getId(),
        renter.getId(),
        LocalDate.of(2026, 10, startDay),
        LocalDate.of(2026, 10, endDay));
  }

  @Test
  void collectStatistics_withoutRequests_returnsZeroes() {
    Statistics statistics = reportService.collectStatistics();

    assertEquals(2, statistics.totalUsers());
    assertEquals(1, statistics.totalListings());
    assertEquals(0, statistics.totalRequests());
    assertEquals(0, BigDecimal.ZERO.compareTo(statistics.averageTotalPrice()));
  }

  @Test
  void collectStatistics_countsRequestsByStatusAndAveragesTotalPrice() {
    book(1, 2);
    RentalRequest confirmed = book(2, 4);
    RentalRequest cancelled = book(4, 7);
    RentalRequest completed = book(7, 11);
    requests.changeStatus(confirmed.getId(), RentalRequestStatus.CONFIRMED);
    requests.changeStatus(cancelled.getId(), RentalRequestStatus.CANCELLED);
    requests.changeStatus(completed.getId(), RentalRequestStatus.CONFIRMED);
    requests.changeStatus(completed.getId(), RentalRequestStatus.ACTIVE);
    requests.changeStatus(completed.getId(), RentalRequestStatus.COMPLETED);

    Statistics statistics = reportService.collectStatistics();

    assertEquals(4, statistics.totalRequests());
    assertEquals(2, statistics.activeRequests());
    assertEquals(1, statistics.completedRequests());
    assertEquals(1, statistics.cancelledOrRejectedRequests());
    assertEquals(0, new BigDecimal("250.00").compareTo(statistics.averageTotalPrice()));
  }
}
