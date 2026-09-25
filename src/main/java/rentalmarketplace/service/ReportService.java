package rentalmarketplace.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.Statistics;
import rentalmarketplace.repository.SchemaRepository;
import rentalmarketplace.util.ExcelExporter;

public class ReportService {
  private static final Set<RentalRequestStatus> ACTIVE_STATUSES =
      Set.of(RentalRequestStatus.NEW, RentalRequestStatus.CONFIRMED, RentalRequestStatus.ACTIVE);
  private static final Set<RentalRequestStatus> COMPLETED_STATUSES =
      Set.of(RentalRequestStatus.COMPLETED);
  private static final Set<RentalRequestStatus> CANCELLED_STATUSES =
      Set.of(RentalRequestStatus.CANCELLED, RentalRequestStatus.REJECTED);
  private static final DateTimeFormatter FILE_NAME_TIME =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  private final UserService userService;
  private final ListingService listingService;
  private final RentalRequestService rentalRequestService;
  private final SchemaRepository schemaRepository;

  public ReportService(
      UserService userService,
      ListingService listingService,
      RentalRequestService rentalRequestService,
      SchemaRepository schemaRepository) {
    this.userService = userService;
    this.listingService = listingService;
    this.rentalRequestService = rentalRequestService;
    this.schemaRepository = schemaRepository;
  }

  public Statistics collectStatistics() {
    List<RentalRequest> requests = rentalRequestService.getAll();
    return new Statistics(
        userService.getAllUsers().size(),
        listingService.getAllListings().size(),
        requests.size(),
        countWithStatus(requests, ACTIVE_STATUSES),
        countWithStatus(requests, COMPLETED_STATUSES),
        countWithStatus(requests, CANCELLED_STATUSES),
        averageTotalPrice(requests));
  }

  public Path exportToExcel() {
    Path file =
        Path.of("rental-marketplace-" + LocalDateTime.now().format(FILE_NAME_TIME) + ".xlsx")
            .toAbsolutePath();
    ExcelExporter.export(
        userService.getAllUsers(),
        listingService.getAllListings(),
        rentalRequestService.getAll(),
        file);
    return file;
  }

  public List<String> getTableNames() {
    return schemaRepository.findAllTableNames();
  }

  private long countWithStatus(List<RentalRequest> requests, Set<RentalRequestStatus> statuses) {
    return requests.stream().filter(request -> statuses.contains(request.getStatus())).count();
  }

  private BigDecimal averageTotalPrice(List<RentalRequest> requests) {
    if (requests.isEmpty()) {
      return BigDecimal.ZERO.setScale(2);
    }
    BigDecimal sum =
        requests.stream()
            .map(RentalRequest::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return sum.divide(BigDecimal.valueOf(requests.size()), 2, RoundingMode.HALF_UP);
  }
}
