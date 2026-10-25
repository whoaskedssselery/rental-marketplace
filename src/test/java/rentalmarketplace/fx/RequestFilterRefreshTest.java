package rentalmarketplace.fx;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import rentalmarketplace.fx.controller.RequestFormController;
import rentalmarketplace.fx.controller.RequestsController;
import rentalmarketplace.fx.controller.UserFormController;
import rentalmarketplace.fx.support.Dialogs;
import rentalmarketplace.fx.support.FormValidator;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.FakeListingRepository;
import rentalmarketplace.repository.FakeRentalRequestRepository;
import rentalmarketplace.repository.FakeUserRepository;
import rentalmarketplace.service.ListingService;
import rentalmarketplace.service.RentalRequestService;
import rentalmarketplace.service.RequestSearchService;
import rentalmarketplace.service.UserService;

// Requires a desktop JavaFX toolkit: mvn -Djavafx.tests=true test
@EnabledIfSystemProperty(named = "javafx.tests", matches = "true")
class RequestFilterRefreshTest {
  @BeforeAll
  static void startJavaFx() {
    FxTestSupport.start();
  }

  @Test
  void filterControlsRemainAlignedAtTheMinimumWindowWidth() throws Exception {
    onFxThread(
        () -> {
          Fixture fixture = new Fixture();
          Scene scene = new Scene(fixture.root, 876, 600);
          scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
          fixture.root.applyCss();
          fixture.root.resize(876, 600);
          fixture.root.layout();
          var from = fixture.root.lookup("#fromPicker");
          var to = fixture.root.lookup("#toPicker");
          var sort = fixture.root.lookup("#sortBox");
          var apply =
              fixture.root.lookupAll(".button").stream()
                  .map(Button.class::cast)
                  .filter(button -> button.getText().equals("Применить"))
                  .findFirst()
                  .orElseThrow();
          double y = from.localToScene(from.getBoundsInLocal()).getMinY();

          assertEquals(y, to.localToScene(to.getBoundsInLocal()).getMinY(), 0.5);
          assertEquals(y, sort.localToScene(sort.getBoundsInLocal()).getMinY(), 0.5);
          assertEquals(y, apply.localToScene(apply.getBoundsInLocal()).getMinY(), 0.5);
          assertTrue(apply.localToScene(apply.getBoundsInLocal()).getMaxX() < 876);
          return null;
        });
  }

