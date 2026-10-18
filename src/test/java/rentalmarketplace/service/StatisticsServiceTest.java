package rentalmarketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.FakeListingRepository;
import rentalmarketplace.repository.FakeRentalRequestRepository;
import rentalmarketplace.repository.FakeReviewRepository;
import rentalmarketplace.repository.FakeUserRepository;

class StatisticsServiceTest {
  private UserService users;
  private ListingService listings;
  private RentalRequestService requests;
  private ReviewService reviews;
  private StatisticsService statistics;
  private User owner;
  private User renter;
  private Listing drill;
  private Listing tent;

  @BeforeEach
  void setUp() {
    users = new UserService(new FakeUserRepository());
    listings = new ListingService(new FakeListingRepository(), users);
    requests = new RentalRequestService(new FakeRentalRequestRepository(), listings, users);
    reviews = new ReviewService(new FakeReviewRepository(), requests, listings, users);
    statistics = new StatisticsService(users, listings, requests, reviews);
    owner = users.createUser("Владелец", "owner@example.com", null, UserRole.OWNER);
    renter = users.createUser("Арендатор", "renter@example.com", null, UserRole.RENTER);
  }

  private void seed() {
    drill =
        listings.createListing(owner.getId(), "Дрель", "d", new BigDecimal("500"), "Инструменты");
    tent = listings.createListing(owner.getId(), "Палатка", "d", new BigDecimal("350"), "Спорт");
    listings.createListing(owner.getId(), "Лопата", "d", new BigDecimal("100"), "инструменты");
    RentalRequest completed = book(drill, 1, 5);
    requests.changeStatus(completed.getId(), RentalRequestStatus.CONFIRMED);
    requests.changeStatus(completed.getId(), RentalRequestStatus.ACTIVE);
    requests.changeStatus(completed.getId(), RentalRequestStatus.COMPLETED);
    RentalRequest cancelled = book(tent, 10, 12);
    requests.changeStatus(cancelled.getId(), RentalRequestStatus.CANCELLED);
    book(tent, 20, 22);
    book(drill, 25, 26);
    reviews.create(completed.getId(), 4, null);
  }

  private RentalRequest book(Listing listing, int start, int end) {
    return requests.create(
        listing.getId(), renter.getId(), LocalDate.of(2026, 9, start), LocalDate.of(2026, 9, end));
  }

  @Test
  void collect_countsEntitiesAndStatuses() {
    seed();

    StatisticsReport report = statistics.collect();

    assertEquals(2, report.totalUsers());
    assertEquals(3, report.totalListings());
    assertEquals(4, report.totalRequests());
    assertEquals(1, report.totalReviews());
    assertEquals(2, report.activeRequests());
    assertEquals(1, report.completedRequests());
    assertEquals(1, report.cancelledOrRejectedRequests());
    assertEquals(2L, report.requestsByStatus().get(RentalRequestStatus.NEW));
    assertEquals(0L, report.requestsByStatus().get(RentalRequestStatus.REJECTED));
  }

  @Test
  void collect_calculatesPriceStatisticsAndShare() {
    seed();

    StatisticsReport report = statistics.collect();

    assertEquals(new BigDecimal("2000"), report.maxTotalPrice());
    assertEquals(new BigDecimal("500"), report.minTotalPrice());
    assertEquals(new BigDecimal("975.00"), report.averageTotalPrice());
    assertEquals(new BigDecimal("25.0"), report.cancelledSharePercent());
    assertEquals(new BigDecimal("4.0"), report.averageRating());
  }

  @Test
  void collect_groupsListingsByCategoryIgnoringCase() {
    seed();

    assertEquals(2L, statistics.collect().listingsByCategory().get("Инструменты"));
    assertEquals(1L, statistics.collect().listingsByCategory().get("Спорт"));
  }

  @Test
  void collect_onEmptyDatabase_hasZerosAndNoDivisionByZero() {
    StatisticsReport report = statistics.collect();

    assertEquals(0, report.totalRequests());
    assertEquals(new BigDecimal("0.00"), report.averageTotalPrice());
    assertEquals(new BigDecimal("0.00"), report.minTotalPrice());
    assertEquals(new BigDecimal("0.00"), report.maxTotalPrice());
    assertEquals(new BigDecimal("0.0"), report.cancelledSharePercent());
    assertNull(report.averageRating());
    assertEquals(RentalRequestStatus.values().length, report.requestsByStatus().size());
  }

  @Test
  void countRequestsInPeriod_includesBoundaries() {
    seed();

    assertEquals(1, statistics.countRequestsInPeriod(day(9, 5), day(9, 5)));
    assertEquals(2, statistics.countRequestsInPeriod(day(9, 1), day(9, 12)));
    assertEquals(4, statistics.countRequestsInPeriod(day(9, 1), day(9, 30)));
    assertEquals(0, statistics.countRequestsInPeriod(day(10, 1), day(10, 31)));
  }

  @Test
  void countRequestsInPeriod_rejectsMissingOrReversedPeriod() {
    assertThrows(
        BusinessRuleException.class, () -> statistics.countRequestsInPeriod(null, day(9, 1)));
    assertThrows(
        BusinessRuleException.class, () -> statistics.countRequestsInPeriod(day(9, 5), day(9, 1)));
  }

  private LocalDate day(int month, int day) {
    return LocalDate.of(2026, month, day);
  }
}
