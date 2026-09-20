package rentalmarketplace.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import rentalmarketplace.exception.DatabaseAccessException;
import rentalmarketplace.model.Listing;
import rentalmarketplace.util.DatabaseManager;

public class ListingRepositoryJdbc implements ListingRepository {

  @Override
  public Listing save(Listing listing) {
    String sql =
        "INSERT INTO listings (owner_id, title, description, price_per_day, category, available) "
            + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id, created_at";
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, listing.getOwnerId());
      statement.setString(2, listing.getTitle());
      statement.setString(3, listing.getDescription());
      statement.setBigDecimal(4, listing.getPricePerDay());
      statement.setString(5, listing.getCategory());
      statement.setBoolean(6, listing.isAvailable());
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        listing.setId(resultSet.getInt("id"));
        listing.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
      }
      return listing;
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось сохранить объект аренды: " + e.getMessage(), e);
    }
  }

  @Override
  public Optional<Listing> findById(Integer id) {
    String sql =
        "SELECT id, owner_id, title, description, price_per_day, category, available, created_at "
            + "FROM listings WHERE id = ?";
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
      throw new DatabaseAccessException("Не удалось найти объект аренды: " + e.getMessage(), e);
    }
  }

  @Override
  public List<Listing> findAll() {
    String sql =
        "SELECT id, owner_id, title, description, price_per_day, category, available, created_at "
            + "FROM listings ORDER BY id";
    List<Listing> listings = new ArrayList<>();
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        listings.add(mapRow(resultSet));
      }
      return listings;
    } catch (SQLException e) {
      throw new DatabaseAccessException(
          "Не удалось получить список объектов аренды: " + e.getMessage(), e);
    }
  }

  @Override
  public Listing update(Listing listing) {
    String sql =
        "UPDATE listings SET owner_id = ?, title = ?, description = ?, price_per_day = ?, "
            + "category = ?, available = ? WHERE id = ?";
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, listing.getOwnerId());
      statement.setString(2, listing.getTitle());
      statement.setString(3, listing.getDescription());
      statement.setBigDecimal(4, listing.getPricePerDay());
      statement.setString(5, listing.getCategory());
      statement.setBoolean(6, listing.isAvailable());
      statement.setInt(7, listing.getId());
      statement.executeUpdate();
      return listing;
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось обновить объект аренды: " + e.getMessage(), e);
    }
  }

  @Override
  public void deleteById(Integer id) {
    String sql = "DELETE FROM listings WHERE id = ?";
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось удалить объект аренды: " + e.getMessage(), e);
    }
  }

  private Listing mapRow(ResultSet resultSet) throws SQLException {
    return new Listing(
        resultSet.getInt("id"),
        resultSet.getInt("owner_id"),
        resultSet.getString("title"),
        resultSet.getString("description"),
        resultSet.getBigDecimal("price_per_day"),
        resultSet.getString("category"),
        resultSet.getBoolean("available"),
        resultSet.getTimestamp("created_at").toLocalDateTime());
  }
}
