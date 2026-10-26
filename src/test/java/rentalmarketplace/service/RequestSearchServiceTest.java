package rentalmarketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.FakeListingRepository;
import rentalmarketplace.repository.FakeRentalRequestRepository;
import rentalmarketplace.repository.FakeUserRepository;

class RequestSearchServiceTest {
  private RentalRequestService service;
  private RequestSearchService search;
  private User ivan;
  private User sergey;
  private User anna;
  private User maria;
  private Listing drill;
  private Listing scooter;
  private Listing tent;
  private RentalRequest drillByAnna;
  private RentalRequest scooterByAnna;
  private RentalRequest scooterByMaria;
  private RentalRequest tentByMaria;

  @BeforeEach
  void setUp() {
    UserService users =
        new UserService(new FakeUserRepository(), rentalmarketplace.security.TestAccess.admin());
    ListingService listings = new ListingService(new FakeListingRepository(), users);
    service = new RentalRequestService(new FakeRentalRequestRepository(), listings, users);
    search = new RequestSearchService(service, listings, users);
    ivan = users.createUser("Иван Петров", "ivan@example.com", null, UserRole.OWNER);
    sergey = users.createUser("Сергей Ковалев", "sergey@example.com", null, UserRole.OWNER);
    anna = users.createUser("Анна Смирнова", "anna.smirnova@example.com", null, UserRole.RENTER);
    maria = users.createUser("Мария Иванова", "maria@example.com", null, UserRole.RENTER);
    drill =
        listings.createListing(
            ivan.getId(), "Перфоратор Bosch", "Мощный для ремонта", new BigDecimal("500"), "Инс");
    scooter =
        listings.createListing(
            sergey.getId(), "Электросамокат", "Складной", new BigDecimal("400"), "Транспорт");
    tent =
        listings.createListing(
            sergey.getId(), "Палатка", "Для похода", new BigDecimal("350"), "Спорт");
    drillByAnna = service.create(drill.getId(), anna.getId(), day(9, 1), day(9, 5));
    scooterByAnna = service.create(scooter.getId(), anna.getId(), day(9, 5), day(9, 7));
    scooterByMaria = service.create(scooter.getId(), maria.getId(), day(9, 10), day(9, 11));
    tentByMaria = service.create(tent.getId(), maria.getId(), day(8, 1), day(8, 10));
    service.changeStatus(scooterByAnna.getId(), RentalRequestStatus.CONFIRMED);
  }

  private LocalDate day(int month, int day) {
    return LocalDate.of(2026, month, day);
  }

  private RequestQuery query(
      String text,
      RentalRequestStatus status,
      Integer ownerId,
      LocalDate from,
      LocalDate to,
      RequestSort sort,
      boolean descending) {
    return new RequestQuery(text, status, ownerId, null, null, from, to, sort, descending);
  }

  private List<Integer> ids(List<RequestDetails> details) {
    return details.stream().map(RequestDetails::requestId).toList();
  }

  @Test
  void query_withoutRestrictions_returnsEveryRequestOrderedByIdWithNames() {
    List<RequestDetails> details = search.query(RequestQuery.all());

    assertEquals(
        List.of(
            drillByAnna.getId(),
            scooterByAnna.getId(),
            scooterByMaria.getId(),
            tentByMaria.getId()),
        ids(details));
    RequestDetails first = details.get(0);
    assertEquals("Перфоратор Bosch", first.listingTitle());
    assertEquals("Анна Смирнова", first.renterName());
    assertEquals("Иван Петров", first.ownerName());
    assertEquals(ivan.getId(), first.ownerId());
    assertEquals(new BigDecimal("2000"), first.totalPrice());
  }

  @Test
  void query_withoutRestrictions_returnsAll() {
    assertEquals(4, search.query(RequestQuery.all()).size());
  }

  @Test
  void query_textSearchesListingTitleDescriptionRenterAndOwner() {
    assertEquals(
        List.of(scooterByAnna.getId(), scooterByMaria.getId()),
        ids(search.query(query("самокат", null, null, null, null, null, false))));
    assertEquals(
        List.of(drillByAnna.getId()),
        ids(search.query(query("ремонта", null, null, null, null, null, false))));
    assertEquals(
        List.of(drillByAnna.getId(), scooterByAnna.getId()),
        ids(search.query(query("ANNA.smirnova", null, null, null, null, null, false))));
    assertEquals(
        List.of(scooterByAnna.getId(), scooterByMaria.getId(), tentByMaria.getId()),
        ids(search.query(query("  ковалев ", null, null, null, null, null, false))));
  }

  @Test
  void query_textWithoutMatches_returnsEmptyList() {
    assertTrue(search.query(query("ничего", null, null, null, null, null, false)).isEmpty());
  }

  @Test
  void query_filtersByStatusOwnerListingAndRenter() {
    assertEquals(
        List.of(scooterByAnna.getId()),
        ids(
            search.query(
                query(null, RentalRequestStatus.CONFIRMED, null, null, null, null, false))));
    assertEquals(
        List.of(drillByAnna.getId()),
        ids(search.query(query(null, null, ivan.getId(), null, null, null, false))));
    assertEquals(
        List.of(tentByMaria.getId()),
        ids(
            search.query(
                new RequestQuery(null, null, null, tent.getId(), null, null, null, null, false))));
    assertEquals(
        List.of(scooterByMaria.getId(), tentByMaria.getId()),
        ids(
            search.query(
                new RequestQuery(null, null, null, null, maria.getId(), null, null, null, false))));
  }

