package rentalmarketplace.fx;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.fx.controller.RequestFormController;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.FakeListingRepository;
import rentalmarketplace.repository.FakeRentalRequestRepository;
import rentalmarketplace.repository.FakeReviewRepository;
import rentalmarketplace.repository.FakeUserRepository;
import rentalmarketplace.repository.UserRepository;
import rentalmarketplace.security.AccessPolicy;
import rentalmarketplace.service.AuthenticationService;
import rentalmarketplace.service.ListingDetails;
import rentalmarketplace.service.ListingService;
import rentalmarketplace.service.RentalRequestService;
import rentalmarketplace.service.RequestSearchService;
import rentalmarketplace.service.ReviewService;
import rentalmarketplace.service.UserService;
import rentalmarketplace.util.DatabaseManager;

@EnabledIfSystemProperty(named = "javafx.tests", matches = "true")
class AuthenticationWorkflowTest {
  @BeforeAll
  static void startJavaFx() {
    FxTestSupport.start();
  }

  @Test
  void authenticationPanelUsesContentHeightAndCentersAtDesktopSizes() throws Exception {
    onFx(
        () -> {
          for (int[] size : new int[][] {{900, 600}, {1100, 700}}) {
            Parent root = new AppContext().loader("/fxml/authentication.fxml").load();
            Scene scene = new Scene(root, size[0], size[1]);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            for (boolean registration : new boolean[] {false, true}) {
              root.applyCss();
              root.layout();
              if (registration) {
                button(root, "switchModeButton").fire();
                root.applyCss();
                root.layout();
              }
              VBox panel = (VBox) root.lookup(".auth-panel");
              Bounds bounds = panel.localToScene(panel.getBoundsInLocal());
              assertEquals(panel.prefHeight(panel.getWidth()), panel.getHeight(), 1);
              assertEquals(scene.getWidth() / 2, bounds.getCenterX(), 2);
              assertEquals(scene.getHeight() / 2, bounds.getCenterY(), 2);
              assertTrue(bounds.getMinY() >= 0);
              assertTrue(bounds.getMaxY() <= scene.getHeight());
            }
          }
          return null;
        });
  }

  @Test
  void signInFormRejectsInvalidInputBeforeWorkStarts() throws Exception {
    onFx(
        () -> {
          Parent root =
              assertDoesNotThrow(() -> new AppContext().loader("/fxml/authentication.fxml").load());
          new Scene(root);
          root.applyCss();
          assertNotNull(
              root.lookup("#passwordField"), "Startup must show sign-in before account data");
          button(root, "submitButton").fire();
          assertTrue(root.lookup("#validation-error-emailField").isVisible());
          assertTrue(root.lookup("#validation-error-passwordField").isVisible());
          assertNull(root.lookup("#tabPane"));
          button(root, "switchModeButton").fire();
          field(root, "nameField").setText("New Account");
          field(root, "emailField").setText("new@test.com");
          field(root, "passwordField").setText("too short");
          button(root, "submitButton").fire();
          assertTrue(root.lookup("#validation-error-passwordField").isVisible());
          assertFalse(button(root, "submitButton").isDisabled());
          return null;
        });
  }

  @Test
  void pendingSubmissionRunsOffFxThreadAndPreventsDuplicateAttempts() throws Exception {
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger attempts = new AtomicInteger();
    AtomicBoolean ranOnFx = new AtomicBoolean();
    AuthenticationService service =
        new AuthenticationService(new UserRepository()) {
          @Override
          public void initialize() {}

          @Override
          public User login(String email, String password) {
            attempts.incrementAndGet();
            ranOnFx.set(Platform.isFxApplicationThread());
            try {
              if (!release.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Test timed out");
              }
            } catch (InterruptedException error) {
              Thread.currentThread().interrupt();
              throw new IllegalStateException(error);
            }
            throw new BusinessRuleException("Проверьте email и пароль");
          }
        };
    Parent root =
        onFx(
            () -> {
              AppContext context =
                  new AppContext() {
                    @Override
                    public AuthenticationService authenticationService() {
                      return service;
                    }
                  };
              Parent form = context.loader("/fxml/authentication.fxml").load();
              new Scene(form);
              form.applyCss();
              field(form, "emailField").setText("pending@test.com");
              field(form, "passwordField").setText("test passphrase");
              button(form, "submitButton").fire();
              assertTrue(button(form, "submitButton").isDisabled());
              assertTrue(button(form, "switchModeButton").isDisabled());
              assertTrue(field(form, "passwordField").getText().isEmpty());
              button(form, "submitButton").fire();
              return form;
            });
    try {
      await(() -> attempts.get() == 1);
      assertFalse(ranOnFx.get());
    } finally {
      release.countDown();
    }
    await(() -> !button(root, "submitButton").isDisabled());
    onFx(
        () -> {
          assertTrue(root.lookup("#errorLabel").isVisible());
          assertTrue(field(root, "passwordField").getText().isEmpty());
          assertEquals(1, attempts.get());
          return null;
        });
  }

