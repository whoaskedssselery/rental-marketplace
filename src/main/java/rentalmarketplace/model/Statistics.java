package rentalmarketplace.model;

import java.math.BigDecimal;

public record Statistics(
    int totalUsers,
    int totalListings,
    int totalRequests,
    long activeRequests,
    long completedRequests,
    long cancelledOrRejectedRequests,
    BigDecimal averageTotalPrice) {}
