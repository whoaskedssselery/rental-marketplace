package rentalmarketplace.repository;

import java.util.List;
import rentalmarketplace.model.RentalRequest;

public interface RentalRequestRepository extends CrudRepository<RentalRequest, Integer> {
  /**
   * Возвращает заявки по объекту, которые ещё «живые» и блокируют даты: статусы NEW, CONFIRMED,
   * ACTIVE. Финальные (COMPLETED/CANCELLED/REJECTED) не возвращаются.
   */
  List<RentalRequest> findActiveByListingId(Integer listingId);
}
