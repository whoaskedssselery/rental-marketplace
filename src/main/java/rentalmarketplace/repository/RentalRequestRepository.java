package rentalmarketplace.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import rentalmarketplace.exception.DatabaseAccessException;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.util.DatabaseManager;

public class RentalRequestRepository implements CrudRepository<RentalRequest, Integer> {

  private static final String SELECT_ALL =
      "SELECT id, listing_id, renter_id, start_date, end_date, status, total_price, created_at "
          + "FROM rental_requests";

  @Override
  public RentalRequest save(RentalRequest entity) {
    String sql =
        "INSERT INTO rental_requests "
            + "(listing_id, renter_id, start_date, end_date, status, total_price) "
            + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id, created_at";
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, entity.getListingId());
      statement.setInt(2, entity.getRenterId());
      statement.setDate(3, Date.valueOf(entity.getStartDate()));
      statement.setDate(4, Date.valueOf(entity.getEndDate()));
      statement.setString(5, entity.getStatus().name());
      statement.setBigDecimal(6, entity.getTotalPrice());
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        entity.setId(resultSet.getInt("id"));
        entity.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
      }
      return entity;
    } catch (SQLException e) {
      if (DatabaseManager.isExclusionViolation(e)) {
        throw new DatabaseAccessException(
            "Даты пересекаются с другой активной заявкой на этот объект", e);
      }
      throw new DatabaseAccessException("Не удалось сохранить заявку: " + e.getMessage(), e);
    }
  }

  @Override
  public Optional<RentalRequest> findById(Integer id) {
    String sql = SELECT_ALL + " WHERE id = ?";
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return Optional.of(mapRow(resultSet));
        }
      }
    } catch (SQLException e) {
      throw new DatabaseAccessException(
          "Не удалось найти заявку по id=" + id + ": " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  @Override
  public List<RentalRequest> findAll() {
    String sql = SELECT_ALL + " ORDER BY id";
    List<RentalRequest> result = new ArrayList<>();
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        result.add(mapRow(resultSet));
      }
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось получить список заявок: " + e.getMessage(), e);
    }
    return result;
  }

  @Override
  public RentalRequest update(RentalRequest entity) {
    String sql =
        "UPDATE rental_requests "
            + "SET listing_id = ?, renter_id = ?, start_date = ?, end_date = ?, "
            + "status = ?, total_price = ? "
            + "WHERE id = ?";
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, entity.getListingId());
      statement.setInt(2, entity.getRenterId());
      statement.setDate(3, Date.valueOf(entity.getStartDate()));
      statement.setDate(4, Date.valueOf(entity.getEndDate()));
      statement.setString(5, entity.getStatus().name());
      statement.setBigDecimal(6, entity.getTotalPrice());
      statement.setInt(7, entity.getId());
      statement.executeUpdate();
      return entity;
    } catch (SQLException e) {
      if (DatabaseManager.isExclusionViolation(e)) {
        throw new DatabaseAccessException(
            "Даты пересекаются с другой активной заявкой на этот объект", e);
      }
      throw new DatabaseAccessException("Не удалось обновить заявку: " + e.getMessage(), e);
    }
  }

  @Override
  public void deleteById(Integer id) {
    String sql = "DELETE FROM rental_requests WHERE id = ?";
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось удалить заявку: " + e.getMessage(), e);
    }
  }

  public List<RentalRequest> findActiveByListingId(Integer listingId) {
    List<RentalRequest> result = new ArrayList<>();
    String sql = SELECT_ALL + " WHERE listing_id = ? AND status IN ('NEW', 'CONFIRMED', 'ACTIVE')";
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, listingId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          result.add(mapRow(resultSet));
        }
      }
    } catch (SQLException e) {
      throw new DatabaseAccessException(
          "Не удалось получить активные заявки: " + e.getMessage(), e);
    }
    return result;
  }

  private RentalRequest mapRow(ResultSet resultSet) throws SQLException {
    Timestamp createdAt = resultSet.getTimestamp("created_at");
    return new RentalRequest(
        resultSet.getInt("id"),
        resultSet.getInt("listing_id"),
        resultSet.getInt("renter_id"),
        resultSet.getDate("start_date").toLocalDate(),
        resultSet.getDate("end_date").toLocalDate(),
        RentalRequestStatus.valueOf(resultSet.getString("status")),
        resultSet.getBigDecimal("total_price"),
        createdAt == null ? null : createdAt.toLocalDateTime());
  }
}
