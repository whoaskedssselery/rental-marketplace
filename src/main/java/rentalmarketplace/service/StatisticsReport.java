package rentalmarketplace.service;

import java.math.BigDecimal;
import java.util.Map;
import rentalmarketplace.model.RentalRequestStatus;

public record StatisticsReport(
    int totalUsers,
    int totalListings,
    int totalRequests,
    int totalReviews,
    long activeRequests,
    long completedRequests,
    long cancelledOrRejectedRequests,
    BigDecimal averageTotalPrice,
    BigDecimal minTotalPrice,
    BigDecimal maxTotalPrice,
    BigDecimal cancelledSharePercent,
    BigDecimal averageRating,
    Map<RentalRequestStatus, Long> requestsByStatus,
    Map<String, Long> listingsByCategory) {}
