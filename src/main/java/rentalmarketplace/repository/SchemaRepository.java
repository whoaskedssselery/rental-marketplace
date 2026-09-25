package rentalmarketplace.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import rentalmarketplace.exception.DatabaseAccessException;
import rentalmarketplace.util.DatabaseManager;

public class SchemaRepository {

  public List<String> findAllTableNames() {
    String sql =
        "SELECT table_name FROM information_schema.tables "
            + "WHERE table_schema = 'public' ORDER BY table_name";
    List<String> tableNames = new ArrayList<>();
    try (Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        tableNames.add(resultSet.getString("table_name"));
      }
      return tableNames;
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось получить список таблиц: " + e.getMessage(), e);
    }
  }
}
