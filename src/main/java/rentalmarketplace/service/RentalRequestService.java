package rentalmarketplace.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.User;
import rentalmarketplace.repository.RentalRequestRepository;

public class RentalRequestService {
  private final RentalRequestRepository rentalRequestRepository;
  private final ListingService listingService;
  private final UserService userService;

  public RentalRequestService(
      RentalRequestRepository rentalRequestRepository,
      ListingService listingService,
      UserService userService) {
    this.rentalRequestRepository = rentalRequestRepository;
    this.listingService = listingService;
    this.userService = userService;
  }

  public RentalRequest create(
      Integer listingId, Integer renterId, LocalDate startDate, LocalDate endDate) {
    Listing listing = listingService.getListingById(listingId);
    userService.getUserById(renterId);

    validateDates(startDate, endDate);

    if (!listing.isAvailable()) {
      throw new BusinessRuleException("Объект аренды недоступен для бронирования");
    }

    if (listing.getOwnerId().equals(renterId)) {
      throw new BusinessRuleException("Арендатор не может быть владельцем объекта");
    }

    checkOverlap(listingId, startDate, endDate, null);

    BigDecimal totalPrice = calculateTotal(listing, startDate, endDate);
    RentalRequest request =
        new RentalRequest(
            listingId, renterId, startDate, endDate, RentalRequestStatus.NEW, totalPrice);
    return rentalRequestRepository.save(request);
  }

  public RentalRequest getById(Integer id) {
    return rentalRequestRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Заявка с id=" + id + " не найдена"));
  }

  public List<RentalRequest> getAll() {
    return rentalRequestRepository.findAll();
  }

  public RentalRequest updateDates(
      Integer requestId, LocalDate newStartDate, LocalDate newEndDate) {
    RentalRequest request = getById(requestId);

    if (!canChangeDates(request.getStatus())) {
      throw new BusinessRuleException(
          "Менять даты можно только у заявок в статусе NEW (текущий: " + request.getStatus() + ")");
    }
    validateDates(newStartDate, newEndDate);
    checkOverlap(request.getListingId(), newStartDate, newEndDate, requestId);

    Listing listing = listingService.getListingById(request.getListingId());
    BigDecimal newTotal = calculateTotal(listing, newStartDate, newEndDate);

    request.setStartDate(newStartDate);
    request.setEndDate(newEndDate);
    request.setTotalPrice(newTotal);
    return rentalRequestRepository.update(request);
  }

  public RentalRequest changeStatus(Integer requestId, RentalRequestStatus newStatus) {
    RentalRequest request = getById(requestId);
    RentalRequestStatus current = request.getStatus();
    if (!isTransitionAllowed(current, newStatus)) {
      throw new BusinessRuleException("Переход " + current + " -> " + newStatus + " запрещён");
    }
    request.setStatus(newStatus);
    return rentalRequestRepository.update(request);
  }

  public void delete(Integer id) {
    getById(id);
    rentalRequestRepository.deleteById(id);
  }

  public boolean canChangeDates(RentalRequestStatus status) {
    return status == RentalRequestStatus.NEW;
  }

  public Optional<BigDecimal> estimateTotal(
      Integer listingId, LocalDate startDate, LocalDate endDate) {
    if (listingId == null || startDate == null || endDate == null || !endDate.isAfter(startDate)) {
      return Optional.empty();
    }
    Listing listing = listingService.getListingById(listingId);
    return Optional.of(calculateTotal(listing, startDate, endDate));
  }

  public List<RequestDetails> query(RequestQuery query) {
    Map<Integer, Listing> listingsById =
        listingService.getAllListings().stream()
            .collect(Collectors.toMap(Listing::getId, Function.identity()));
    Map<Integer, User> usersById =
        userService.getAllUsers().stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
    String text = normalize(query.text());
    Comparator<RequestDetails> order = comparatorFor(query);
    return getAll().stream()
        .map(request -> toDetails(request, listingsById, usersById))
        .filter(details -> matchesQuery(details, listingsById, text, query))
        .sorted(query.descending() ? order.reversed() : order)
        .toList();
  }

  public List<RentalRequestStatus> allowedTransitions(RentalRequestStatus status) {
    return Arrays.stream(RentalRequestStatus.values())
        .filter(next -> isTransitionAllowed(status, next))
        .toList();
  }

