package rentalmarketplace.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;

public class InMemoryRentalRequestRepository implements RentalRequestRepository {
  private final Map<Integer, RentalRequest> storage = new ConcurrentHashMap<>();
  private final AtomicInteger idSequence = new AtomicInteger(0);

  @Override
  public RentalRequest save(RentalRequest entity) {
    if (entity.getId() == null) {
      entity.setId(idSequence.incrementAndGet());
    }
    storage.put(entity.getId(), entity);
    return entity;
  }

  @Override
  public Optional<RentalRequest> findById(Integer id) {
    return Optional.ofNullable(storage.get(id));
  }

  @Override
  public List<RentalRequest> findAll() {
    List<RentalRequest> result = new ArrayList<>(storage.values());
    result.sort(Comparator.comparing(RentalRequest::getId));
    return result;
  }

  @Override
  public RentalRequest update(RentalRequest entity) {
    storage.put(entity.getId(), entity);
    return entity;
  }

  @Override
  public void deleteById(Integer id) {
    storage.remove(id);
  }

  @Override
  public List<RentalRequest> findActiveByListingId(Integer listingId) {
    List<RentalRequest> result = new ArrayList<>();
    for (RentalRequest request : storage.values()) {
      if (!request.getListingId().equals(listingId)) {
        continue;
      }
      RentalRequestStatus status = request.getStatus();
      if (status == RentalRequestStatus.NEW
          || status == RentalRequestStatus.CONFIRMED
          || status == RentalRequestStatus.ACTIVE) {
        result.add(request);
      }
    }
    return result;
  }
}
