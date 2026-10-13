package rentalmarketplace.fx.controller;

import java.util.Arrays;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rentalmarketplace.fx.support.Exportable;
import rentalmarketplace.fx.support.Refreshable;

public class MainController {
  private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

  @FXML private TabPane tabPane;
  @FXML private Button exportButton;
  @FXML private Label statusLabel;

  @FXML private Object requestsController;
  @FXML private Object listingsController;
  @FXML private Object usersController;
  @FXML private Object reviewsController;
  @FXML private Object statisticsController;

  private List<Object> tabControllers;

  @FXML
  private void initialize() {
    tabControllers =
        Arrays.asList(
            requestsController,
            listingsController,
            usersController,
            reviewsController,
            statisticsController);
    tabPane
        .getSelectionModel()
        .selectedIndexProperty()
        .addListener((o, was, now) -> onTabChanged());
    onTabChanged();
  }

  @FXML
  private void onRefresh() {
    if (activeController() instanceof Refreshable refreshable) {
      try {
        refreshable.refresh();
        statusLabel.setText("Данные обновлены");
      } catch (RuntimeException e) {
        LOG.error("Не удалось обновить данные", e);
        statusLabel.setText("Не удалось обновить данные, подробности записаны в журнал");
      }
    } else {
      statusLabel.setText("На этой вкладке нечего обновлять");
    }
  }

  @FXML
  private void onExport() {
    statusLabel.setText("Экспорт данных будет подключен в задаче КР2-09");
  }

  private void onTabChanged() {
    exportButton.setDisable(!(activeController() instanceof Exportable));
    statusLabel.setText("Готово");
  }

  private Object activeController() {
    int index = tabPane.getSelectionModel().getSelectedIndex();
    return index >= 0 && index < tabControllers.size() ? tabControllers.get(index) : null;
  }
}
