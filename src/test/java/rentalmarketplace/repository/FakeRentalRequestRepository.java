package rentalmarketplace.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;

public class FakeRentalRequestRepository extends RentalRequestRepository {
  private final Map<Integer, RentalRequest> storage = new LinkedHashMap<>();
  private int nextId = 1;

  @Override
  public RentalRequest save(RentalRequest request) {
    request.setId(nextId++);
    request.setCreatedAt(LocalDateTime.now());
    storage.put(request.getId(), request);
    return request;
  }

  @Override
  public Optional<RentalRequest> findById(Integer id) {
    return Optional.ofNullable(storage.get(id));
  }

  @Override
  public List<RentalRequest> findAll() {
    return new ArrayList<>(storage.values());
  }

  @Override
  public RentalRequest update(RentalRequest request) {
    storage.put(request.getId(), request);
    return request;
  }

  @Override
  public void deleteById(Integer id) {
    storage.remove(id);
  }

  @Override
  public List<RentalRequest> findActiveByListingId(Integer listingId) {
    return storage.values().stream()
        .filter(request -> request.getListingId().equals(listingId))
        .filter(request -> isActive(request.getStatus()))
        .toList();
  }

  private boolean isActive(RentalRequestStatus status) {
    return status == RentalRequestStatus.NEW
        || status == RentalRequestStatus.CONFIRMED
        || status == RentalRequestStatus.ACTIVE;
  }
}
