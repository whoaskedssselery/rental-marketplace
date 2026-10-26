package rentalmarketplace.service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.UserRepository;
import rentalmarketplace.security.AccessPolicy;

public class UserService {
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
  private static final int FULL_NAME_MAX_LENGTH = 150;
  private static final int EMAIL_MAX_LENGTH = 150;
  private static final int PHONE_MAX_LENGTH = 30;

  private final UserRepository userRepository;
  private final AccessPolicy access;

  public UserService(UserRepository userRepository, AccessPolicy access) {
    this.userRepository = userRepository;
    this.access = access;
  }

  public AccessPolicy access() {
    return access;
  }

  public User createUser(String fullName, String email, String phone, UserRole role) {
    access.requireAdmin();
    User user = validatedUser(fullName, email, phone, role);
    checkEmailIsFree(email, null);
    return userRepository.save(user);
  }

  static User validatedUser(String fullName, String email, String phone, UserRole role) {
    validateFullName(fullName);
    validateEmail(email);
    validatePhone(phone);
    validateRole(role);
    return new User(fullName.trim(), email.trim(), normalizePhone(phone), role);
  }

  public User getUserById(Integer id) {
    access.currentUser();
    return userRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Пользователь с id=" + id + " не найден"));
  }

  public List<User> getAllUsers() {
    access.currentUser();
    return userRepository.findAll();
  }

  public List<User> getVisibleUsers() {
    return access.isAdmin() ? getAllUsers() : List.of(access.currentUser());
  }

  public User updateUser(Integer id, String fullName, String email, String phone, UserRole role) {
    User existing = getUserById(id);
    access.requireOwner(id);
    if (!access.isAdmin() && role != existing.getRole()) {
      throw new BusinessRuleException("Менять роль может только администратор");
    }
    validateFullName(fullName);
    validateEmail(email);
    validatePhone(phone);
    validateRole(role);
    checkEmailIsFree(email, id);
    existing.setFullName(fullName.trim());
    existing.setEmail(email.trim());
    existing.setPhone(normalizePhone(phone));
    existing.setRole(role);
    return userRepository.update(existing);
  }

  public void deleteUser(Integer id) {
    access.requireAdmin();
    getUserById(id);
    userRepository.deleteById(id);
  }

  private static void validateFullName(String fullName) {
    if (fullName == null || fullName.isBlank()) {
      throw new BusinessRuleException("Имя пользователя не может быть пустым");
    }
    if (fullName.trim().length() > FULL_NAME_MAX_LENGTH) {
      throw new BusinessRuleException(
          "Имя пользователя не может быть длиннее " + FULL_NAME_MAX_LENGTH + " символов");
    }
  }

  private static void validateEmail(String email) {
    if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
      throw new BusinessRuleException("Некорректный формат email: " + email);
    }
    if (email.trim().length() > EMAIL_MAX_LENGTH) {
      throw new BusinessRuleException(
          "Email не может быть длиннее " + EMAIL_MAX_LENGTH + " символов");
    }
  }

  private static void validatePhone(String phone) {
    if (phone != null && phone.trim().length() > PHONE_MAX_LENGTH) {
      throw new BusinessRuleException(
          "Телефон не может быть длиннее " + PHONE_MAX_LENGTH + " символов");
    }
  }

  private static void validateRole(UserRole role) {
    if (role == null) {
      throw new BusinessRuleException("Роль пользователя обязательна");
    }
  }

  private static String normalizePhone(String phone) {
    return phone == null || phone.isBlank() ? null : phone.trim();
  }

  private void checkEmailIsFree(String email, Integer excludeId) {
    Optional<User> existing = userRepository.findByEmail(email.trim());
    if (existing.isPresent() && !existing.get().getId().equals(excludeId)) {
      throw new BusinessRuleException("Пользователь с email " + email + " уже существует");
    }
  }
}
