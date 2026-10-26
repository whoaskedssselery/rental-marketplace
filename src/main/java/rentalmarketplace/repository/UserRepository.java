package rentalmarketplace.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.DatabaseAccessException;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.util.DatabaseManager;

public class UserRepository implements CrudRepository<User, Integer> {

  public void ensureAuthenticationSchema() {
    try (Connection connection = DatabaseManager.getConnection()) {
      connection.setAutoCommit(false);
      try (var statement = connection.createStatement()) {
        statement.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash TEXT");
        statement.execute(
            "CREATE UNIQUE INDEX IF NOT EXISTS users_email_lower_key ON users (lower(email))");
        connection.commit();
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new DatabaseAccessException(
          "Не удалось обновить схему входа. Проверьте права БД и повторы email без учёта регистра",
          e);
    }
  }

  public User saveWithPassword(User user, String passwordHash) {
    String sql =
        "INSERT INTO users (full_name,email,phone,role,password_hash) VALUES (?,?,?,?,?) "
            + "RETURNING id,created_at";
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, user.getFullName());
      statement.setString(2, user.getEmail());
      statement.setString(3, user.getPhone());
      statement.setString(4, user.getRole().name());
      statement.setString(5, passwordHash);
      try (ResultSet result = statement.executeQuery()) {
        result.next();
        user.setId(result.getInt("id"));
        user.setCreatedAt(result.getTimestamp("created_at").toLocalDateTime());
      }
      return user;
    } catch (SQLException e) {
      if ("23505".equals(e.getSQLState())) {
        throw new BusinessRuleException("Этот email уже занят. Войдите в существующий аккаунт");
      }
      throw new DatabaseAccessException("Не удалось зарегистрировать аккаунт", e);
    }
  }

  public Optional<String> findPasswordHash(Integer userId) {
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement =
            connection.prepareStatement("SELECT password_hash FROM users WHERE id=?")) {
      statement.setInt(1, userId);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? Optional.ofNullable(result.getString(1)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось проверить пароль", e);
    }
  }

  public void setInitialPassword(Integer userId, String passwordHash) {
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "UPDATE users SET password_hash=? "
                    + "WHERE id=? AND role='ADMIN' AND password_hash IS NULL")) {
      statement.setString(1, passwordHash);
      statement.setInt(2, userId);
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось задать пароль администратора", e);
    }
  }

  @Override
  public User save(User user) {
    String sql =
        "INSERT INTO users (full_name, email, phone, role) VALUES (?, ?, ?, ?) "
            + "RETURNING id, created_at";
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, user.getFullName());
      statement.setString(2, user.getEmail());
      statement.setString(3, user.getPhone());
      statement.setString(4, user.getRole().name());
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        user.setId(resultSet.getInt("id"));
        user.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
      }
      return user;
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось сохранить пользователя: " + e.getMessage(), e);
    }
  }

  @Override
  public Optional<User> findById(Integer id) {
    String sql = "SELECT id, full_name, email, phone, role, created_at FROM users WHERE id = ?";
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return Optional.of(mapRow(resultSet));
        }
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось найти пользователя: " + e.getMessage(), e);
    }
  }

  public Optional<User> findByEmail(String email) {
    String sql =
        "SELECT id, full_name, email, phone, role, created_at FROM users "
            + "WHERE lower(email) = lower(?)";
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, email);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return Optional.of(mapRow(resultSet));
        }
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new DatabaseAccessException(
          "Не удалось найти пользователя по email: " + e.getMessage(), e);
    }
  }

  @Override
  public List<User> findAll() {
    String sql = "SELECT id, full_name, email, phone, role, created_at FROM users ORDER BY id";
    List<User> users = new ArrayList<>();
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        users.add(mapRow(resultSet));
      }
      return users;
    } catch (SQLException e) {
      throw new DatabaseAccessException(
          "Не удалось получить список пользователей: " + e.getMessage(), e);
    }
  }

  @Override
  public User update(User user) {
    String sql = "UPDATE users SET full_name = ?, email = ?, phone = ?, role = ? WHERE id = ?";
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, user.getFullName());
      statement.setString(2, user.getEmail());
      statement.setString(3, user.getPhone());
      statement.setString(4, user.getRole().name());
      statement.setInt(5, user.getId());
      statement.executeUpdate();
      return user;
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось обновить пользователя: " + e.getMessage(), e);
    }
  }

  @Override
  public void deleteById(Integer id) {
    String sql = "DELETE FROM users WHERE id = ?";
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      statement.executeUpdate();
    } catch (SQLException e) {
      if (DatabaseManager.isForeignKeyViolation(e)) {
        throw new DatabaseAccessException(
            "Нельзя удалить пользователя: на него ссылаются объекты аренды или заявки", e);
      }
      throw new DatabaseAccessException("Не удалось удалить пользователя: " + e.getMessage(), e);
    }
  }

  private User mapRow(ResultSet resultSet) throws SQLException {
    LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
    return new User(
        resultSet.getInt("id"),
        resultSet.getString("full_name"),
        resultSet.getString("email"),
        resultSet.getString("phone"),
        UserRole.valueOf(resultSet.getString("role")),
        createdAt);
  }
}