  @Test
  void registrationLoginAndLogoutSwitchRootsAndClearSecrets() throws Exception {
    org.junit.jupiter.api.Assumptions.assumeTrue(DatabaseManager.isAvailable());
    String email = "ui-" + UUID.randomUUID() + "@test.com";
    String password = "a long test passphrase";
    Stage stage =
        onFx(
            () -> {
              Stage value = new Stage();
              new FxApplication().start(value);
              Parent root = value.getScene().getRoot();
              assertNull(root.lookup("#tabPane"));
              button(root, "switchModeButton").fire();
              field(root, "nameField").setText("UI Account");
              field(root, "emailField").setText(email);
              field(root, "passwordField").setText(password);
              button(root, "submitButton").fire();
              assertTrue(button(root, "submitButton").isDisabled());
              assertTrue(field(root, "passwordField").getText().isEmpty());
              return value;
            });
    try {
      await(() -> stage.getScene().getRoot().lookup("#logoutButton") != null);
      onFx(
          () -> {
            Parent root = stage.getScene().getRoot();
            assertEquals("UI Account", ((Label) root.lookup("#currentUserLabel")).getText());
            stage.setWidth(900);
            Label accountLabel = (Label) root.lookup("#currentUserLabel");
            accountLabel.setText("Очень длинное имя участника маркетплейса".repeat(3));
            root.applyCss();
            root.layout();
            double rowY =
                root.lookup(".app-title")
                    .localToScene(root.lookup(".app-title").getBoundsInLocal())
                    .getCenterY();
            double previousRight = 0;
            for (String selector :
                List.of(
                    ".app-title",
                    "#refreshButton",
                    "#exportAllButton",
                    "#exportButton",
                    "#currentUserLabel",
                    "#logoutButton")) {
              var node = root.lookup(selector);
              Bounds bounds = node.localToScene(node.getBoundsInLocal());
              assertEquals(rowY, bounds.getCenterY(), 1, "Header must occupy one row");
              assertTrue(bounds.getMinX() >= previousRight, "Header controls must not overlap");
              assertTrue(bounds.getMaxX() <= stage.getScene().getWidth());
              previousRight = bounds.getMaxX();
            }
            accountLabel.setText("UI Account");
            button(root, "logoutButton").fire();
            assertNull(stage.getScene().getRoot().lookup("#tabPane"));
            Parent auth = stage.getScene().getRoot();
            auth.applyCss();
            auth.layout();
            assertTrue(field(auth, "passwordField").getText().isEmpty());
            field(auth, "emailField").setText(email.toUpperCase());
            field(auth, "passwordField").setText("wrong passphrase");
            button(auth, "submitButton").fire();
            return null;
          });
      await(() -> !button(stage.getScene().getRoot(), "submitButton").isDisabled());
      onFx(
          () -> {
            Parent auth = stage.getScene().getRoot();
            assertTrue(auth.lookup("#errorLabel").isVisible());
            assertTrue(field(auth, "passwordField").getText().isEmpty());
            field(auth, "passwordField").setText(password);
            button(auth, "submitButton").fire();
            return null;
          });
      await(() -> stage.getScene().getRoot().lookup("#logoutButton") != null);
    } finally {
      onFx(
          () -> {
            stage.close();
            return null;
          });
      try (var connection = DatabaseManager.getConnection();
          var statement =
              connection.prepareStatement("DELETE FROM users WHERE lower(email) = lower(?)")) {
        statement.setString(1, email);
        statement.executeUpdate();
      }
    }
  }

