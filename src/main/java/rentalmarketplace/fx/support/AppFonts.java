package rentalmarketplace.fx.support;

import java.io.IOException;
import java.io.InputStream;
import javafx.scene.text.Font;

public final class AppFonts {
  private static boolean loaded;

  private AppFonts() {}

  public static synchronized void load() {
    if (loaded) {
      return;
    }
    for (String name : new String[] {"Regular", "Medium", "SemiBold"}) {
      String path = "/fonts/onest/Onest-" + name + ".ttf";
      try (InputStream stream = AppFonts.class.getResourceAsStream(path)) {
        if (stream == null || Font.loadFont(stream, 13) == null) {
          throw new IllegalStateException("Не удалось загрузить шрифт " + path);
        }
      } catch (IOException error) {
        throw new IllegalStateException("Не удалось прочитать шрифт " + path, error);
      }
    }
    loaded = true;
  }
}
