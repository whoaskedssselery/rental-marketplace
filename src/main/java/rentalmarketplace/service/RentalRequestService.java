package rentalmarketplace.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.repository.RentalRequestRepository;

public class RentalRequestService {
  private static final BigDecimal MAX_TOTAL_PRICE = new BigDecimal("99999999.99");
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
    userService.access().requireOwner(renterId);
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
    RentalRequest request = findRequest(id);
    if (!mayRead(request)) {
      throw new BusinessRuleException("Заявка доступна только участникам аренды");
    }
    return request;
  }

  private RentalRequest findRequest(Integer id) {
    userService.access().currentUser();
    return rentalRequestRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Заявка с id=" + id + " не найдена"));
  }

  public List<RentalRequest> getAll() {
    userService.access().currentUser();
    return rentalRequestRepository.findAll().stream().filter(this::mayRead).toList();
  }

  /** Internal projection source for public reviews, never exposed as a booking endpoint. */
  List<RentalRequest> reviewedRequests(java.util.Set<Integer> reviewRequestIds) {
    userService.access().currentUser();
    return rentalRequestRepository.findAll().stream()
        .filter(request -> reviewRequestIds.contains(request.getId()))
        .toList();
  }

  private boolean mayRead(RentalRequest request) {
    return userService.access().mayEdit(request.getRenterId())
        || userService
            .access()
            .mayEdit(listingService.getListingById(request.getListingId()).getOwnerId());
  }

  public boolean canEdit(Integer id) {
    RentalRequest request = getById(id);
    return userService.access().mayEdit(request.getRenterId())
        && canChangeDates(request.getStatus());
  }

  public boolean canDelete(Integer id) {
    return userService.access().mayEdit(getById(id).getRenterId());
  }

  public List<RentalRequestStatus> allowedTransitions(Integer id) {
    RentalRequest request = getById(id);
    boolean owner =
        userService
            .access()
            .mayEdit(listingService.getListingById(request.getListingId()).getOwnerId());
    return allowedTransitions(request.getStatus()).stream()
        .filter(status -> owner || status == RentalRequestStatus.CANCELLED)
        .toList();
  }

  public RentalRequest updateDates(
      Integer requestId, LocalDate newStartDate, LocalDate newEndDate) {
    RentalRequest request = getById(requestId);

    userService.access().requireOwner(request.getRenterId());

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
    if (newStatus != RentalRequestStatus.CANCELLED) {
      userService
          .access()
          .requireOwner(listingService.getListingById(request.getListingId()).getOwnerId());
    }
    RentalRequestStatus current = request.getStatus();
    if (!isTransitionAllowed(current, newStatus)) {
      throw new BusinessRuleException("Переход " + current + " -> " + newStatus + " запрещён");
    }
    request.setStatus(newStatus);
    return rentalRequestRepository.update(request);
  }

  public void delete(Integer id) {
    userService.access().requireOwner(getById(id).getRenterId());
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

  public List<RentalRequestStatus> allowedTransitions(RentalRequestStatus status) {
    return Arrays.stream(RentalRequestStatus.values())
        .filter(next -> isTransitionAllowed(status, next))
        .toList();
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
    BigDecimal total = listing.getPricePerDay().multiply(BigDecimal.valueOf(days));
    if (total.compareTo(MAX_TOTAL_PRICE) > 0) {
      throw new BusinessRuleException(
          "Сумма аренды не может превышать 99 999 999,99 руб. Сократите срок аренды.");
    }
    return total;
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
