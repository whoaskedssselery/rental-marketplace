package rentalmarketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import rentalmarketplace.model.Review;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.FakeListingRepository;
import rentalmarketplace.repository.FakeRentalRequestRepository;
import rentalmarketplace.repository.FakeReviewRepository;
import rentalmarketplace.repository.FakeUserRepository;

class ReviewServiceTest {
  private RentalRequestService requestService;
  private ReviewService service;
  private User renter;
  private Listing drill;
  private Listing scooter;

  @BeforeEach
  void setUp() {
    UserService userService = new UserService(new FakeUserRepository());
    ListingService listingService = new ListingService(new FakeListingRepository(), userService);
    requestService =
        new RentalRequestService(new FakeRentalRequestRepository(), listingService, userService);
    service =
        new ReviewService(new FakeReviewRepository(), requestService, listingService, userService);
    User owner = userService.createUser("Владелец", "owner@example.com", null, UserRole.OWNER);
    renter = userService.createUser("Иван Петров", "ivan@example.com", null, UserRole.RENTER);
    drill =
        listingService.createListing(
            owner.getId(), "Дрель", "мощная", new BigDecimal("1000.00"), "Инструменты");
    scooter =
        listingService.createListing(
            owner.getId(), "Самокат", "быстрый", new BigDecimal("400.00"), "Транспорт");
  }

  private RentalRequest completedRequest(Listing listing, int startDay) {
    RentalRequest request =
        requestService.create(
            listing.getId(),
            renter.getId(),
            LocalDate.of(2026, 9, startDay),
            LocalDate.of(2026, 9, startDay + 2));
    requestService.changeStatus(request.getId(), RentalRequestStatus.CONFIRMED);
    requestService.changeStatus(request.getId(), RentalRequestStatus.ACTIVE);
    return requestService.changeStatus(request.getId(), RentalRequestStatus.COMPLETED);
  }

  @Test
  void create_withValidData_savesReview() {
    RentalRequest request = completedRequest(drill, 1);

    Review saved = service.create(request.getId(), 5, "  Отлично  ");

    assertNotNull(saved.getId());
    assertEquals(request.getId(), saved.getRequestId());
    assertEquals(5, saved.getRating());
    assertEquals("Отлично", saved.getComment());
  }

  @Test
  void create_withBlankComment_storesNull() {
    RentalRequest request = completedRequest(drill, 1);

    assertNull(service.create(request.getId(), 4, "   ").getComment());
    assertNull(service.getById(1).getComment());
  }

  @Test
  void create_forMissingRequest_throwsEntityNotFound() {
    assertThrows(EntityNotFoundException.class, () -> service.create(999, 5, "x"));
  }

  @Test
  void create_forNotCompletedRequest_throwsBusinessRule() {
    RentalRequest request =
        requestService.create(
            drill.getId(), renter.getId(), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3));

    assertThrows(BusinessRuleException.class, () -> service.create(request.getId(), 5, "x"));
  }

  @Test
  void create_secondReviewForSameRequest_throwsBusinessRule() {
    RentalRequest request = completedRequest(drill, 1);
    service.create(request.getId(), 5, "x");

    assertThrows(BusinessRuleException.class, () -> service.create(request.getId(), 3, "y"));
  }

  @Test
  void create_withRatingOutOfRange_throwsBusinessRule() {
    RentalRequest request = completedRequest(drill, 1);

    assertThrows(BusinessRuleException.class, () -> service.create(request.getId(), 0, "x"));
    assertThrows(BusinessRuleException.class, () -> service.create(request.getId(), 6, "x"));
  }

  @Test
  void create_commentLengthBoundary() {
    RentalRequest request = completedRequest(drill, 1);

    assertThrows(
        BusinessRuleException.class, () -> service.create(request.getId(), 5, "a".repeat(1001)));
    assertEquals(
        "a".repeat(1000), service.create(request.getId(), 5, "a".repeat(1000)).getComment());
  }

  @Test
  void getById_missing_throwsEntityNotFound() {
    assertThrows(EntityNotFoundException.class, () -> service.getById(42));
  }

  @Test
  void update_changesRatingAndComment() {
    Review review = service.create(completedRequest(drill, 1).getId(), 3, "норм");

    Review updated = service.update(review.getId(), 5, "лучше некуда");

    assertEquals(5, updated.getRating());
    assertEquals("лучше некуда", service.getById(review.getId()).getComment());
  }

  @Test
  void update_withInvalidRatingOrMissingId_fails() {
    Review review = service.create(completedRequest(drill, 1).getId(), 3, "норм");

    assertThrows(BusinessRuleException.class, () -> service.update(review.getId(), 7, "x"));
    assertThrows(EntityNotFoundException.class, () -> service.update(999, 3, "x"));
  }

  @Test
  void delete_removesReview() {
    Review review = service.create(completedRequest(drill, 1).getId(), 3, "норм");

    service.delete(review.getId());

    assertTrue(service.getAll().isEmpty());
    assertThrows(EntityNotFoundException.class, () -> service.delete(review.getId()));
  }

  @Test
  void getDetails_collectsListingAndRenterNames() {
    RentalRequest request = completedRequest(drill, 1);
    service.create(request.getId(), 4, "хорошо");

    ReviewDetails details = service.getDetails().get(0);

    assertEquals("Дрель", details.listingTitle());
    assertEquals("Иван Петров", details.renterName());
    assertEquals(drill.getId(), details.listingId());
    assertEquals(4, details.rating());
  }

  @Test
  void getReviewableRequests_returnsOnlyCompletedWithoutReview() {
    RentalRequest reviewed = completedRequest(drill, 1);
    RentalRequest free = completedRequest(scooter, 10);
    requestService.create(
        drill.getId(), renter.getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));
    service.create(reviewed.getId(), 5, "x");

    List<ReviewableRequest> available = service.getReviewableRequests();

    assertEquals(1, available.size());
    assertEquals(free.getId(), available.get(0).requestId());
    assertEquals("Самокат", available.get(0).listingTitle());
    assertTrue(available.get(0).toString().contains("Самокат"));
  }

  @Test
  void averageRatingForListing_roundsToOneDecimal() {
    service.create(completedRequest(drill, 1).getId(), 5, null);
    service.create(completedRequest(drill, 10).getId(), 4, null);
    service.create(completedRequest(drill, 20).getId(), 4, null);
    service.create(completedRequest(scooter, 1).getId(), 1, null);

    assertEquals(
        new BigDecimal("4.3"), service.averageRatingForListing(drill.getId()).orElseThrow());
    assertEquals(
        new BigDecimal("1.0"), service.averageRatingForListing(scooter.getId()).orElseThrow());
  }

  @Test
  void averageRatingForListing_withoutReviews_isEmpty() {
    completedRequest(drill, 1);

    assertTrue(service.averageRatingForListing(drill.getId()).isEmpty());
  }
}
