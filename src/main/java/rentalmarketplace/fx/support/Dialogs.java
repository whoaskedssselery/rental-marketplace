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
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class Dialogs {
  private static final String STYLESHEET = "/css/app.css";
  private static final String APP_TITLE = "Маркетплейс аренды";

  private Dialogs() {}

  public static boolean confirm(String title, String message) {
    ButtonType yes = new ButtonType("Удалить", ButtonBar.ButtonData.OK_DONE);
    ButtonType no = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, yes, no);
    alert.setTitle(APP_TITLE);
    alert.setHeaderText(title);
    own(alert);
    alert.setGraphic(null);
    alert.getDialogPane().getStylesheets().add(stylesheet());
    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    alert.getDialogPane().setPrefWidth(440);
    ((Button) alert.getDialogPane().lookupButton(yes)).getStyleClass().add("danger-button");
    ((Button) alert.getDialogPane().lookupButton(yes)).setDefaultButton(false);
    ((Button) alert.getDialogPane().lookupButton(no)).setDefaultButton(true);
    return alert.showAndWait().filter(button -> button == yes).isPresent();
  }

  public static void error(String message) {
    show(Alert.AlertType.ERROR, "Ошибка", message);
  }

  public static void error(String header, String message) {
    show(Alert.AlertType.ERROR, header, message);
  }

  public static void message(String header, String message) {
    show(Alert.AlertType.INFORMATION, header, message);
  }

  public static <T> Optional<T> form(
      String title, Node content, Supplier<ValidationResult> validator, Supplier<T> resultFactory) {
    ButtonType save = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
    ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);

    Label errors = new Label();
    errors.getStyleClass().add("form-error");
    errors.setWrapText(true);
    errors.setVisible(false);
    errors.setManaged(false);
    FormErrors fieldErrors = new FormErrors(content, () -> hide(errors));
    ScrollPane body = formBody(content);

    Dialog<T> dialog = new Dialog<>();
    dialog.setTitle(APP_TITLE);
    dialog.setHeaderText(title);
    own(dialog);
    dialog.setResizable(true);
    DialogPane pane = dialog.getDialogPane();
    pane.getStylesheets().add(stylesheet());
    pane.getButtonTypes().addAll(save, cancel);
    pane.getStyleClass().add("form-dialog");
    pane.setPrefWidth(528);
    pane.setContent(new VBox(12, body, errors));
    dialog.setOnShown(event -> resizeForm(pane, body, content));

    Button saveButton = (Button) pane.lookupButton(save);
    saveButton.getStyleClass().add("primary-button");
    saveButton.addEventFilter(
        ActionEvent.ACTION,
        event -> {
          ValidationResult result = validator.get();
          var unmapped = fieldErrors.show(result);
          if (!result.isValid()) {
            errors.setText(String.join("\n", unmapped));
            errors.setVisible(!unmapped.isEmpty());
            errors.setManaged(!unmapped.isEmpty());
            resizeForm(pane, body, content);
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
    errors.getStyleClass().add("form-error");
    errors.setWrapText(true);
    errors.setVisible(false);
    errors.setManaged(false);
    FormErrors fieldErrors = new FormErrors(content, () -> hide(errors));
    ScrollPane body = formBody(content);

    Dialog<Boolean> dialog = new Dialog<>();
    dialog.setTitle(APP_TITLE);
    dialog.setHeaderText(title);
    own(dialog);
    dialog.setResizable(true);
    DialogPane pane = dialog.getDialogPane();
    pane.getStylesheets().add(stylesheet());
    pane.getButtonTypes().addAll(saveType, cancel);
    pane.getStyleClass().add("form-dialog");
    pane.setPrefWidth(528);
    pane.setContent(new VBox(12, body, errors));
    dialog.setOnShown(event -> resizeForm(pane, body, content));

    Button saveButton = (Button) pane.lookupButton(saveType);
    saveButton.getStyleClass().add("primary-button");
    saveButton.addEventFilter(
        ActionEvent.ACTION,
        event -> {
          String problem = null;
          hide(errors);
          ValidationResult result = validator.get();
          var unmapped = fieldErrors.show(result);
          if (!result.isValid()) {
            problem = String.join("\n", unmapped);
          } else {
            try {
              save.run();
            } catch (RuntimeException e) {
              problem = UserMessages.of(e);
            }
          }
          if (problem != null) {
            errors.setText(problem);
            errors.setVisible(!problem.isEmpty());
            errors.setManaged(!problem.isEmpty());
            resizeForm(pane, body, content);
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
    own(alert);
    alert.setGraphic(null);
    alert.getDialogPane().getStylesheets().add(stylesheet());
    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    alert.getDialogPane().setPrefWidth(440);
    alert.showAndWait();
  }

  private static String stylesheet() {
    AppFonts.load();
    return Dialogs.class.getResource(STYLESHEET).toExternalForm();
  }

  private static ScrollPane formBody(Node content) {
    ScrollPane scroll = new ScrollPane(content);
    scroll.getStyleClass().add("form-scroll");
    scroll.setFitToWidth(true);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scroll.setMinHeight(0);
    scroll.setMaxHeight(maxFormHeight());
    return scroll;
  }

  private static void resizeForm(DialogPane pane, ScrollPane body, Node content) {
    pane.applyCss();
    body.setPrefViewportHeight(Math.min(maxFormHeight(), content.prefHeight(480) + 20));
    if (pane.getScene() != null && pane.getScene().getWindow() != null) {
      pane.getScene().getWindow().sizeToScene();
    }
  }

  private static double maxFormHeight() {
    return Math.min(540, Screen.getPrimary().getVisualBounds().getHeight() - 220);
  }

  private static void hide(Label errors) {
    errors.setText("");
    errors.setManaged(false);
    errors.setVisible(false);
  }

  private static void own(Dialog<?> dialog) {
    Window.getWindows().stream()
        .filter(window -> window instanceof Stage && window.isShowing() && window.isFocused())
        .findFirst()
        .ifPresent(dialog::initOwner);
  }
}
