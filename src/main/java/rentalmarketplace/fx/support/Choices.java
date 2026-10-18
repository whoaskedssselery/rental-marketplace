package rentalmarketplace.fx.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

public final class Choices {
  private Choices() {}

  public static <T> void setup(
      ComboBox<T> box, String allLabel, List<T> values, Function<T, String> label) {
    List<T> choices = new ArrayList<>();
    choices.add(null);
    choices.addAll(values);
    box.setItems(FXCollections.observableArrayList(choices));
    box.setCellFactory(list -> cell(allLabel, label));
    box.setButtonCell(cell(allLabel, label));
    box.getSelectionModel().selectFirst();
  }

  private static <T> ListCell<T> cell(String allLabel, Function<T, String> label) {
    return new ListCell<>() {
      @Override
      protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty ? null : item == null ? allLabel : label.apply(item));
      }
    };
  }
}
