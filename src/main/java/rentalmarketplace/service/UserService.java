package rentalmarketplace.service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.UserRepository;

public class UserService {
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
  private static final int FULL_NAME_MAX_LENGTH = 150;
  private static final int EMAIL_MAX_LENGTH = 150;
  private static final int PHONE_MAX_LENGTH = 30;

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User createUser(String fullName, String email, String phone, UserRole role) {
    validateFullName(fullName);
    validateEmail(email);
    validatePhone(phone);
    checkEmailIsFree(email, null);
    User user = new User(fullName.trim(), email.trim(), phone, role);
    return userRepository.save(user);
  }

  public User getUserById(Integer id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Пользователь с id=" + id + " не найден"));
  }

  public List<User> getAllUsers() {
    return userRepository.findAll();
  }

  public User updateUser(Integer id, String fullName, String email, String phone, UserRole role) {
    User existing = getUserById(id);
    validateFullName(fullName);
    validateEmail(email);
    validatePhone(phone);
    checkEmailIsFree(email, id);
    existing.setFullName(fullName.trim());
    existing.setEmail(email.trim());
    existing.setPhone(phone);
    existing.setRole(role);
    return userRepository.update(existing);
  }

  public void deleteUser(Integer id) {
    getUserById(id);
    userRepository.deleteById(id);
  }

  private void validateFullName(String fullName) {
    if (fullName == null || fullName.isBlank()) {
      throw new BusinessRuleException("Имя пользователя не может быть пустым");
    }
    if (fullName.trim().length() > FULL_NAME_MAX_LENGTH) {
      throw new BusinessRuleException(
          "Имя пользователя не может быть длиннее " + FULL_NAME_MAX_LENGTH + " символов");
    }
  }

  private void validateEmail(String email) {
    if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
      throw new BusinessRuleException("Некорректный формат email: " + email);
    }
    if (email.trim().length() > EMAIL_MAX_LENGTH) {
      throw new BusinessRuleException(
          "Email не может быть длиннее " + EMAIL_MAX_LENGTH + " символов");
    }
  }

  private void validatePhone(String phone) {
    if (phone != null && phone.length() > PHONE_MAX_LENGTH) {
      throw new BusinessRuleException(
          "Телефон не может быть длиннее " + PHONE_MAX_LENGTH + " символов");
    }
  }

  private void checkEmailIsFree(String email, Integer excludeId) {
    Optional<User> existing = userRepository.findByEmail(email.trim());
    if (existing.isPresent() && !existing.get().getId().equals(excludeId)) {
      throw new BusinessRuleException("Пользователь с email " + email + " уже существует");
    }
  }
}
