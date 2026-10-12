package rentalmarketplace.service;

import java.time.LocalDate;
import rentalmarketplace.model.RentalRequestStatus;

public record RequestQuery(
    String text,
    RentalRequestStatus status,
    Integer ownerId,
    Integer listingId,
    Integer renterId,
    LocalDate periodFrom,
    LocalDate periodTo,
    RequestSort sort,
    boolean descending) {

  public static RequestQuery all() {
    return new RequestQuery(null, null, null, null, null, null, null, null, false);
  }
}
