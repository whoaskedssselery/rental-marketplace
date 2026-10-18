package rentalmarketplace.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import rentalmarketplace.model.Review;

public class FakeReviewRepository extends ReviewRepository {
  private final Map<Integer, Review> storage = new LinkedHashMap<>();
  private int nextId = 1;

  @Override
  public Review save(Review review) {
    review.setId(nextId++);
    review.setCreatedAt(LocalDateTime.now());
    storage.put(review.getId(), review);
    return review;
  }

  @Override
  public Optional<Review> findById(Integer id) {
    return Optional.ofNullable(storage.get(id));
  }

  @Override
  public Optional<Review> findByRequestId(Integer requestId) {
    return storage.values().stream().filter(r -> r.getRequestId().equals(requestId)).findFirst();
  }

  @Override
  public List<Review> findAll() {
    return new ArrayList<>(storage.values());
  }

  @Override
  public Review update(Review review) {
    storage.put(review.getId(), review);
    return review;
  }

  @Override
  public void deleteById(Integer id) {
    storage.remove(id);
  }
}
