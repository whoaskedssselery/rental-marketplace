package rentalmarketplace.fx;

import javafx.application.Platform;

final class FxTestSupport {
  private static boolean started;

  private FxTestSupport() {}

  static synchronized void start() {
    if (!started) {
      Platform.startup(() -> Platform.setImplicitExit(false));
      started = true;
    }
  }
}
