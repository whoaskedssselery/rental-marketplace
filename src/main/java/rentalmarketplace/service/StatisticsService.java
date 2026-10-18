package rentalmarketplace.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.Review;

public class StatisticsService {
  private static final Set<RentalRequestStatus> ACTIVE =
      Set.of(RentalRequestStatus.NEW, RentalRequestStatus.CONFIRMED, RentalRequestStatus.ACTIVE);
  private static final Set<RentalRequestStatus> CLOSED_UNSUCCESSFULLY =
      Set.of(RentalRequestStatus.CANCELLED, RentalRequestStatus.REJECTED);

  private final UserService userService;
  private final ListingService listingService;
  private final RentalRequestService rentalRequestService;
  private final ReviewService reviewService;

  public StatisticsService(
      UserService userService,
      ListingService listingService,
      RentalRequestService rentalRequestService,
      ReviewService reviewService) {
    this.userService = userService;
    this.listingService = listingService;
    this.rentalRequestService = rentalRequestService;
    this.reviewService = reviewService;
  }

  public StatisticsReport collect() {
    List<RentalRequest> requests = rentalRequestService.getAll();
    List<Listing> listings = listingService.getAllListings();
    List<Review> reviews = reviewService.getAll();
    return new StatisticsReport(
        userService.getAllUsers().size(),
        listings.size(),
        requests.size(),
        reviews.size(),
        countWith(requests, ACTIVE),
        countWith(requests, Set.of(RentalRequestStatus.COMPLETED)),
        countWith(requests, CLOSED_UNSUCCESSFULLY),
        average(requests),
        extreme(requests, true),
        extreme(requests, false),
        share(countWith(requests, CLOSED_UNSUCCESSFULLY), requests.size()),
        averageRating(reviews),
        byStatus(requests),
        byCategory(listings));
  }

  public long countRequestsInPeriod(LocalDate from, LocalDate to) {
    if (from == null || to == null) {
      throw new BusinessRuleException("Укажите начало и конец периода");
    }
    if (to.isBefore(from)) {
      throw new BusinessRuleException("Конец периода не может быть раньше его начала");
    }
    return rentalRequestService.getAll().stream()
        .filter(request -> !request.getEndDate().isBefore(from))
        .filter(request -> !request.getStartDate().isAfter(to))
        .count();
  }

  private long countWith(List<RentalRequest> requests, Set<RentalRequestStatus> statuses) {
    return requests.stream().filter(request -> statuses.contains(request.getStatus())).count();
  }

  private BigDecimal average(List<RentalRequest> requests) {
    if (requests.isEmpty()) {
      return BigDecimal.ZERO.setScale(2);
    }
    BigDecimal sum =
        requests.stream()
            .map(RentalRequest::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return sum.divide(BigDecimal.valueOf(requests.size()), 2, RoundingMode.HALF_UP);
  }

  private BigDecimal extreme(List<RentalRequest> requests, boolean minimum) {
    return requests.stream()
        .map(RentalRequest::getTotalPrice)
        .reduce((a, b) -> (a.compareTo(b) <= 0) == minimum ? a : b)
        .orElse(BigDecimal.ZERO.setScale(2));
  }

  private BigDecimal share(long part, int total) {
    if (total == 0) {
      return BigDecimal.ZERO.setScale(1);
    }
    return BigDecimal.valueOf(part * 100)
        .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
  }

  private BigDecimal averageRating(List<Review> reviews) {
    if (reviews.isEmpty()) {
      return null;
    }
    int sum = reviews.stream().mapToInt(Review::getRating).sum();
    return BigDecimal.valueOf(sum)
        .divide(BigDecimal.valueOf(reviews.size()), 1, RoundingMode.HALF_UP);
  }

  private Map<RentalRequestStatus, Long> byStatus(List<RentalRequest> requests) {
    Map<RentalRequestStatus, Long> counts = new EnumMap<>(RentalRequestStatus.class);
    for (RentalRequestStatus status : RentalRequestStatus.values()) {
      counts.put(status, 0L);
    }
    requests.forEach(request -> counts.merge(request.getStatus(), 1L, Long::sum));
    return counts;
  }

  private Map<String, Long> byCategory(List<Listing> listings) {
    Map<String, Long> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    listings.forEach(listing -> counts.merge(listing.getCategory(), 1L, Long::sum));
    return counts;
  }
}
