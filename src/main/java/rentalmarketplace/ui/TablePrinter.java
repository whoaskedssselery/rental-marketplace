package rentalmarketplace.ui;

import java.util.List;
import rentalmarketplace.model.Displayable;

public final class TablePrinter {
  private TablePrinter() {}

  public static void print(List<? extends Displayable> rows, String header, String emptyMessage) {
    if (rows.isEmpty()) {
      System.out.println(emptyMessage);
      return;
    }
    System.out.println(header);
    rows.forEach(row -> System.out.println(row.toTableRow()));
  }
}
