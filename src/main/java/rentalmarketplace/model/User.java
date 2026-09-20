package rentalmarketplace.model;

import java.time.LocalDateTime;

public class User implements Displayable {
  private Integer id;
  private String fullName;
  private String email;
  private String phone;
  private UserRole role;
  private LocalDateTime createdAt;

  /** Используется при создании нового пользователя — id и createdAt назначит база данных. */
  public User(String fullName, String email, String phone, UserRole role) {
    this(null, fullName, email, phone, role, null);
  }

  /** Используется при загрузке уже существующего пользователя (из БД или in-memory хранилища). */
  public User(
      Integer id,
      String fullName,
      String email,
      String phone,
      UserRole role,
      LocalDateTime createdAt) {
    this.id = id;
    this.fullName = fullName;
    this.email = email;
    this.phone = phone;
    this.role = role;
    this.createdAt = createdAt;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole role) {
    this.role = role;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public String toTableRow() {
    return String.format(
        "%-4s %-25s %-30s %-16s %-8s %-20s",
        id == null ? "-" : id,
        fullName,
        email,
        phone == null || phone.isBlank() ? "-" : phone,
        role,
        createdAt == null ? "-" : createdAt);
  }

  @Override
  public String toString() {
    return toTableRow();
  }
}
