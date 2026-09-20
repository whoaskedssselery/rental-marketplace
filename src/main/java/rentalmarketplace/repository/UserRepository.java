package rentalmarketplace.repository;

import java.util.Optional;
import rentalmarketplace.model.User;

public interface UserRepository extends CrudRepository<User, Integer> {
  Optional<User> findByEmail(String email);
}
