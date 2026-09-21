package rentalmarketplace.repository;

import java.util.List;
import rentalmarketplace.model.RentalRequest;

public interface RentalRequestRepository extends CrudRepository<RentalRequest, Integer> {
  List<RentalRequest> findActiveByListingId(Integer listingId);
}
