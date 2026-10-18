package rentalmarketplace.fx.support;

import java.util.List;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class Tables {
  private Tables() {}

  public static <R, V> void column(TableColumn<R, V> column, Function<R, V> value) {
    column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
  }

  public static <R, V> void column(
      TableColumn<R, V> column, Function<R, V> value, Function<V, String> format) {
    column(column, value);
    column.setCellFactory(c -> TableCells.text(format));
  }

  public static <T> void showSorted(TableView<T> table, ObservableList<T> items) {
    SortedList<T> sorted = new SortedList<>(items);
    sorted.comparatorProperty().bind(table.comparatorProperty());
    table.setItems(sorted);
  }

  public static <T, K> void reselect(
      TableView<T> table, List<T> items, T previous, Function<T, K> idOf) {
    if (previous == null) {
      return;
    }
    items.stream()
        .filter(item -> idOf.apply(item).equals(idOf.apply(previous)))
        .findFirst()
        .ifPresent(item -> table.getSelectionModel().select(item));
  }
}