  private RequestDetails toDetails(
      RentalRequest request, Map<Integer, Listing> listingsById, Map<Integer, User> usersById) {
    Listing listing = listingsById.get(request.getListingId());
    User renter = usersById.get(request.getRenterId());
    User owner = usersById.get(listing.getOwnerId());
    return new RequestDetails(
        request.getId(),
        listing.getId(),
        listing.getTitle(),
        renter.getId(),
        renter.getFullName(),
        renter.getEmail(),
        owner.getId(),
        owner.getFullName(),
        request.getStartDate(),
        request.getEndDate(),
        request.getStatus(),
        request.getTotalPrice(),
        request.getCreatedAt());
  }

  private boolean matchesQuery(
      RequestDetails details, Map<Integer, Listing> listingsById, String text, RequestQuery query) {
    return (query.status() == null || details.status() == query.status())
        && (query.ownerId() == null || details.ownerId().equals(query.ownerId()))
        && (query.listingId() == null || details.listingId().equals(query.listingId()))
        && (query.renterId() == null || details.renterId().equals(query.renterId()))
        && (query.periodFrom() == null || !details.endDate().isBefore(query.periodFrom()))
        && (query.periodTo() == null || !details.startDate().isAfter(query.periodTo()))
        && (text.isEmpty() || matchesText(details, listingsById.get(details.listingId()), text));
  }

  private boolean matchesText(RequestDetails details, Listing listing, String text) {
    return containsIgnoreCase(details.listingTitle(), text)
        || containsIgnoreCase(listing.getDescription(), text)
        || containsIgnoreCase(details.renterName(), text)
        || containsIgnoreCase(details.renterEmail(), text)
        || containsIgnoreCase(details.ownerName(), text);
  }

  private Comparator<RequestDetails> comparatorFor(RequestQuery query) {
    Comparator<RequestDetails> byId = Comparator.comparing(RequestDetails::requestId);
    if (query.sort() == null) {
      return byId;
    }
    Comparator<RequestDetails> main =
        switch (query.sort()) {
          case CREATED_AT ->
              Comparator.comparing(
                  RequestDetails::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
          case TOTAL_PRICE -> Comparator.comparing(RequestDetails::totalPrice);
          case START_DATE -> Comparator.comparing(RequestDetails::startDate);
          case LISTING_TITLE ->
              Comparator.comparing(RequestDetails::listingTitle, String.CASE_INSENSITIVE_ORDER);
        };
    return main.thenComparing(byId);
  }

  private String normalize(String query) {
    return query == null ? "" : query.trim().toLowerCase();
  }

  private boolean containsIgnoreCase(String value, String lowerCaseText) {
    return value != null && value.toLowerCase().contains(lowerCaseText);
  }

  private void validateDates(LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null) {
      throw new BusinessRuleException("Даты начала и окончания обязательны");
    }
    if (!endDate.isAfter(startDate)) {
      throw new BusinessRuleException("Дата окончания должна быть позже даты начала");
    }
  }

  private BigDecimal calculateTotal(Listing listing, LocalDate startDate, LocalDate endDate) {
    long days = ChronoUnit.DAYS.between(startDate, endDate);
    if (days <= 0) {
      throw new BusinessRuleException("Количество дней аренды должно быть положительным");
    }
    return listing.getPricePerDay().multiply(BigDecimal.valueOf(days));
  }

  private void checkOverlap(
      Integer listingId, LocalDate startDate, LocalDate endDate, Integer excludeRequestId) {
    List<RentalRequest> activeRequests = rentalRequestRepository.findActiveByListingId(listingId);
    for (RentalRequest other : activeRequests) {
      if (excludeRequestId != null && excludeRequestId.equals(other.getId())) {
        continue;
      }
      boolean overlaps =
          startDate.isBefore(other.getEndDate()) && endDate.isAfter(other.getStartDate());
      if (overlaps) {
        throw new BusinessRuleException("Даты пересекаются с активной заявкой id=" + other.getId());
      }
    }
  }

  private boolean isTransitionAllowed(RentalRequestStatus from, RentalRequestStatus to) {
    if (from == null || to == null) {
      return false;
    }
    return switch (from) {
      case NEW ->
          to == RentalRequestStatus.CONFIRMED
              || to == RentalRequestStatus.CANCELLED
              || to == RentalRequestStatus.REJECTED;
      case CONFIRMED -> to == RentalRequestStatus.ACTIVE || to == RentalRequestStatus.CANCELLED;
      case ACTIVE -> to == RentalRequestStatus.COMPLETED || to == RentalRequestStatus.CANCELLED;
      case COMPLETED, CANCELLED, REJECTED -> false;
    };
  }
}
