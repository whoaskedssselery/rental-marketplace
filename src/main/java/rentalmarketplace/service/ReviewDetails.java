package rentalmarketplace.service;

import java.time.LocalDateTime;

public record ReviewDetails(
    Integer reviewId,
    Integer requestId,
    Integer listingId,
    String listingTitle,
    Integer renterId,
    String renterName,
    int rating,
    String comment,
    LocalDateTime createdAt) {}