  @Test
  void dialogShowsErrorsBesideFieldsAndClearsThemWhenEdited() throws Exception {
    onFxThread(
        () -> {
          FXMLLoader loader = new AppContext().loader("/fxml/user-form.fxml");
          Parent form = loader.load();
          UserFormController controller = loader.getController();
          FutureTask<Void> inspect =
              new FutureTask<>(
                  () -> {
                    try {
                      DialogPane pane = (DialogPane) form.getScene().getRoot();
                      var save =
                          pane.getButtonTypes().stream()
                              .filter(type -> type.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                              .findFirst()
                              .orElseThrow();
                      ((Button) pane.lookupButton(save)).fire();
                      Label error = (Label) form.lookup("#validation-error-nameField");
                      assertTrue(error != null && error.isManaged() && error.isVisible());
                      TextField name = (TextField) form.lookup("#nameField");
                      assertTrue(
                          name.getPseudoClassStates()
                              .contains(PseudoClass.getPseudoClass("invalid")));

                      name.setText("Анна");

                      assertFalse(error.isManaged());
                      assertFalse(
                          name.getPseudoClassStates()
                              .contains(PseudoClass.getPseudoClass("invalid")));
                      return null;
                    } finally {
                      form.getScene().getWindow().hide();
                    }
                  });
          Platform.runLater(inspect);
          assertFalse(Dialogs.saveForm("Новый пользователь", form, controller::validate, () -> {}));
          inspect.get(10, TimeUnit.SECONDS);
          return null;
        });
  }

  @Test
  void dialogScrollsToAnInvalidFieldAboveTheViewport() throws Exception {
    onFxThread(
        () -> {
          TextField input = new TextField();
          input.setId("firstField");
          VBox field = new VBox(6, new Label("Имя"), input);
          field.setUserData("Имя");
          Region spacer = new Region();
          spacer.setMinHeight(1000);
          spacer.setPrefHeight(1000);
          VBox form = new VBox(16, field, spacer);
          FutureTask<Void> verify =
              new FutureTask<>(
                  () -> {
                    try {
                      ScrollPane scroll =
                          (ScrollPane) form.getScene().getRoot().lookup(".form-scroll");
                      assertTrue(scroll.getVvalue() < 0.1);
                      return null;
                    } finally {
                      form.getScene().getWindow().hide();
                    }
                  });
          Platform.runLater(
              () -> {
                DialogPane pane = (DialogPane) form.getScene().getRoot();
                ScrollPane scroll = (ScrollPane) pane.lookup(".form-scroll");
                scroll.setVvalue(1);
                var save =
                    pane.getButtonTypes().stream()
                        .filter(type -> type.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                        .findFirst()
                        .orElseThrow();
                ((Button) pane.lookupButton(save)).fire();
                Platform.runLater(verify);
              });
          Dialogs.saveForm(
              "Тест формы",
              form,
              () -> {
                FormValidator validator = new FormValidator();
                validator.requiredText("Имя", input.getText(), 150);
                return validator.result();
              },
              () -> {});
          verify.get(10, TimeUnit.SECONDS);
          return null;
        });
  }

  @Test
  void refreshAddsNewOwnersAndPreservesTheSelectedOwner() throws Exception {
    onFxThread(
        () -> {
          Fixture fixture = new Fixture();
          fixture.owners().setValue(fixture.owner);
          User added =
              fixture.users.createUser("New owner", "new@example.com", null, UserRole.OWNER);

          fixture.controller.refresh();

          assertTrue(
              fixture.owners().getItems().stream()
                  .anyMatch(user -> user != null && user.getId().equals(added.getId())));
          assertEquals(fixture.owner.getId(), fixture.owners().getValue().getId());
          return null;
        });
  }

  @Test
  @SuppressWarnings("unchecked")
  void totalPreviewShowsOverflowInlineWithoutThrowingFromDateChange() throws Exception {
    onFxThread(
        () -> {
          Fixture fixture = new Fixture();
          Listing listing = fixture.listings.getAllListings().getFirst();
          fixture.listings.updateListing(
              listing.getId(),
              fixture.owner.getId(),
              "Expensive tool",
              "",
              new BigDecimal("60000000.00"),
              "Tools",
              true);
          FXMLLoader loader = fixture.context.loader("/fxml/request-form.fxml");
          Parent form = readyForm(loader);
          RequestFormController controller = loader.getController();
          controller.prepareForCreate();
          ((ComboBox<Listing>) form.lookup("#listingBox")).setValue(listing);
          ((DatePicker) form.lookup("#startPicker")).setValue(LocalDate.of(2035, 1, 1));

          assertDoesNotThrow(
              () -> ((DatePicker) form.lookup("#endPicker")).setValue(LocalDate.of(2035, 1, 3)));

          Label total = (Label) form.lookup("#totalLabel");
          assertTrue(total.getStyleClass().contains("error-text"));
          assertTrue(total.getText().contains("99 999 999,99"));
          return null;
        });
  }

  @Test
  @SuppressWarnings("unchecked")
  void formAcceptsTypedDatesBeforeTheDatePickerLosesFocus() throws Exception {
    onFxThread(
        () -> {
          Fixture fixture = new Fixture();
          FXMLLoader loader = fixture.context.loader("/fxml/request-form.fxml");
          Parent form = readyForm(loader);
          RequestFormController controller = loader.getController();
          controller.prepareForCreate();
          ((ComboBox<Listing>) form.lookup("#listingBox")).getSelectionModel().selectFirst();
          ((ComboBox<User>) form.lookup("#renterBox")).getSelectionModel().selectLast();
          ((DatePicker) form.lookup("#startPicker")).getEditor().setText("01.01.2035");
          ((DatePicker) form.lookup("#endPicker")).getEditor().setText("03.01.2035");

          assertTrue(controller.validate().isValid());
          assertEquals(LocalDate.of(2035, 1, 1), controller.values().startDate());
          assertEquals(LocalDate.of(2035, 1, 3), controller.values().endDate());
          assertTrue(((Label) form.lookup("#totalLabel")).getText().contains("20,00"));
          return null;
        });
  }

  @Test
  @SuppressWarnings("unchecked")
  void formRejectsMalformedTypedDateInsteadOfSavingThePreviousDate() throws Exception {
    onFxThread(
        () -> {
          Fixture fixture = new Fixture();
          FXMLLoader loader = fixture.context.loader("/fxml/request-form.fxml");
          Parent form = readyForm(loader);
          RequestFormController controller = loader.getController();
          controller.prepareForCreate();
          ((ComboBox<Listing>) form.lookup("#listingBox")).getSelectionModel().selectFirst();
          ((ComboBox<User>) form.lookup("#renterBox")).getSelectionModel().selectLast();
          ((DatePicker) form.lookup("#startPicker")).setValue(LocalDate.of(2035, 1, 1));
          ((DatePicker) form.lookup("#endPicker")).setValue(LocalDate.of(2035, 1, 3));
          ((DatePicker) form.lookup("#startPicker")).getEditor().setText("31.02.2035");

          assertDoesNotThrow(() -> ((DatePicker) form.lookup("#startPicker")).commitValue());
          assertFalse(controller.validate().isValid());
          assertNull(((DatePicker) form.lookup("#startPicker")).getValue());
          return null;
        });
  }

  @Test
  void filterIncludesUsersWhoOwnListingsRegardlessOfTheirRole() throws Exception {
    onFxThread(
        () -> {
          Fixture fixture = new Fixture();
          User ownerWithRenterRole =
              fixture.users.createUser(
                  "Owner with another role", "other-role@example.com", null, UserRole.RENTER);
          fixture.listings.createListing(
              ownerWithRenterRole.getId(), "Tool", "", BigDecimal.TEN, "Tools");

          fixture.controller.refresh();

          assertTrue(
              fixture.owners().getItems().stream()
                  .anyMatch(
                      user -> user != null && user.getId().equals(ownerWithRenterRole.getId())));
          return null;
        });
  }

  @Test
  void refreshClearsDeletedOwnerFromBothTheControlAndAppliedQuery() throws Exception {
    onFxThread(
        () -> {
          Fixture fixture = new Fixture();
          User removable =
              fixture.users.createUser(
                  "Temporary owner", "temporary@example.com", null, UserRole.OWNER);
          fixture.controller.refresh();
          fixture.owners().setValue(removable);
          fixture.root.lookupAll(".button").stream()
              .map(Button.class::cast)
              .filter(button -> button.getText().equals("Применить"))
              .findFirst()
              .orElseThrow()
              .fire();
          assertEquals(0, fixture.table().getItems().size());
          fixture.users.deleteUser(removable.getId());

          fixture.controller.refresh();

          assertNull(fixture.owners().getValue());
          assertEquals(1, fixture.table().getItems().size());
          return null;
        });
  }

  private static Parent readyForm(FXMLLoader loader) throws Exception {
    Parent form = loader.load();
    new Scene(form);
    form.applyCss();
    return form;
  }

  private static <T> T onFxThread(Callable<T> action) throws Exception {
    FutureTask<T> task = new FutureTask<>(action);
    Platform.runLater(task);
    return task.get(10, TimeUnit.SECONDS);
  }

  private static final class Fixture {
    private final UserService users = new UserService(new FakeUserRepository());
    private final ListingService listings = new ListingService(new FakeListingRepository(), users);
    private final RentalRequestService requests =
        new RentalRequestService(new FakeRentalRequestRepository(), listings, users);
    private final RequestSearchService search = new RequestSearchService(requests, listings, users);
    private final User owner = users.createUser("Owner", "owner@example.com", null, UserRole.OWNER);
    private final Parent root;
    private final RequestsController controller;
    private final AppContext context;

    private Fixture() throws Exception {
      User renter = users.createUser("Renter", "renter@example.com", null, UserRole.RENTER);
      var listing = listings.createListing(owner.getId(), "Drill", "", BigDecimal.TEN, "Tools");
      requests.create(
          listing.getId(), renter.getId(), LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 3));
      context =
          new AppContext() {
            @Override
            public UserService userService() {
              return users;
            }

            @Override
            public ListingService listingService() {
              return listings;
            }

            @Override
            public RentalRequestService rentalRequestService() {
              return requests;
            }

            @Override
            public RequestSearchService requestSearchService() {
              return search;
            }
          };
      FXMLLoader loader = context.loader("/fxml/requests.fxml");
      root = loader.load();
      controller = loader.getController();
    }

    @SuppressWarnings("unchecked")
    private ComboBox<User> owners() {
      return (ComboBox<User>) root.lookup("#ownerBox");
    }

    private TableView<?> table() {
      return (TableView<?>) root.lookup("#table");
    }
  }
}
