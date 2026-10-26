package rentalmarketplace.fx;

import java.io.IOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rentalmarketplace.fx.controller.AuthenticationController;
import rentalmarketplace.fx.controller.MainController;
import rentalmarketplace.fx.support.AppFonts;
import rentalmarketplace.fx.support.Dialogs;
import rentalmarketplace.util.DatabaseManager;

public class FxApplication extends Application {
  private static final Logger LOG = LoggerFactory.getLogger(FxApplication.class);
  private static final String TITLE = "Маркетплейс аренды";
  private final AppContext context = new AppContext();
  private Scene scene;

  @Override
  public void start(Stage stage) {
    AppFonts.load();
    if (!DatabaseManager.isAvailable()) {
      showStartupError(
          "Не удалось подключиться к базе данных",
          "Проверьте, что Docker запущен и контейнер с PostgreSQL работает "
              + "(docker compose up -d), а настройки подключения в файле .env верны.");
      Platform.exit();
      return;
    }
    try {
      Parent root = authenticationRoot();
      scene = new Scene(root, 1100, 700);
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

  private Parent authenticationRoot() throws IOException {
    FXMLLoader loader = context.loader("/fxml/authentication.fxml");
    Parent root = loader.load();
    AuthenticationController controller = loader.getController();
    controller.setOnAuthenticated(this::showMarketplace);
    return root;
  }

  private void showMarketplace() {
    try {
      FXMLLoader loader = context.loader("/fxml/main.fxml");
      Parent root = loader.load();
      MainController controller = loader.getController();
      controller.setOnLogout(this::showAuthentication);
      scene.setRoot(root);
    } catch (IOException error) {
      throw new IllegalStateException("Не удалось открыть маркетплейс", error);
    }
  }

  private void showAuthentication() {
    try {
      Parent root = authenticationRoot();
      context.authenticationService().logout();
      scene.setRoot(root);
    } catch (IOException error) {
      LOG.error("Не удалось открыть окно входа", error);
      context.authenticationService().logout();
      scene.setRoot(new javafx.scene.layout.StackPane());
      showStartupError("Не удалось открыть окно входа", "Перезапустите приложение.");
    }
  }

  private void showStartupError(String header, String message) {
    Dialogs.error(header, message);
  }

  public static void main(String[] args) {
    launch(args);
  }
}
