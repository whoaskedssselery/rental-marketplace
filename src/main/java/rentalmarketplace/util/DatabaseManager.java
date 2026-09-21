package rentalmarketplace.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rentalmarketplace.exception.DatabaseAccessException;

public final class DatabaseManager {
  private static final Map<String, String> DOT_ENV = loadDotEnv();

  private DatabaseManager() {}

  private static Map<String, String> loadDotEnv() {
    Map<String, String> values = new HashMap<>();
    Path envFile = Path.of(".env");
    if (!Files.isRegularFile(envFile)) {
      return values;
    }
    try {
      for (String line : Files.readAllLines(envFile)) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
          continue;
        }
        int separator = trimmed.indexOf('=');
        if (separator <= 0) {
          continue;
        }
        values.put(trimmed.substring(0, separator).trim(), trimmed.substring(separator + 1).trim());
      }
    } catch (IOException e) {
      throw new IllegalStateException("Не удалось прочитать файл .env", e);
    }
    return values;
  }

  private static String requireEnv(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      value = DOT_ENV.get(name);
    }
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
          "Переменная окружения " + name + " не задана. Проверьте файл .env");
    }
    return value;
  }

  public static Connection getConnection() {
    try {
      return DriverManager.getConnection(
          requireEnv("DB_URL"), requireEnv("DB_USER"), requireEnv("DB_PASSWORD"));
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
