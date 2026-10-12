package rentalmarketplace.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import rentalmarketplace.model.RentalRequestStatus;

public record RequestDetails(
    Integer requestId,
    Integer listingId,
    String listingTitle,
    Integer renterId,
    String renterName,
    String renterEmail,
    Integer ownerId,
    String ownerName,
    LocalDate startDate,
    LocalDate endDate,
    RentalRequestStatus status,
    BigDecimal totalPrice,
    LocalDateTime createdAt) {}
