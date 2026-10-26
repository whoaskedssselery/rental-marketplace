package rentalmarketplace.service;

import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.UserRepository;
import rentalmarketplace.security.PasswordHasher;

public class AuthenticationService {
  private final UserRepository users;
  private final PasswordHasher hasher = new PasswordHasher();
  private final String dummyHash = hasher.hash("non-account timing equalizer");
  private volatile Integer userId;

  public AuthenticationService(UserRepository users) {
    this.users = users;
  }

  public void initialize() {
    users.ensureAuthenticationSchema();
    String email = System.getenv("APP_ADMIN_EMAIL");
    String password = System.getenv("APP_ADMIN_PASSWORD");
    if (email != null && !email.isBlank() && password != null && !password.isBlank()) {
      initializeAdminPassword(email, password);
    }
  }

  /** Explicit local setup; never promotes an account or overwrites existing credentials. */
  public void initializeAdminPassword(String email, String password) {
    validatePassword(password);
    User user =
        users
            .findByEmail(email.trim())
            .orElseThrow(() -> new BusinessRuleException("Администратор с таким email не найден"));
    if (user.getRole() != UserRole.ADMIN) {
      throw new BusinessRuleException("Начальная настройка пароля доступна только администратору");
    }
    users.setInitialPassword(user.getId(), hasher.hash(password));
  }

  public User register(String fullName, String email, String password) {
    logout();
    User user = UserService.validatedUser(fullName, email, null, UserRole.RENTER);
    validatePassword(password);
    if (users.findByEmail(user.getEmail()).isPresent()) {
      throw new BusinessRuleException("Этот email уже занят. Войдите в существующий аккаунт");
    }
    User saved = users.saveWithPassword(user, hasher.hash(password));
    userId = saved.getId();
    return saved;
  }

  public User login(String email, String password) {
    logout();
    if (email == null
        || email.trim().length() > 150
        || password == null
        || password.length() > 128) {
      throw invalidCredentials();
    }
    User user = users.findByEmail(email.trim()).orElse(null);
    String hash = user == null ? null : users.findPasswordHash(user.getId()).orElse(null);
    boolean valid = hasher.verify(password, hash == null ? dummyHash : hash);
    if (user == null || hash == null || !valid) {
      throw invalidCredentials();
    }
    userId = user.getId();
    return user;
  }

  public User currentUser() {
    Integer id = userId;
    if (id == null) {
      throw new BusinessRuleException("Войдите в аккаунт");
    }
    return users
        .findById(id)
        .orElseThrow(() -> new BusinessRuleException("Аккаунт удалён. Войдите заново"));
  }

  public void logout() {
    userId = null;
  }

  private void validatePassword(String password) {
    if (password == null || password.length() < 15 || password.length() > 128) {
      throw new BusinessRuleException("Пароль должен содержать от 15 до 128 символов");
    }
  }

  private BusinessRuleException invalidCredentials() {
    return new BusinessRuleException("Неверный email или пароль");
  }
}
