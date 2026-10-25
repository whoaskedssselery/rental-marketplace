package rentalmarketplace.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import rentalmarketplace.exception.DatabaseAccessException;
import rentalmarketplace.fx.support.Exportable;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.ListingRepository;
import rentalmarketplace.repository.RentalRequestRepository;
import rentalmarketplace.repository.ReviewRepository;
import rentalmarketplace.repository.UserRepository;
import rentalmarketplace.service.ExportService;
import rentalmarketplace.service.ListingDetails;
import rentalmarketplace.service.RequestDetails;
import rentalmarketplace.service.RequestQuery;
import rentalmarketplace.service.RequestSearchService;
import rentalmarketplace.service.RequestSort;
import rentalmarketplace.service.ReviewDetails;
import rentalmarketplace.service.ReviewableRequest;
import rentalmarketplace.service.SheetData;
import rentalmarketplace.util.DatabaseManager;

@EnabledIfSystemProperty(named = "javafx.tests", matches = "true")
class RequirementsWorkflowTest {
  @BeforeAll
  static void startJavaFx() {
    FxTestSupport.start();
    org.junit.jupiter.api.Assumptions.assumeTrue(DatabaseManager.isAvailable());
  }

  @Test
  void failedRefreshDoesNotReportSuccess() throws Exception {
    onFx(
        () -> {
          AtomicBoolean fail = new AtomicBoolean();
          AppContext context =
              new AppContext() {
                @Override
                public RequestSearchService requestSearchService() {
                  return new RequestSearchService(
                      rentalRequestService(), listingService(), userService()) {
                    @Override
                    public List<RequestDetails> query(RequestQuery query) {
                      if (fail.get()) {
                        throw new DatabaseAccessException(
                            "Ошибка тестового обновления", new IllegalStateException("test"));
                      }
                      return super.query(query);
                    }
                  };
                }
              };
          Ui ui = new Ui(context);
          try {
            fail.set(true);
            inDialog(ui::refresh, pane -> button(pane, "OK").fire());
            assertEquals("", ((Label) ui.root.lookup("#statusLabel")).getText());
            assertFalse(ui.root.lookup("#statusBar").isManaged());
          } finally {
            ui.close();
          }
          return null;
        });
  }

