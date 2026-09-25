package rentalmarketplace.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import rentalmarketplace.exception.DatabaseAccessException;

public final class DatabaseManager {
  private static final String FOREIGN_KEY_VIOLATION = "23503";
  private static final String EXCLUSION_VIOLATION = "23P01";
  private static final Path ENV_FILE = Path.of(".env");

  private static HikariDataSource dataSource;

  private DatabaseManager() {}

  public static synchronized Connection getConnection() {
    try {
      if (dataSource == null) {
        dataSource = createDataSource();
      }
      return dataSource.getConnection();
    } catch (SQLException | RuntimeException e) {
      throw new DatabaseAccessException(
          "Не удалось подключиться к базе данных: " + e.getMessage(), e);
    }
  }

  public static boolean isForeignKeyViolation(SQLException e) {
    return FOREIGN_KEY_VIOLATION.equals(e.getSQLState());
  }

  public static boolean isExclusionViolation(SQLException e) {
    return EXCLUSION_VIOLATION.equals(e.getSQLState());
  }

  private static HikariDataSource createDataSource() {
    Properties dotEnv = loadDotEnv();
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(readSetting("DB_URL", dotEnv));
    config.setUsername(readSetting("DB_USER", dotEnv));
    config.setPassword(readSetting("DB_PASSWORD", dotEnv));
    config.setMaximumPoolSize(5);
    config.setMinimumIdle(1);
    config.setConnectionTimeout(5000);
    config.setInitializationFailTimeout(-1);
    return new HikariDataSource(config);
  }

  private static Properties loadDotEnv() {
    Properties properties = new Properties();
    if (Files.isRegularFile(ENV_FILE)) {
      try (Reader reader = Files.newBufferedReader(ENV_FILE, StandardCharsets.UTF_8)) {
        properties.load(reader);
      } catch (IOException e) {
        throw new IllegalStateException("Не удалось прочитать файл .env: " + e.getMessage(), e);
      }
    }
    return properties;
  }

  private static String readSetting(String name, Properties dotEnv) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      value = dotEnv.getProperty(name);
    }
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
          "Не задана настройка "
              + name
              + " (переменная окружения или файл .env, см. .env.example)");
    }
    return value.trim();
  }
}
