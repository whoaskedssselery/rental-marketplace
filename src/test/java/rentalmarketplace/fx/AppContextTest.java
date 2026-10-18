package rentalmarketplace.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.Test;

class AppContextTest {

  @Test
  void createsAllServices() {
    AppContext context = new AppContext();

    assertNotNull(context.userService());
    assertNotNull(context.listingService());
    assertNotNull(context.rentalRequestService());
    assertNotNull(context.reviewService());
    assertNotNull(context.statisticsService());
    assertNotNull(context.reportService());
  }

  @Test
  void returnsTheSameServiceEveryTime() {
    AppContext context = new AppContext();

    assertSame(context.userService(), context.userService());
  }

  @Test
  void controllerFactoryPassesContextWhenConstructorAcceptsIt() throws Exception {
    AppContext context = new AppContext();
    FXMLLoader loader = context.loader("/fxml/main.fxml");

    Object controller = loader.getControllerFactory().call(WithContext.class);

    assertEquals(context, assertInstanceOf(WithContext.class, controller).context);
  }

  @Test
  void controllerFactoryFallsBackToDefaultConstructor() {
    FXMLLoader loader = new AppContext().loader("/fxml/main.fxml");

    Object controller = loader.getControllerFactory().call(WithoutContext.class);

    assertInstanceOf(WithoutContext.class, controller);
  }

  @Test
  void controllerFactoryRejectsUnconstructableController() {
    FXMLLoader loader = new AppContext().loader("/fxml/main.fxml");

    assertThrows(
        IllegalStateException.class, () -> loader.getControllerFactory().call(Broken.class));
  }

  @Test
  void loaderRejectsMissingFile() {
    AppContext context = new AppContext();

    assertThrows(IllegalArgumentException.class, () -> context.loader("/fxml/missing.fxml"));
  }

  public static class WithContext {
    private final AppContext context;

    public WithContext(AppContext context) {
      this.context = context;
    }
  }

  public static class WithoutContext {
    public WithoutContext() {}
  }

  public static class Broken {
    private Broken() {}
  }
}