  @Test
  void realFormsPersistFullRentalLifecycleAndRelatedEntities() throws Exception {
    onFx(
        () -> {
          String marker = "Workflow-" + UUID.randomUUID().toString().substring(0, 8);
          AppContext context = new AppContext();
          Ui ui = new Ui(context);
          try {
            Parent users = ui.tab("Пользователи");
            createUser(users, marker + " Owner", marker + "-owner@test.com", UserRole.OWNER);
            createUser(users, marker + " Renter", marker + "-renter@test.com", UserRole.RENTER);
            User owner = new UserRepository().findByEmail(marker + "-owner@test.com").orElseThrow();
            User renter =
                new UserRepository().findByEmail(marker + "-renter@test.com").orElseThrow();
            select(users, row -> ((User) row).getId().equals(renter.getId()));
            inDialog(
                () -> button(users, "Изменить").fire(),
                pane -> {
                  assertEquals(marker + " Renter", text(pane, "nameField").getText());
                  text(pane, "phoneField").setText("+7 900 000-00-00");
                  save(pane);
                });
            assertEquals(
                "+7 900 000-00-00", context.userService().getUserById(renter.getId()).getPhone());

            Parent listings = ui.tab("Объекты");
            inDialog(
                () -> button(listings, "Добавить").fire(),
                pane -> {
                  save(pane);
                  assertTrue(pane.getScene().getWindow().isShowing());
                  assertTrue(pane.lookup("#validation-error-titleField").isVisible());
                  choose(pane, "ownerBox", value -> ((User) value).getId().equals(owner.getId()));
                  text(pane, "titleField").setText(marker + " Drill");
                  text(pane, "priceField").setText("125.50");
                  box(pane, "categoryBox").getEditor().setText("Tools");
                  save(pane);
                });
            Listing listing =
                context.listingService().getAllListings().stream()
                    .filter(value -> value.getTitle().equals(marker + " Drill"))
                    .findFirst()
                    .orElseThrow();
            select(listings, row -> ((ListingDetails) row).listingId().equals(listing.getId()));
            inDialog(
                () -> button(listings, "Изменить").fire(),
                pane -> {
                  assertEquals(marker + " Drill", text(pane, "titleField").getText());
                  text(pane, "priceField").setText("150.25");
                  save(pane);
                });
            assertEquals(
                0,
                new BigDecimal("150.25")
                    .compareTo(
                        context.listingService().getListingById(listing.getId()).getPricePerDay()));

            Parent requests = ui.tab("Заявки");
            inDialog(
                () -> button(requests, "Добавить").fire(),
                pane -> {
                  save(pane);
                  assertTrue(pane.lookup("#validation-error-listingBox").isVisible());
                  choose(
                      pane,
                      "listingBox",
                      value -> ((Listing) value).getId().equals(listing.getId()));
                  choose(pane, "renterBox", value -> ((User) value).getId().equals(renter.getId()));
                  date(pane, "startPicker", LocalDate.of(2035, 1, 1));
                  date(pane, "endPicker", LocalDate.of(2035, 1, 3));
                  save(pane);
                });
            RequestDetails request =
                (RequestDetails)
                    select(
                        requests,
                        row -> ((RequestDetails) row).listingId().equals(listing.getId()));
            assertEquals(0, new BigDecimal("300.50").compareTo(request.totalPrice()));
            inDialog(
                () -> button(requests, "Изменить даты").fire(),
                pane -> {
                  assertEquals(
                      LocalDate.of(2035, 1, 1),
                      ((DatePicker) pane.lookup("#startPicker")).getValue());
                  assertTrue(pane.lookup("#listingBox").isDisabled());
                  date(pane, "endPicker", LocalDate.of(2035, 1, 4));
                  save(pane);
                });
            assertEquals(
                0,
                new BigDecimal("450.75")
                    .compareTo(
                        context
                            .rentalRequestService()
                            .getById(request.requestId())
                            .getTotalPrice()));
            inDialog(
                () -> button(requests, "Подробнее").fire(),
                pane -> {
                  assertTrue(((Label) pane.lookup(".content")).getText().contains(marker));
                  pane.getScene().getWindow().hide();
                });

            for (RentalRequestStatus status :
                List.of(
                    RentalRequestStatus.CONFIRMED,
                    RentalRequestStatus.ACTIVE,
                    RentalRequestStatus.COMPLETED)) {
              inDialog(
                  () -> button(requests, "Сменить статус").fire(),
                  pane -> {
                    ComboBox<?> choices = (ComboBox<?>) pane.lookup(".combo-box");
                    choices.getSelectionModel().select(choices.getItems().indexOf(status));
                    save(pane);
                  });
              assertEquals(
                  status, context.rentalRequestService().getById(request.requestId()).getStatus());
            }
            assertTrue(button(requests, "Изменить даты").isDisabled());
            assertTrue(button(requests, "Сменить статус").isDisabled());

            ui.tab("Объекты");
            ui.refresh();
            select(listings, row -> ((ListingDetails) row).listingId().equals(listing.getId()));
            assertEquals(1, ((ListView<?>) listings.lookup("#relatedList")).getItems().size());
            ui.tab("Пользователи");
            ui.refresh();
            select(users, row -> ((User) row).getId().equals(renter.getId()));
            assertEquals(1, ((ListView<?>) users.lookup("#requestsList")).getItems().size());

            Parent reviews = ui.tab("Отзывы");
            inDialog(
                () -> button(reviews, "Добавить").fire(),
                pane -> {
                  choose(
                      pane,
                      "requestBox",
                      value -> ((ReviewableRequest) value).requestId().equals(request.requestId()));
                  box(pane, "ratingBox").getSelectionModel().select(Integer.valueOf(5));
                  ((TextArea) pane.lookup("#commentArea")).setText("Workflow review");
                  save(pane);
                });
            ReviewDetails review =
                (ReviewDetails)
                    select(
                        reviews,
                        row -> ((ReviewDetails) row).requestId().equals(request.requestId()));
            inDialog(
                () -> button(reviews, "Изменить").fire(),
                pane -> {
                  assertFalse(pane.lookup("#requestBox").isVisible());
                  assertTrue(pane.lookup("#requestLabel").isVisible());
                  assertEquals(Integer.valueOf(5), box(pane, "ratingBox").getValue());
                  box(pane, "ratingBox").getSelectionModel().select(Integer.valueOf(4));
                  save(pane);
                });
            assertEquals(4, context.reviewService().getById(review.reviewId()).getRating());
            remove(reviews);
            assertTrue(new ReviewRepository().findById(review.reviewId()).isEmpty());

            ui.tab("Заявки");
            inDialog(
                () -> button(requests, "Удалить").fire(), pane -> button(pane, "Отмена").fire());
            assertTrue(new RentalRequestRepository().findById(request.requestId()).isPresent());
            remove(requests);
            assertTrue(new RentalRequestRepository().findById(request.requestId()).isEmpty());
            ui.tab("Объекты");
            remove(listings);
            assertTrue(new ListingRepository().findById(listing.getId()).isEmpty());
            ui.tab("Пользователи");
            remove(users);
            select(users, row -> ((User) row).getId().equals(owner.getId()));
            remove(users);
            assertTrue(new UserRepository().findById(owner.getId()).isEmpty());
            assertTrue(new UserRepository().findById(renter.getId()).isEmpty());
          } finally {
            ui.close();
            cleanup(context, marker);
          }
          return null;
        });
  }

