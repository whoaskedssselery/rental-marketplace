package rentalmarketplace.fx.support;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public final class FileDialogs {
  private static final String EXTENSION = ".xlsx";
  private static File lastDirectory;

  private FileDialogs() {}

  public static Optional<Path> chooseXlsxToSave(Window owner, String baseName) {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Сохранить таблицу Excel");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Книга Excel", "*.xlsx"));
    chooser.setInitialFileName(baseName + "-" + LocalDate.now() + EXTENSION);
    if (lastDirectory != null && lastDirectory.isDirectory()) {
      chooser.setInitialDirectory(lastDirectory);
    }
    File chosen = chooser.showSaveDialog(owner);
    if (chosen == null) {
      return Optional.empty();
    }
    lastDirectory = chosen.getParentFile();
    Path path = chosen.toPath();
    if (!path.getFileName().toString().toLowerCase().endsWith(EXTENSION)) {
      path = path.resolveSibling(path.getFileName() + EXTENSION);
    }
    return Optional.of(path);
  }
}
