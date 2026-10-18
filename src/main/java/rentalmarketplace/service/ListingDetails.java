package rentalmarketplace.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ListingDetails(
    Integer listingId,
    Integer ownerId,
    String ownerName,
    String title,
    String description,
    BigDecimal pricePerDay,
    String category,
    boolean available,
    LocalDateTime createdAt) {}