  @Test
  void realMainWindowFiltersSortsCalculatesStatisticsAndExportsVisibleRows() throws Exception {
    onFx(
        () -> {
          AppContext context = new AppContext();
          String marker = "Search-" + UUID.randomUUID().toString().substring(0, 8);
          User owner =
              context
                  .userService()
                  .createUser(marker + " Owner", marker + "-owner@test.com", null, UserRole.OWNER);
          User renter =
              context
                  .userService()
                  .createUser(
                      marker + " Renter", marker + "-renter@test.com", null, UserRole.RENTER);
          Listing drill =
              context
                  .listingService()
                  .createListing(owner.getId(), marker + " Drill", "", BigDecimal.TEN, "Tools");
          Listing tent =
              context
                  .listingService()
                  .createListing(
                      owner.getId(), marker + " Tent", "", new BigDecimal("20"), "Sport");
          RentalRequest first =
              context
                  .rentalRequestService()
                  .create(
                      drill.getId(),
                      renter.getId(),
                      LocalDate.of(2036, 1, 1),
                      LocalDate.of(2036, 1, 2));
          RentalRequest second =
              context
                  .rentalRequestService()
                  .create(
                      tent.getId(),
                      renter.getId(),
                      LocalDate.of(2036, 1, 3),
                      LocalDate.of(2036, 1, 5));
          RentalRequest third =
              context
                  .rentalRequestService()
                  .create(
                      drill.getId(),
                      renter.getId(),
                      LocalDate.of(2036, 1, 6),
                      LocalDate.of(2036, 1, 9));
          Ui ui = new Ui(context);
          try {
            Parent requests = ui.tab("Заявки");
            int total = table(requests).getItems().size();
            assertEquals(context.rentalRequestService().getAll().size(), total);
            assertFalse(ui.root.lookup("#statusBar").isManaged());
            for (String term :
                List.of(marker + " Drill", renter.getFullName(), owner.getFullName())) {
              text(requests, "searchField").setText(term);
              button(requests, "Применить").fire();
              assertTrue(table(requests).getItems().size() > 0);
              assertTrue(table(requests).getItems().size() <= 3);
            }
            button(requests, "Сбросить").fire();
            box(requests, "statusBox").getSelectionModel().select(RentalRequestStatus.NEW);
            choose(
                requests,
                "ownerBox",
                value -> value instanceof User user && user.getId().equals(owner.getId()));
            date(requests, "fromPicker", LocalDate.of(2036, 1, 1));
            date(requests, "toPicker", LocalDate.of(2036, 1, 9));
            box(requests, "sortBox").getSelectionModel().select(RequestSort.TOTAL_PRICE);
            button(requests, "Применить").fire();
            List<RequestDetails> rows =
                table(requests).getItems().stream().map(RequestDetails.class::cast).toList();
            assertEquals(
                List.of(first.getId(), third.getId(), second.getId()),
                rows.stream().map(RequestDetails::requestId).toList());
            ((CheckBox) requests.lookup("#descendingBox")).setSelected(true);
            button(requests, "Применить").fire();
            assertEquals(
                second.getId(),
                ((RequestDetails) table(requests).getItems().getFirst()).requestId());
            box(requests, "sortBox").getSelectionModel().select(RequestSort.START_DATE);
            button(requests, "Применить").fire();
            assertEquals(
                third.getId(),
                ((RequestDetails) table(requests).getItems().getFirst()).requestId());
            date(requests, "fromPicker", LocalDate.of(2036, 2, 1));
            button(requests, "Применить").fire();
            assertTrue(requests.lookup("#filterError").isVisible());
            button(requests, "Сбросить").fire();
            text(requests, "searchField").setText("No-match-for-export");
            button(requests, "Применить").fire();
            assertTrue(table(requests).getItems().isEmpty());
            assertTrue(
                ((Label) table(requests).getPlaceholder()).getText().contains("Ничего не найдено"));
            button(requests, "Сбросить").fire();
            assertEquals(total, table(requests).getItems().size());

            Parent statistics = ui.tab("Статистика");
            int figures =
                List.of("requestsBox", "moneyBox", "catalogBox").stream()
                    .mapToInt(
                        id ->
                            ((javafx.scene.layout.VBox) statistics.lookup("#" + id))
                                .getChildren()
                                .size())
                    .sum();
            assertEquals(12, figures);
            date(statistics, "fromPicker", LocalDate.of(2036, 1, 1));
            date(statistics, "toPicker", LocalDate.of(2036, 1, 9));
            button(statistics, "Посчитать").fire();
            assertEquals(
                "Заявок за период: 3", ((Label) statistics.lookup("#periodResult")).getText());
            assertEquals(6, ((TableView<?>) statistics.lookup("#statusTable")).getItems().size());

            ui.tab("Заявки");
            text(requests, "searchField").setText(marker + " Drill");
            button(requests, "Применить").fire();
            List<SheetData> sheets = new ArrayList<>();
            for (String name : List.of("requests", "listings", "users", "reviews", "statistics")) {
              Exportable exportable =
                  (Exportable) ui.loader.getNamespace().get(name + "Controller");
              sheets.add(
                  new SheetData(exportable.exportName(), exportable.headers(), exportable.rows()));
            }
            Path file = Files.createTempFile("kr2-visible-export-", ".xlsx");
            try {
              new ExportService().export(sheets, file);
              try (var workbook = WorkbookFactory.create(file.toFile())) {
                assertEquals(5, workbook.getNumberOfSheets());
                var sheet = workbook.getSheet("Заявки");
                assertEquals(table(requests).getItems().size(), sheet.getLastRowNum());
                assertEquals(CellType.NUMERIC, sheet.getRow(1).getCell(7).getCellType());
                assertEquals(marker + " Drill", sheet.getRow(1).getCell(1).getStringCellValue());
              }
            } finally {
              Files.deleteIfExists(file);
            }
          } finally {
            ui.close();
            cleanup(context, marker);
          }
          return null;
        });
  }

