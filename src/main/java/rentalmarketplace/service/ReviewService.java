package rentalmarketplace.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.Review;
import rentalmarketplace.model.User;
import rentalmarketplace.repository.ReviewRepository;

public class ReviewService {
  private static final int MIN_RATING = 1;
  private static final int MAX_RATING = 5;
  private static final int COMMENT_MAX_LENGTH = 1000;

  private final ReviewRepository reviewRepository;
  private final RentalRequestService rentalRequestService;
  private final ListingService listingService;
  private final UserService userService;

  public ReviewService(
      ReviewRepository reviewRepository,
      RentalRequestService rentalRequestService,
      ListingService listingService,
      UserService userService) {
    this.reviewRepository = reviewRepository;
    this.rentalRequestService = rentalRequestService;
    this.listingService = listingService;
    this.userService = userService;
  }

  public Review create(Integer requestId, int rating, String comment) {
    RentalRequest request = rentalRequestService.getById(requestId);
    if (request.getStatus() != RentalRequestStatus.COMPLETED) {
      throw new BusinessRuleException("Отзыв можно оставить только на завершенную заявку");
    }
    if (reviewRepository.findByRequestId(requestId).isPresent()) {
      throw new BusinessRuleException("На эту заявку отзыв уже оставлен");
    }
    validateRating(rating);
    return reviewRepository.save(new Review(requestId, rating, normalizeComment(comment)));
  }

  public Review getById(Integer id) {
    return reviewRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Отзыв с id=" + id + " не найден"));
  }

  public List<Review> getAll() {
    return reviewRepository.findAll();
  }

  public Review update(Integer id, int rating, String comment) {
    Review review = getById(id);
    validateRating(rating);
    review.setRating(rating);
    review.setComment(normalizeComment(comment));
    return reviewRepository.update(review);
  }

  public void delete(Integer id) {
    getById(id);
    reviewRepository.deleteById(id);
  }

  public List<ReviewDetails> getDetails() {
    Map<Integer, RentalRequest> requestsById =
        rentalRequestService.getAll().stream()
            .collect(Collectors.toMap(RentalRequest::getId, Function.identity()));
    Map<Integer, Listing> listingsById = listingsById();
    Map<Integer, User> usersById = usersById();
    return getAll().stream()
        .map(review -> toDetails(review, requestsById, listingsById, usersById))
        .toList();
  }

  public List<ReviewDetails> query(ReviewQuery query) {
    String text = query.text() == null ? "" : query.text().trim().toLowerCase();
    Comparator<ReviewDetails> byId = Comparator.comparing(ReviewDetails::reviewId);
    Comparator<ReviewDetails> order = byId;
    if (query.sort() == ReviewSort.CREATED_AT) {
      order =
          Comparator.comparing(
                  ReviewDetails::createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
              .thenComparing(byId);
    } else if (query.sort() == ReviewSort.RATING) {
      order = Comparator.comparingInt(ReviewDetails::rating).thenComparing(byId);
    }
    return getDetails().stream()
        .filter(details -> query.rating() == null || details.rating() == query.rating())
        .filter(details -> text.isEmpty() || matchesText(details, text))
        .sorted(query.descending() ? order.reversed() : order)
        .toList();
  }

  public List<ReviewableRequest> getReviewableRequests() {
    Set<Integer> reviewedRequestIds =
        getAll().stream().map(Review::getRequestId).collect(Collectors.toSet());
    Map<Integer, Listing> listingsById = listingsById();
    Map<Integer, User> usersById = usersById();
    return rentalRequestService.getAll().stream()
        .filter(request -> request.getStatus() == RentalRequestStatus.COMPLETED)
        .filter(request -> !reviewedRequestIds.contains(request.getId()))
        .map(
            request ->
                new ReviewableRequest(
                    request.getId(),
                    listingsById.get(request.getListingId()).getTitle(),
                    usersById.get(request.getRenterId()).getFullName(),
                    request.getStartDate(),
                    request.getEndDate()))
        .toList();
  }

  public Optional<BigDecimal> averageRatingForListing(Integer listingId) {
    Set<Integer> requestIds =
        rentalRequestService.getAll().stream()
            .filter(request -> request.getListingId().equals(listingId))
            .map(RentalRequest::getId)
            .collect(Collectors.toSet());
    List<Review> reviews =
        getAll().stream().filter(review -> requestIds.contains(review.getRequestId())).toList();
    if (reviews.isEmpty()) {
      return Optional.empty();
    }
    int sum = reviews.stream().mapToInt(Review::getRating).sum();
    return Optional.of(
        BigDecimal.valueOf(sum)
            .divide(BigDecimal.valueOf(reviews.size()), 1, RoundingMode.HALF_UP));
  }

  private boolean matchesText(ReviewDetails details, String text) {
    return contains(details.listingTitle(), text)
        || contains(details.renterName(), text)
        || contains(details.comment(), text);
  }

  private boolean contains(String value, String lowerCaseText) {
    return value != null && value.toLowerCase().contains(lowerCaseText);
  }

  private ReviewDetails toDetails(
      Review review,
      Map<Integer, RentalRequest> requestsById,
      Map<Integer, Listing> listingsById,
      Map<Integer, User> usersById) {
    RentalRequest request = requestsById.get(review.getRequestId());
    Listing listing = listingsById.get(request.getListingId());
    User renter = usersById.get(request.getRenterId());
    return new ReviewDetails(
        review.getId(),
        review.getRequestId(),
        listing.getId(),
        listing.getTitle(),
        renter.getId(),
        renter.getFullName(),
        review.getRating(),
        review.getComment(),
        review.getCreatedAt());
  }

  private Map<Integer, Listing> listingsById() {
    return listingService.getAllListings().stream()
        .collect(Collectors.toMap(Listing::getId, Function.identity()));
  }

  private Map<Integer, User> usersById() {
    return userService.getAllUsers().stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));
  }

  private void validateRating(int rating) {
    if (rating < MIN_RATING || rating > MAX_RATING) {
      throw new BusinessRuleException("Оценка должна быть от " + MIN_RATING + " до " + MAX_RATING);
    }
  }

  private String normalizeComment(String comment) {
    if (comment == null || comment.isBlank()) {
      return null;
    }
    String text = comment.trim();
    if (text.length() > COMMENT_MAX_LENGTH) {
      throw new BusinessRuleException(
          "Комментарий не может быть длиннее " + COMMENT_MAX_LENGTH + " символов");
    }
    return text;
  }
}
