package rentalmarketplace.fx.support;

import java.util.function.Function;
import javafx.scene.control.TableCell;

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
}
