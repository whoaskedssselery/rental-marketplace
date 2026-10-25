package rentalmarketplace.fx.support;

import java.util.function.Function;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public final class TableCells {
  private TableCells() {}

  public static <R, T> TableCell<R, T> text(Function<T, String> format) {
    return new TableCell<>() {
      @Override
      protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty || item == null ? null : format.apply(item));
      }
    };
  }

  public static <R, T> TableCell<R, T> status(
      Function<T, String> format, Function<T, String> styleClass) {
    return new TableCell<>() {
      private final Label label = new Label();
      private final HBox line = createLine();

      private HBox createLine() {
        Region dot = new Region();
        dot.getStyleClass().add("status-dot");
        label.getStyleClass().add("status-label");
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);
        HBox content = new HBox(7, dot, label);
        content.setAlignment(Pos.CENTER_LEFT);
        content.prefWidthProperty().bind(widthProperty().subtract(20));
        return content;
      }

      @Override
      protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setGraphic(null);
          setAccessibleText(null);
          return;
        }
        String text = format.apply(item);
        label.setText(text);
        line.getStyleClass().setAll("status-line", styleClass.apply(item));
        setAccessibleText(text);
        setGraphic(line);
      }
    };
  }
}
