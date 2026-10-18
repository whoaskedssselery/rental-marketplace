package rentalmarketplace.fx.support;

import java.util.Optional;
import java.util.function.Supplier;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class Dialogs {
  private static final String STYLESHEET = "/css/app.css";
  private static final String APP_TITLE = "Маркетплейс аренды";

  private Dialogs() {}

  public static boolean confirm(String title, String message) {
    ButtonType yes = new ButtonType("Да", ButtonBar.ButtonData.OK_DONE);
    ButtonType no = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, yes, no);
    alert.setTitle(APP_TITLE);
    alert.setHeaderText(title);
    alert.setGraphic(null);
    alert.getDialogPane().getStylesheets().add(stylesheet());
    return alert.showAndWait().filter(button -> button == yes).isPresent();
  }

  public static void error(String message) {
    show(Alert.AlertType.ERROR, "Ошибка", message);
  }

  public static void message(String header, String message) {
    show(Alert.AlertType.INFORMATION, header, message);
  }

  public static void info(String message) {
    show(Alert.AlertType.INFORMATION, "Готово", message);
  }

  public static <T> Optional<T> form(
      String title, Node content, Supplier<ValidationResult> validator, Supplier<T> resultFactory) {
    ButtonType save = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
    ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);

    Label errors = new Label();
    errors.getStyleClass().add("error-text");
    errors.setWrapText(true);
    errors.setVisible(false);
    errors.setManaged(false);

    Dialog<T> dialog = new Dialog<>();
    dialog.setTitle(APP_TITLE);
    dialog.setHeaderText(title);
    DialogPane pane = dialog.getDialogPane();
    pane.getStylesheets().add(stylesheet());
    pane.getButtonTypes().addAll(save, cancel);
    pane.setContent(new VBox(12, content, errors));

    Button saveButton = (Button) pane.lookupButton(save);
    saveButton.getStyleClass().add("primary-button");
    saveButton.addEventFilter(
        ActionEvent.ACTION,
        event -> {
          ValidationResult result = validator.get();
          if (!result.isValid()) {
            errors.setText(result.summary());
            errors.setVisible(true);
            errors.setManaged(true);
            pane.getScene().getWindow().sizeToScene();
            event.consume();
          }
        });
    dialog.setResultConverter(button -> button == save ? resultFactory.get() : null);
    return dialog.showAndWait();
  }

  public static boolean saveForm(
      String title, Node content, Supplier<ValidationResult> validator, Runnable save) {
    ButtonType saveType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
    ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);

    Label errors = new Label();
    errors.getStyleClass().add("error-text");
    errors.setWrapText(true);
    errors.setVisible(false);
    errors.setManaged(false);

    Dialog<Boolean> dialog = new Dialog<>();
    dialog.setTitle(APP_TITLE);
    dialog.setHeaderText(title);
    DialogPane pane = dialog.getDialogPane();
    pane.getStylesheets().add(stylesheet());
    pane.getButtonTypes().addAll(saveType, cancel);
    pane.setContent(new VBox(12, content, errors));

    Button saveButton = (Button) pane.lookupButton(saveType);
    saveButton.getStyleClass().add("primary-button");
    saveButton.addEventFilter(
        ActionEvent.ACTION,
        event -> {
          String problem = null;
          ValidationResult result = validator.get();
          if (!result.isValid()) {
            problem = result.summary();
          } else {
            try {
              save.run();
            } catch (RuntimeException e) {
              problem = UserMessages.of(e);
            }
          }
          if (problem != null) {
            errors.setText(problem);
            errors.setVisible(true);
            errors.setManaged(true);
            pane.getScene().getWindow().sizeToScene();
            event.consume();
          }
        });
    dialog.setResultConverter(button -> button == saveType);
    return dialog.showAndWait().orElse(false);
  }

  private static void show(Alert.AlertType type, String header, String message) {
    Alert alert = new Alert(type, message, ButtonType.OK);
    alert.setTitle(APP_TITLE);
    alert.setHeaderText(header);
    alert.setGraphic(null);
    alert.getDialogPane().getStylesheets().add(stylesheet());
    alert.showAndWait();
  }

  private static String stylesheet() {
    return Dialogs.class.getResource(STYLESHEET).toExternalForm();
  }
}
