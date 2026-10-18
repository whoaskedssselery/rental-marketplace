package rentalmarketplace.service;

import java.time.LocalDate;

public record ReviewableRequest(
    Integer requestId,
    String listingTitle,
    String renterName,
    LocalDate startDate,
    LocalDate endDate) {

  @Override
  public String toString() {
    return "Заявка №"
        + requestId
        + ": "
        + listingTitle
        + ", "
        + renterName
        + ", "
        + startDate
        + " - "
        + endDate;
  }
}
