package rentalmarketplace.fx;

import java.io.IOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rentalmarketplace.util.DatabaseManager;

public class FxApplication extends Application {
  private static final Logger LOG = LoggerFactory.getLogger(FxApplication.class);
  private static final String TITLE = "Маркетплейс аренды";

  @Override
  public void start(Stage stage) {
    if (!DatabaseManager.isAvailable()) {
      showStartupError(
          "Не удалось подключиться к базе данных",
          "Проверьте, что Docker запущен и контейнер с PostgreSQL работает "
              + "(docker compose up -d), а настройки подключения в файле .env верны.");
      Platform.exit();
      return;
    }
    try {
      Parent root = new AppContext().loader("/fxml/main.fxml").load();
      Scene scene = new Scene(root, 1100, 700);
      scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
      stage.setTitle(TITLE);
      stage.setMinWidth(900);
      stage.setMinHeight(600);
      stage.setScene(scene);
      stage.show();
    } catch (IOException | RuntimeException e) {
      LOG.error("Не удалось построить главное окно", e);
      showStartupError("Не удалось открыть главное окно", "Подробности записаны в журнал.");
      Platform.exit();
    }
  }

  private void showStartupError(String header, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(TITLE);
    alert.setHeaderText(header);
    alert.setContentText(message);
    alert.showAndWait();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
