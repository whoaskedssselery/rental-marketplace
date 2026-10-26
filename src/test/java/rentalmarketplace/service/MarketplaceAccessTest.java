package rentalmarketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import rentalmarketplace.security.AccessPolicy;

class MarketplaceAccessTest {
  private final FakeUserRepository userRepo = new FakeUserRepository();
  private User actor;
  private final AccessPolicy access = new AccessPolicy(() -> actor);
  private final UserService users = new UserService(userRepo, access);
  private final ListingService listings = new ListingService(new FakeListingRepository(), users);
  private final FakeRentalRequestRepository requestRepo = new FakeRentalRequestRepository();
  private final RentalRequestService requests =
      new RentalRequestService(requestRepo, listings, users);
  private final ReviewService reviews =
      new ReviewService(new FakeReviewRepository(), requests, listings, users);
  private final User owner = account("Owner", UserRole.RENTER);
  private final User renter = account("Renter", UserRole.OWNER);
  private final User outsider = account("Outsider", UserRole.RENTER);
  private final User admin = account("Admin", UserRole.ADMIN);

  private User account(String name, UserRole role) {
    return userRepo.save(new User(name, name + "@example.com", null, role));
  }

  private Listing listing() {
    actor = owner;
    return listings.createListing(owner.getId(), "Drill", null, BigDecimal.TEN, "Tools");
  }

  private RentalRequest request(Listing listing) {
    actor = renter;
    return requests.create(
        listing.getId(), renter.getId(), LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 3));
  }

  @Test
  void everyMemberCanPublishAndRentButCannotModifyOtherListingsOrTransferOwnership() {
    Listing item = listing();
    actor = renter;
    assertEquals(1, listings.getAllListings().size());
    assertThrows(BusinessRuleException.class, () -> listings.deleteListing(item.getId()));
    assertThrows(
        BusinessRuleException.class,
        () ->
            listings.updateListing(
                item.getId(), renter.getId(), "stolen", null, BigDecimal.TEN, "Tools", true));
    assertThrows(
        BusinessRuleException.class,
        () -> listings.createListing(owner.getId(), "fake", null, BigDecimal.TEN, "Tools"));
    listings.createListing(renter.getId(), "Own", null, BigDecimal.TEN, "Tools");
    request(item);
    actor = owner;
    assertThrows(
        BusinessRuleException.class,
        () ->
            listings.updateListing(
                item.getId(), renter.getId(), "transfer", null, BigDecimal.TEN, "Tools", true));
  }

  @Test
  void requestVisibilityAndActionsDependOnParticipation() {
    var item = listing();
    var booking = request(item);
    assertEquals(1, requests.getAll().size());
    assertTrue(requests.canEdit(booking.getId()));
    requests.updateDates(booking.getId(), LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 4));
    assertThrows(
        BusinessRuleException.class,
        () -> requests.changeStatus(booking.getId(), RentalRequestStatus.CONFIRMED));
    assertEquals(
        java.util.List.of(RentalRequestStatus.CANCELLED),
        requests.allowedTransitions(booking.getId()));
    actor = outsider;
    assertTrue(requests.getAll().isEmpty());
    assertThrows(BusinessRuleException.class, () -> requests.getById(booking.getId()));
    assertThrows(BusinessRuleException.class, () -> requests.delete(booking.getId()));
    assertThrows(
        BusinessRuleException.class,
        () ->
            requests.create(
                item.getId(), renter.getId(), LocalDate.of(2030, 2, 1), LocalDate.of(2030, 2, 2)));
    assertThrows(
        BusinessRuleException.class,
        () ->
            requests.create(
                item.getId(),
                outsider.getId(),
                LocalDate.of(2030, 1, 1),
                LocalDate.of(2030, 1, 2)));
    actor = owner;
    assertFalse(requests.canEdit(booking.getId()));
    assertThrows(
        BusinessRuleException.class,
        () ->
            requests.updateDates(
                booking.getId(), LocalDate.of(2030, 1, 5), LocalDate.of(2030, 1, 6)));
    assertThrows(BusinessRuleException.class, () -> requests.delete(booking.getId()));
    requests.changeStatus(booking.getId(), RentalRequestStatus.CONFIRMED);
    requests.changeStatus(booking.getId(), RentalRequestStatus.ACTIVE);
    requests.changeStatus(booking.getId(), RentalRequestStatus.COMPLETED);
    assertThrows(
        BusinessRuleException.class, () -> reviews.create(booking.getId(), 5, "self review"));
    actor = renter;
    var review = reviews.create(booking.getId(), 5, "Good");
    actor = outsider;
    assertEquals(1, reviews.getDetails().size());
    assertEquals(
        new BigDecimal("5.0"), reviews.averageRatingForListing(item.getId()).orElseThrow());
    assertTrue(reviews.getReviewableRequests().isEmpty());
    assertThrows(BusinessRuleException.class, () -> reviews.update(review.getId(), 1, "fake"));
    assertThrows(BusinessRuleException.class, () -> reviews.delete(review.getId()));
    actor = renter;
    assertEquals(4, reviews.update(review.getId(), 4, "Updated").getRating());
    actor = admin;
    assertEquals(1, requests.getAll().size());
    reviews.delete(review.getId());
  }

  @Test
  void renterCanCancelTheirOwnBooking() {
    var item = listing();
    var booking = request(item);
    requests.changeStatus(booking.getId(), RentalRequestStatus.CANCELLED);
    assertFalse(requests.canEdit(booking.getId()));
    assertTrue(requests.canDelete(booking.getId()));
    requests.delete(booking.getId());
    assertTrue(requests.getAll().isEmpty());
  }

  @Test
  void profileChangesCannotGrantRolesAndSignedOutAccessIsDenied() {
    actor = owner;
    assertEquals(1, users.getVisibleUsers().size());
    users.updateUser(owner.getId(), "Updated", owner.getEmail(), null, owner.getRole());
    assertThrows(
        BusinessRuleException.class,
        () -> users.updateUser(owner.getId(), "x", owner.getEmail(), null, UserRole.ADMIN));
    assertThrows(
        BusinessRuleException.class,
        () -> users.updateUser(renter.getId(), "x", renter.getEmail(), null, renter.getRole()));
    assertThrows(
        BusinessRuleException.class,
        () -> users.createUser("x", "new@example.com", null, UserRole.ADMIN));
    assertThrows(BusinessRuleException.class, () -> users.deleteUser(renter.getId()));
    actor = null;
    assertThrows(BusinessRuleException.class, listings::getAllListings);
    assertThrows(BusinessRuleException.class, reviews::getAll);
    assertThrows(BusinessRuleException.class, requests::getAll);
    assertThrows(BusinessRuleException.class, users::getAllUsers);
  }
}
