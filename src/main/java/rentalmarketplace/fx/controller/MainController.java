package rentalmarketplace.fx.controller;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rentalmarketplace.fx.AppContext;
import rentalmarketplace.fx.support.Dialogs;
import rentalmarketplace.fx.support.Exportable;
import rentalmarketplace.fx.support.FileDialogs;
import rentalmarketplace.fx.support.Refreshable;
import rentalmarketplace.fx.support.UserMessages;
import rentalmarketplace.service.SheetData;

public class MainController {
  private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

  private final AppContext context;

  @FXML private TabPane tabPane;
  @FXML private Button exportButton;
  @FXML private Label statusLabel;
  @FXML private HBox statusBar;

  @FXML private Object requestsController;
  @FXML private Object listingsController;
  @FXML private Object usersController;
  @FXML private Object reviewsController;
  @FXML private Object statisticsController;

  private List<Object> tabControllers;

  public MainController(AppContext context) {
    this.context = context;
  }

  @FXML
  private void initialize() {
    statusBar.visibleProperty().bind(statusLabel.textProperty().isNotEmpty());
    statusBar.managedProperty().bind(statusBar.visibleProperty());
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
      statusLabel.setText("");
      try {
        refreshable.refresh();
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
    if (activeController() instanceof Exportable exportable) {
      export(List.of(toSheet(exportable)), exportable.exportName());
    }
  }

  @FXML
  private void onExportAll() {
    List<SheetData> sheets = new ArrayList<>();
    for (Object controller : tabControllers) {
      if (controller instanceof Exportable exportable) {
        sheets.add(toSheet(exportable));
      }
    }
    export(sheets, "Все-данные");
  }

  private SheetData toSheet(Exportable exportable) {
    return new SheetData(exportable.exportName(), exportable.headers(), exportable.rows());
  }

  private void export(List<SheetData> sheets, String baseName) {
    Optional<Path> file = FileDialogs.chooseXlsxToSave(tabPane.getScene().getWindow(), baseName);
    if (file.isEmpty()) {
      return;
    }
    try {
      context.exportService().export(sheets, file.get());
    } catch (RuntimeException e) {
      LOG.error("Не удалось выгрузить данные", e);
      Dialogs.error(UserMessages.of(e));
      return;
    }
    statusLabel.setText("Файл сохранен: " + file.get());
    Dialogs.message("Экспорт выполнен", "Файл сохранен:\n" + file.get());
  }

  private void onTabChanged() {
    exportButton.setDisable(!(activeController() instanceof Exportable));
    statusLabel.setText("");
  }

  private Object activeController() {
    int index = tabPane.getSelectionModel().getSelectedIndex();
    return index >= 0 && index < tabControllers.size() ? tabControllers.get(index) : null;
  }
}