  private static void createUser(Parent users, String name, String email, UserRole role)
      throws Exception {
    inDialog(
        () -> button(users, "Добавить").fire(),
        pane -> {
          save(pane);
          assertTrue(pane.lookup("#validation-error-nameField").isVisible());
          text(pane, "nameField").setText(name);
          text(pane, "emailField").setText(email);
          box(pane, "roleBox").getSelectionModel().select(role);
          save(pane);
        });
  }

  private static void cleanup(AppContext context, String marker) {
    for (Listing listing : context.listingService().getAllListings()) {
      if (listing.getTitle().startsWith(marker)) {
        context.rentalRequestService().getAll().stream()
            .filter(request -> request.getListingId().equals(listing.getId()))
            .forEach(request -> context.rentalRequestService().delete(request.getId()));
        context.listingService().deleteListing(listing.getId());
      }
    }
    context.userService().getAllUsers().stream()
        .filter(user -> user.getFullName().startsWith(marker))
        .forEach(user -> context.userService().deleteUser(user.getId()));
  }

  private static void remove(Parent root) throws Exception {
    inDialog(() -> button(root, "Удалить").fire(), pane -> button(pane, "Удалить").fire());
  }

  private static void inDialog(Runnable open, Consumer<DialogPane> action) throws Exception {
    FutureTask<Void> inspect =
        new FutureTask<>(
            () -> {
              DialogPane pane =
                  Window.getWindows().stream()
                      .filter(Window::isShowing)
                      .map(window -> window.getScene().getRoot())
                      .filter(DialogPane.class::isInstance)
                      .map(DialogPane.class::cast)
                      .findFirst()
                      .orElseThrow();
              Window dialogWindow = pane.getScene().getWindow();
              try {
                action.accept(pane);
                assertFalse(
                    dialogWindow.isShowing(), "Expected form to close after successful action");
              } finally {
                dialogWindow.hide();
              }
              return null;
            });
    Platform.runLater(inspect);
    open.run();
    inspect.get(15, TimeUnit.SECONDS);
  }