  @Test
  void accountOwnershipControlsFormsAndActionsAcrossSessions() throws Exception {
    onFx(
        () -> {
          AccountUi context = new AccountUi();
          User alice =
              context.accounts.save(new User("Alice", "alice@test.com", null, UserRole.RENTER));
          User bob = context.accounts.save(new User("Bob", "bob@test.com", null, UserRole.OWNER));
          context.actor.set(alice);
          Listing aliceListing =
              context.listings.createListing(
                  alice.getId(), "Alice drill", null, BigDecimal.TEN, "Tools");
          context.actor.set(bob);
          Listing bobListing =
              context.listings.createListing(bob.getId(), "Bob saw", null, BigDecimal.ONE, "Tools");
          var booking =
              context.requests.create(
                  aliceListing.getId(),
                  bob.getId(),
                  LocalDate.of(2030, 1, 1),
                  LocalDate.of(2030, 1, 3));

          Parent listingForm = load(context, "/fxml/listing-form.fxml");
          ComboBox<?> ownerBox = (ComboBox<?>) listingForm.lookup("#ownerBox");
          assertTrue(ownerBox.isDisabled());
          assertEquals(bob.getId(), ((User) ownerBox.getValue()).getId());
          assertEquals(1, ownerBox.getItems().size());

          FXMLLoader requestLoader = context.loader("/fxml/request-form.fxml");
          Parent requestForm = requestLoader.load();
          RequestFormController requestController = requestLoader.getController();
          requestController.prepareForCreate();
          ComboBox<?> renterBox = (ComboBox<?>) requestForm.lookup("#renterBox");
          assertTrue(renterBox.isDisabled());
          assertEquals(bob.getId(), ((User) renterBox.getValue()).getId());
          ComboBox<?> listingBox = (ComboBox<?>) requestForm.lookup("#listingBox");
          assertEquals(List.of(aliceListing), listingBox.getItems());

          Parent listings = load(context, "/fxml/listings.fxml");
          TableView<?> listingTable = (TableView<?>) listings.lookup("#table");
          selectListing(listingTable, aliceListing.getId());
          assertTrue(button(listings, "editButton").isDisabled());
          assertTrue(button(listings, "deleteButton").isDisabled());
          selectListing(listingTable, bobListing.getId());
          assertFalse(button(listings, "editButton").isDisabled());

          Parent renterRequests = load(context, "/fxml/requests.fxml");
          ((TableView<?>) renterRequests.lookup("#table")).getSelectionModel().selectFirst();
          assertFalse(button(renterRequests, "editButton").isDisabled());
          assertFalse(button(renterRequests, "deleteButton").isDisabled());
          Parent profile = load(context, "/fxml/users.fxml");
          assertFalse(button(profile, "addButton").isVisible());
          assertFalse(button(profile, "deleteButton").isVisible());
          assertEquals(1, ((TableView<?>) profile.lookup("#table")).getItems().size());

          context.actor.set(alice);
          Parent ownerRequests = load(context, "/fxml/requests.fxml");
          ((TableView<?>) ownerRequests.lookup("#table")).getSelectionModel().selectFirst();
          assertTrue(button(ownerRequests, "editButton").isDisabled());
          assertTrue(button(ownerRequests, "deleteButton").isDisabled());
          assertFalse(button(ownerRequests, "statusButton").isDisabled());
          context.requests.changeStatus(booking.getId(), RentalRequestStatus.CONFIRMED);
          context.requests.changeStatus(booking.getId(), RentalRequestStatus.ACTIVE);
          context.requests.changeStatus(booking.getId(), RentalRequestStatus.COMPLETED);
          context.actor.set(bob);
          context.reviews.create(booking.getId(), 5, "Good");
          Parent ownReviews = load(context, "/fxml/reviews.fxml");
          ((TableView<?>) ownReviews.lookup("#table")).getSelectionModel().selectFirst();
          assertFalse(button(ownReviews, "editButton").isDisabled());
          context.actor.set(alice);
          Parent foreignReviews = load(context, "/fxml/reviews.fxml");
          ((TableView<?>) foreignReviews.lookup("#table")).getSelectionModel().selectFirst();
          assertTrue(button(foreignReviews, "editButton").isDisabled());
          assertTrue(button(foreignReviews, "deleteButton").isDisabled());
          User admin =
              context.accounts.save(new User("Admin", "admin@test.com", null, UserRole.ADMIN));
          context.actor.set(admin);
          Parent adminUsers = load(context, "/fxml/users.fxml");
          assertTrue(button(adminUsers, "addButton").isVisible());
          assertTrue(button(adminUsers, "deleteButton").isVisible());
          assertEquals(3, ((TableView<?>) adminUsers.lookup("#table")).getItems().size());
          return null;
        });
  }

  private static Parent load(AppContext context, String resource) throws Exception {
    Parent root = context.loader(resource).load();
    new Scene(root);
    root.applyCss();
    return root;
  }

  private static void selectListing(TableView<?> table, Integer id) {
    for (int index = 0; index < table.getItems().size(); index++) {
      if (((ListingDetails) table.getItems().get(index)).listingId().equals(id)) {
        table.getSelectionModel().select(index);
        return;
      }
    }
    fail("Listing missing from catalog");
  }

  private static final class AccountUi extends AppContext {
    private final FakeUserRepository accounts = new FakeUserRepository();
    private final AtomicReference<User> actor = new AtomicReference<>();
    private final UserService users = new UserService(accounts, new AccessPolicy(actor::get));
    private final ListingService listings = new ListingService(new FakeListingRepository(), users);
    private final RentalRequestService requests =
        new RentalRequestService(new FakeRentalRequestRepository(), listings, users);
    private final ReviewService reviews =
        new ReviewService(new FakeReviewRepository(), requests, listings, users);

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
      return new RequestSearchService(requests, listings, users);
    }

    @Override
    public ReviewService reviewService() {
      return reviews;
    }
  }

  private static Button button(Parent root, String id) {
    return (Button) root.lookup("#" + id);
  }

  private static TextInputControl field(Parent root, String id) {
    return (TextInputControl) root.lookup("#" + id);
  }

  private static void await(Callable<Boolean> condition) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
    while (System.nanoTime() < deadline) {
      if (onFx(condition)) {
        return;
      }
      Thread.sleep(25);
    }
    fail("Authentication did not finish");
  }

  private static <T> T onFx(Callable<T> action) throws Exception {
    FutureTask<T> task = new FutureTask<>(action);
    Platform.runLater(task);
    return task.get(20, TimeUnit.SECONDS);
  }
}
