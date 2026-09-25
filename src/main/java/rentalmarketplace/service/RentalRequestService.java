package rentalmarketplace.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    if (request.getStatus() != RentalRequestStatus.NEW) {
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

  public List<RentalRequest> searchByListing(String query) {
    String text = normalize(query);
    Map<Integer, Listing> listingsById =
        listingService.getAllListings().stream()
            .collect(Collectors.toMap(Listing::getId, Function.identity()));
    return getAll().stream()
        .filter(request -> matchesListing(listingsById.get(request.getListingId()), text))
        .toList();
  }

  public List<RentalRequest> searchByRenter(String query) {
    String text = normalize(query);
    Map<Integer, User> usersById =
        userService.getAllUsers().stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
    return getAll().stream()
        .filter(request -> matchesRenter(usersById.get(request.getRenterId()), text))
        .toList();
  }

  public List<RentalRequest> filterByStatus(RentalRequestStatus status) {
    return getAll().stream().filter(request -> request.getStatus() == status).toList();
  }

  public List<RentalRequest> filterByListingOwner(Integer ownerId) {
    Set<Integer> ownedListingIds =
        listingService.getAllListings().stream()
            .filter(listing -> listing.getOwnerId().equals(ownerId))
            .map(Listing::getId)
            .collect(Collectors.toSet());
    return getAll().stream()
        .filter(request -> ownedListingIds.contains(request.getListingId()))
        .toList();
  }

  public List<RentalRequest> sortByCreatedAt() {
    return getAll().stream()
        .sorted(
            Comparator.comparing(
                RentalRequest::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
  }

  public List<RentalRequest> sortByTotalPrice() {
    return getAll().stream().sorted(Comparator.comparing(RentalRequest::getTotalPrice)).toList();
  }

  private String normalize(String query) {
    return query == null ? "" : query.trim().toLowerCase();
  }

  private boolean matchesListing(Listing listing, String text) {
    return listing != null
        && (containsIgnoreCase(listing.getTitle(), text)
            || containsIgnoreCase(listing.getDescription(), text));
  }

  private boolean matchesRenter(User renter, String text) {
    return renter != null
        && (containsIgnoreCase(renter.getFullName(), text)
            || containsIgnoreCase(renter.getEmail(), text));
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