  private static void save(DialogPane pane) {
    button(pane, "Сохранить").fire();
  }

  private static Button button(Parent root, String title) {
    return root.lookupAll(".button").stream()
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(button -> title.equals(button.getText()))
        .findFirst()
        .orElseThrow();
  }

  private static TextField text(Parent root, String id) {
    return (TextField) root.lookup("#" + id);
  }

  @SuppressWarnings("unchecked")
  private static ComboBox<Object> box(Parent root, String id) {
    return (ComboBox<Object>) root.lookup("#" + id);
  }

  private static void choose(Parent root, String id, Predicate<Object> predicate) {
    ComboBox<Object> box = box(root, id);
    box.getSelectionModel()
        .select(box.getItems().stream().filter(predicate).findFirst().orElseThrow());
  }

  private static void date(Parent root, String id, LocalDate value) {
    ((DatePicker) root.lookup("#" + id)).setValue(value);
  }

  @SuppressWarnings("unchecked")
  private static TableView<Object> table(Parent root) {
    return (TableView<Object>) root.lookup("#table");
  }

  private static Object select(Parent root, Predicate<Object> predicate) {
    TableView<Object> table = table(root);
    Object row = table.getItems().stream().filter(predicate).findFirst().orElseThrow();
    table.getSelectionModel().select(row);
    return row;
  }

  private static <T> T onFx(Callable<T> action) throws Exception {
    FutureTask<T> task = new FutureTask<>(action);
    Platform.runLater(task);
    return task.get(60, TimeUnit.SECONDS);
  }

  private static class Ui {
    private final FXMLLoader loader;
    private final Parent root;
    private final Stage stage = new Stage();

    Ui(AppContext context) throws Exception {
      loader = context.loader("/fxml/main.fxml");
      root = loader.load();
      Scene scene = new Scene(root, 1100, 700);
      scene.getStylesheets().add(Ui.class.getResource("/css/app.css").toExternalForm());
      stage.setScene(scene);
      stage.show();
      root.applyCss();
      root.layout();
    }

    Parent tab(String title) {
      TabPane tabs = (TabPane) root.lookup("#tabPane");
      Tab tab =
          tabs.getTabs().stream()
              .filter(item -> title.equals(item.getText()))
              .findFirst()
              .orElseThrow();
      tabs.getSelectionModel().select(tab);
      root.applyCss();
      root.layout();
      return (Parent) tab.getContent();
    }

    void refresh() {
      button(root, "Обновить").fire();
    }

    void close() {
      stage.close();
    }
  }
}
