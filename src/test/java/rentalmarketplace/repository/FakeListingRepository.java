package rentalmarketplace.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import rentalmarketplace.model.Listing;

public class FakeListingRepository extends ListingRepository {
  private final Map<Integer, Listing> storage = new LinkedHashMap<>();
  private int nextId = 1;

  @Override
  public Listing save(Listing listing) {
    listing.setId(nextId++);
    listing.setCreatedAt(LocalDateTime.now());
    storage.put(listing.getId(), listing);
    return listing;
  }

  @Override
  public Optional<Listing> findById(Integer id) {
    return Optional.ofNullable(storage.get(id));
  }

  @Override
  public List<Listing> findAll() {
    return new ArrayList<>(storage.values());
  }

  @Override
  public Listing update(Listing listing) {
    storage.put(listing.getId(), listing);
    return listing;
  }

  @Override
  public void deleteById(Integer id) {
    storage.remove(id);
  }
}
