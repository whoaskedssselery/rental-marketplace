package rentalmarketplace.fx.support;

import java.io.IOException;
import java.util.Optional;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rentalmarketplace.fx.AppContext;

public final class FormLoader {
  private static final Logger LOG = LoggerFactory.getLogger(FormLoader.class);

  public record Loaded<C>(Node node, C controller) {}

  private FormLoader() {}

  public static <C> Optional<Loaded<C>> load(AppContext context, String resourcePath) {
    try {
      FXMLLoader loader = context.loader(resourcePath);
      Node node = loader.load();
      return Optional.of(new Loaded<>(node, loader.getController()));
    } catch (IOException | RuntimeException e) {
      LOG.error("Не удалось открыть форму {}", resourcePath, e);
      Dialogs.error(UserMessages.of(e));
      return Optional.empty();
    }
  }
}
