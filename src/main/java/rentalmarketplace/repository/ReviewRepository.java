package rentalmarketplace.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import rentalmarketplace.exception.DatabaseAccessException;
import rentalmarketplace.model.Review;
import rentalmarketplace.util.DatabaseManager;

public class ReviewRepository implements CrudRepository<Review, Integer> {
  private static final String COLUMNS = "id, request_id, rating, comment, created_at";

  @Override
  public Review save(Review review) {
    String sql =
        "INSERT INTO reviews (request_id, rating, comment) VALUES (?, ?, ?) "
            + "RETURNING id, created_at";
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, review.getRequestId());
      statement.setInt(2, review.getRating());
      statement.setString(3, review.getComment());
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        review.setId(resultSet.getInt("id"));
        review.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
      }
      return review;
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось сохранить отзыв: " + e.getMessage(), e);
    }
  }

  @Override
  public Optional<Review> findById(Integer id) {
    return findOne("SELECT " + COLUMNS + " FROM reviews WHERE id = ?", id);
  }

  public Optional<Review> findByRequestId(Integer requestId) {
    return findOne("SELECT " + COLUMNS + " FROM reviews WHERE request_id = ?", requestId);
  }

  @Override
  public List<Review> findAll() {
    String sql = "SELECT " + COLUMNS + " FROM reviews ORDER BY id";
    List<Review> reviews = new ArrayList<>();
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        reviews.add(mapRow(resultSet));
      }
      return reviews;
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось получить список отзывов: " + e.getMessage(), e);
    }
  }

  @Override
  public Review update(Review review) {
    String sql = "UPDATE reviews SET rating = ?, comment = ? WHERE id = ?";
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, review.getRating());
      statement.setString(2, review.getComment());
      statement.setInt(3, review.getId());
      statement.executeUpdate();
      return review;
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось обновить отзыв: " + e.getMessage(), e);
    }
  }

  @Override
  public void deleteById(Integer id) {
    String sql = "DELETE FROM reviews WHERE id = ?";
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось удалить отзыв: " + e.getMessage(), e);
    }
  }

  private Optional<Review> findOne(String sql, Integer parameter) {
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, parameter);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return Optional.of(mapRow(resultSet));
        }
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось найти отзыв: " + e.getMessage(), e);
    }
  }

  private Review mapRow(ResultSet resultSet) throws SQLException {
    return new Review(
        resultSet.getInt("id"),
        resultSet.getInt("request_id"),
        resultSet.getInt("rating"),
        resultSet.getString("comment"),
        resultSet.getTimestamp("created_at").toLocalDateTime());
  }
}
