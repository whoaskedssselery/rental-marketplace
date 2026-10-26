package rentalmarketplace.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import rentalmarketplace.model.User;

public class FakeUserRepository extends UserRepository {
  private final Map<Integer, User> storage = new LinkedHashMap<>();
  private int nextId = 1;
  private final Map<Integer, String> hashes = new LinkedHashMap<>();

  @Override
  public User saveWithPassword(User user, String hash) {
    save(user);
    hashes.put(user.getId(), hash);
    return user;
  }

  @Override
  public Optional<String> findPasswordHash(Integer id) {
    return Optional.ofNullable(hashes.get(id));
  }

  @Override
  public void ensureAuthenticationSchema() {}

  @Override
  public void setInitialPassword(Integer id, String hash) {
    if (storage.get(id).getRole() == rentalmarketplace.model.UserRole.ADMIN) {
      hashes.putIfAbsent(id, hash);
    }
  }

  @Override
  public User save(User user) {
    user.setId(nextId++);
    user.setCreatedAt(LocalDateTime.now());
    storage.put(user.getId(), user);
    return user;
  }

  @Override
  public Optional<User> findById(Integer id) {
    return Optional.ofNullable(storage.get(id));
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return storage.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
  }

  @Override
  public List<User> findAll() {
    return new ArrayList<>(storage.values());
  }

  @Override
  public User update(User user) {
    storage.put(user.getId(), user);
    return user;
  }

  @Override
  public void deleteById(Integer id) {
    storage.remove(id);
  }
}
