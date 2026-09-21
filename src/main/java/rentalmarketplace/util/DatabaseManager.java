package rentalmarketplace.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import rentalmarketplace.exception.DatabaseAccessException;

public final class DatabaseManager {
  private static final Properties CONFIG = loadConfig();

  private DatabaseManager() {}

  private static Properties loadConfig() {
    Properties properties = new Properties();
    try (InputStream input =
        DatabaseManager.class.getClassLoader().getResourceAsStream("application.properties")) {
      if (input == null) {
        throw new IllegalStateException("Файл application.properties не найден в classpath");
      }
      properties.load(input);
    } catch (IOException e) {
      throw new IllegalStateException("Не удалось прочитать application.properties", e);
    }
    return properties;
  }

  public static Connection getConnection() {
    try {
      return DriverManager.getConnection(
          CONFIG.getProperty("db.url"),
          CONFIG.getProperty("db.user"),
          CONFIG.getProperty("db.password"));
    } catch (SQLException e) {
      throw new DatabaseAccessException(
          "Не удалось подключиться к базе данных: " + e.getMessage(), e);
    }
  }

  public static List<String> listTableNames() {
    String sql =
        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name";
    List<String> tableNames = new ArrayList<>();
    try (Connection connection = getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      while (resultSet.next()) {
        tableNames.add(resultSet.getString("table_name"));
      }
      return tableNames;
    } catch (SQLException e) {
      throw new DatabaseAccessException("Не удалось получить список таблиц: " + e.getMessage(), e);
    }
  }
}
