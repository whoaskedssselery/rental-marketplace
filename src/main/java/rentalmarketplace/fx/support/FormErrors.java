package rentalmarketplace.fx.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class FormErrors {
  private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");

  private record Field(Control control, Label error, String accessibleHelp) {}

  private final Map<String, Field> fields = new LinkedHashMap<>();
  private final Runnable onEdit;

  public FormErrors(Node content, Runnable onEdit) {
    this.onEdit = onEdit;
    collect(content);
  }

  public List<String> show(ValidationResult result) {
    fields.values().forEach(this::clear);
    List<String> unmapped = new ArrayList<>();
    Control first = null;
    for (ValidationResult.FieldError problem : result.errors()) {
      Field field = fields.get(problem.field());
      if (field == null) {
        unmapped.add(problem.text());
        continue;
      }
      String text = field.error().getText();
      field.error().setText(text.isEmpty() ? problem.message() : text + "\n" + problem.message());
      field.error().setVisible(true);
      field.error().setManaged(true);
      field.control().pseudoClassStateChanged(INVALID, true);
      field.control().setAccessibleHelp(field.error().getText());
      if (first == null) {
        first = field.control();
      }
    }
    if (first != null) {
      first.requestFocus();
      reveal(first);
    }
    return unmapped;
  }

  private void collect(Node node) {
    if (node instanceof VBox group && group.getUserData() instanceof String key) {
      Control input =
          group.getChildren().stream()
              .filter(child -> child instanceof Control && !(child instanceof Label))
              .map(Control.class::cast)
              .findFirst()
              .orElse(null);
      if (input != null) {
        group.getChildren().stream()
            .filter(Label.class::isInstance)
            .map(Label.class::cast)
            .findFirst()
            .ifPresent(label -> label.setLabelFor(input));
        Label error = new Label();
        error.setId("validation-error-" + input.getId());
        error.getStyleClass().add("field-error");
        error.setWrapText(true);
        error.setMinHeight(Region.USE_PREF_SIZE);
        Field field = new Field(input, error, input.getAccessibleHelp());
        fields.put(key, field);
        clear(field);
        group.getChildren().add(error);
        if (input instanceof TextInputControl text) {
          text.textProperty().addListener((o, was, now) -> edited(field));
        } else if (input instanceof ComboBox<?> box) {
          box.valueProperty().addListener((o, was, now) -> edited(field));
          if (box.isEditable()) {
            box.getEditor().textProperty().addListener((o, was, now) -> edited(field));
          }
        } else if (input instanceof DatePicker picker) {
          picker.valueProperty().addListener((o, was, now) -> edited(field));
          picker.getEditor().textProperty().addListener((o, was, now) -> edited(field));
        }
      }
    }
    if (node instanceof Parent parent) {
      List.copyOf(parent.getChildrenUnmodifiable()).forEach(this::collect);
    }
  }

  private void edited(Field field) {
    clear(field);
    onEdit.run();
  }

  private void reveal(Control input) {
    Node target = input.getParent();
    for (Parent parent = input.getParent(); parent != null; parent = parent.getParent()) {
      if (parent instanceof ScrollPane scroll) {
        Platform.runLater(
            () -> {
              if (input.getScene() == null) {
                return;
              }
              scroll.applyCss();
              scroll.layout();
              Node content = scroll.getContent();
              Bounds bounds = content.sceneToLocal(target.localToScene(target.getBoundsInLocal()));
              double travel =
                  content.getLayoutBounds().getHeight() - scroll.getViewportBounds().getHeight();
              scroll.setVvalue(
                  travel <= 0 ? 0 : Math.max(0, Math.min(1, (bounds.getMinY() - 8) / travel)));
            });
        return;
      }
    }
  }

  private void clear(Field field) {
    field.error().setText("");
    field.error().setVisible(false);
    field.error().setManaged(false);
    field.control().pseudoClassStateChanged(INVALID, false);
    field.control().setAccessibleHelp(field.accessibleHelp());
  }
}
