package rentalmarketplace.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import rentalmarketplace.exception.DatabaseAccessException;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.util.DatabaseManager;

public class UserRepository implements CrudRepository<User, Integer> {

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