  @Test
  void query_periodIncludesRequestsOverlappingItsBoundaries() {
    assertEquals(
        List.of(drillByAnna.getId(), scooterByAnna.getId()),
        ids(search.query(query(null, null, null, day(9, 5), day(9, 5), null, false))));
    assertEquals(
        List.of(scooterByMaria.getId()),
        ids(search.query(query(null, null, null, day(9, 8), null, null, false))));
    assertEquals(
        List.of(tentByMaria.getId()),
        ids(search.query(query(null, null, null, null, day(8, 31), null, false))));
    assertTrue(
        search.query(query(null, null, null, day(10, 1), day(10, 31), null, false)).isEmpty());
  }

  @Test
  void query_combinesFilters() {
    assertEquals(
        List.of(scooterByAnna.getId()),
        ids(
            search.query(
                query(
                    "самокат",
                    RentalRequestStatus.CONFIRMED,
                    sergey.getId(),
                    day(9, 1),
                    day(9, 30),
                    null,
                    false))));
    assertTrue(
        search
            .query(query("самокат", RentalRequestStatus.COMPLETED, null, null, null, null, false))
            .isEmpty());
  }

  @Test
  void query_sortsByTotalPriceInBothDirections() {
    assertEquals(
        List.of(
            scooterByMaria.getId(),
            scooterByAnna.getId(),
            drillByAnna.getId(),
            tentByMaria.getId()),
        ids(search.query(query(null, null, null, null, null, RequestSort.TOTAL_PRICE, false))));
    assertEquals(
        List.of(
            tentByMaria.getId(),
            drillByAnna.getId(),
            scooterByAnna.getId(),
            scooterByMaria.getId()),
        ids(search.query(query(null, null, null, null, null, RequestSort.TOTAL_PRICE, true))));
  }

  @Test
  void query_sortsByStartDateAndListingTitleAndCreatedAt() {
    assertEquals(
        List.of(
            tentByMaria.getId(),
            drillByAnna.getId(),
            scooterByAnna.getId(),
            scooterByMaria.getId()),
        ids(search.query(query(null, null, null, null, null, RequestSort.START_DATE, false))));
    assertEquals(
        List.of(
            tentByMaria.getId(),
            drillByAnna.getId(),
            scooterByAnna.getId(),
            scooterByMaria.getId()),
        ids(search.query(query(null, null, null, null, null, RequestSort.LISTING_TITLE, false))));
    assertEquals(
        tentByMaria.getId(),
        ids(search.query(query(null, null, null, null, null, RequestSort.CREATED_AT, true)))
            .get(0));
  }

  @Test
  void allowedTransitions_followsStatusTable() {
    assertEquals(
        List.of(
            RentalRequestStatus.CONFIRMED,
            RentalRequestStatus.CANCELLED,
            RentalRequestStatus.REJECTED),
        service.allowedTransitions(RentalRequestStatus.NEW));
    assertEquals(
        List.of(RentalRequestStatus.ACTIVE, RentalRequestStatus.CANCELLED),
        service.allowedTransitions(RentalRequestStatus.CONFIRMED));
    assertEquals(
        List.of(RentalRequestStatus.COMPLETED, RentalRequestStatus.CANCELLED),
        service.allowedTransitions(RentalRequestStatus.ACTIVE));
    assertTrue(service.allowedTransitions(RentalRequestStatus.COMPLETED).isEmpty());
    assertTrue(service.allowedTransitions(RentalRequestStatus.CANCELLED).isEmpty());
    assertTrue(service.allowedTransitions(RentalRequestStatus.REJECTED).isEmpty());
  }

  @Test
  void canChangeDates_onlyForNewRequests() {
    assertTrue(service.canChangeDates(RentalRequestStatus.NEW));
    assertFalse(service.canChangeDates(RentalRequestStatus.CONFIRMED));
    assertFalse(service.canChangeDates(RentalRequestStatus.COMPLETED));
  }

  @Test
  void estimateTotal_multipliesPriceByDays() {
    assertEquals(
        new BigDecimal("2000"),
        service.estimateTotal(drill.getId(), day(9, 1), day(9, 5)).orElseThrow());
  }

  @Test
  void estimateTotal_withIncompleteOrInvalidDates_isEmpty() {
    assertTrue(service.estimateTotal(null, day(9, 1), day(9, 5)).isEmpty());
    assertTrue(service.estimateTotal(drill.getId(), null, day(9, 5)).isEmpty());
    assertTrue(service.estimateTotal(drill.getId(), day(9, 1), null).isEmpty());
    assertTrue(service.estimateTotal(drill.getId(), day(9, 5), day(9, 5)).isEmpty());
    assertTrue(service.estimateTotal(drill.getId(), day(9, 5), day(9, 1)).isEmpty());
  }
}
