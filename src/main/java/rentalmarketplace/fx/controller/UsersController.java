package rentalmarketplace.fx.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import rentalmarketplace.fx.AppContext;
import rentalmarketplace.fx.support.Dialogs;
import rentalmarketplace.fx.support.Exportable;
import rentalmarketplace.fx.support.FormLoader;
import rentalmarketplace.fx.support.Labels;
import rentalmarketplace.fx.support.Refreshable;
import rentalmarketplace.fx.support.TableCells;
import rentalmarketplace.fx.support.UserMessages;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.service.ListingDetails;
import rentalmarketplace.service.RequestDetails;
import rentalmarketplace.service.RequestQuery;
import rentalmarketplace.service.UserService;

public class UsersController implements Refreshable, Exportable {
  private final AppContext context;
  private final UserService service;
  private final ObservableList<User> items = FXCollections.observableArrayList();
  private final ObservableList<String> ownedListings = FXCollections.observableArrayList();
  private final ObservableList<String> rentedRequests = FXCollections.observableArrayList();

  @FXML private TableView<User> table;
  @FXML private TableColumn<User, Integer> idColumn;
  @FXML private TableColumn<User, String> nameColumn;
  @FXML private TableColumn<User, String> emailColumn;
  @FXML private TableColumn<User, String> phoneColumn;
  @FXML private TableColumn<User, UserRole> roleColumn;
  @FXML private TableColumn<User, LocalDateTime> createdColumn;
  @FXML private Button editButton;
  @FXML private Button deleteButton;
  @FXML private Label countLabel;
  @FXML private Label listingsTitle;
  @FXML private Label requestsTitle;
  @FXML private ListView<String> listingsList;
  @FXML private ListView<String> requestsList;

  public UsersController(AppContext context) {
    this.context = context;
    this.service = context.userService();
  }

  @FXML
  private void initialize() {
    idColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getId()));
    nameColumn.setCellValueFactory(
        row -> new ReadOnlyObjectWrapper<>(row.getValue().getFullName()));
    emailColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getEmail()));
    phoneColumn.setCellValueFactory(
        row ->
            new ReadOnlyObjectWrapper<>(
                row.getValue().getPhone() == null ? "" : row.getValue().getPhone()));
    roleColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getRole()));
    createdColumn.setCellValueFactory(
        row -> new ReadOnlyObjectWrapper<>(row.getValue().getCreatedAt()));
    roleColumn.setCellFactory(column -> TableCells.text(Labels::role));
    createdColumn.setCellFactory(column -> TableCells.text(Labels::dateTime));

    SortedList<User> sorted = new SortedList<>(items);
    sorted.comparatorProperty().bind(table.comparatorProperty());
    table.setItems(sorted);
    table.getSelectionModel().selectedItemProperty().addListener((o, was, now) -> onSelected());
    listingsList.setItems(ownedListings);
    requestsList.setItems(rentedRequests);
    listingsList.setPlaceholder(new Label("Нет объектов"));
    requestsList.setPlaceholder(new Label("Нет заявок"));
    onSelected();
    refresh();
  }

  @Override
  public void refresh() {
    User selected = table.getSelectionModel().getSelectedItem();
    try {
      items.setAll(service.getAllUsers());
    } catch (RuntimeException e) {
      Dialogs.error(UserMessages.of(e));
      return;
    }
    if (selected != null) {
      items.stream()
          .filter(row -> row.getId().equals(selected.getId()))
          .findFirst()
          .ifPresent(row -> table.getSelectionModel().select(row));
    }
    table.setPlaceholder(new Label("Пользователей пока нет"));
    countLabel.setText("Пользователей: " + items.size());
    onSelected();
  }

  @Override
  public String exportName() {
    return "Пользователи";
  }

  @Override
  public List<String> headers() {
    return List.of("Номер", "ФИО", "Email", "Телефон", "Роль", "Создан");
  }

  @Override
  public List<List<Object>> rows() {
    List<List<Object>> rows = new ArrayList<>();
    for (User user : table.getItems()) {
      rows.add(
          List.of(
              user.getId(),
              user.getFullName(),
              user.getEmail(),
              user.getPhone() == null ? "" : user.getPhone(),
              Labels.role(user.getRole()),
              user.getCreatedAt() == null ? "" : user.getCreatedAt()));
    }
    return rows;
  }

  @FXML
  private void onAdd() {
    FormLoader.Loaded<UserFormController> form =
        FormLoader.<UserFormController>load(context, "/fxml/user-form.fxml").orElse(null);
    if (form == null) {
      return;
    }
    boolean saved =
        Dialogs.saveForm(
            "Новый пользователь",
            form.node(),
            form.controller()::validate,
            () -> {
              UserFormController.Values values = form.controller().values();
              service.createUser(values.fullName(), values.email(), values.phone(), values.role());
            });
    if (saved) {
      refresh();
    }
  }

  @FXML
  private void onEdit() {
    User selected = table.getSelectionModel().getSelectedItem();
    if (selected == null) {
      return;
    }
    FormLoader.Loaded<UserFormController> form =
        FormLoader.<UserFormController>load(context, "/fxml/user-form.fxml").orElse(null);
    if (form == null) {
      return;
    }
    form.controller().prepareForEdit(selected);
    boolean saved =
        Dialogs.saveForm(
            "Пользователь №" + selected.getId(),
            form.node(),
            form.controller()::validate,
            () -> {
              UserFormController.Values values = form.controller().values();
              service.updateUser(
                  selected.getId(),
                  values.fullName(),
                  values.email(),
                  values.phone(),
                  values.role());
            });
    if (saved) {
      refresh();
    }
  }

  @FXML
  private void onDelete() {
    User user = table.getSelectionModel().getSelectedItem();
    if (user == null) {
      return;
    }
    boolean confirmed =
        Dialogs.confirm(
            "Удалить пользователя «" + user.getFullName() + "»?",
            "Пользователь будет удален без возможности восстановления. "
                + "Если у него есть объекты или заявки, удалить его не получится.");
    if (!confirmed) {
      return;
    }
    try {
      service.deleteUser(user.getId());
    } catch (RuntimeException e) {
      Dialogs.error(UserMessages.of(e));
      return;
    }
    refresh();
  }

  private void onSelected() {
    User selected = table.getSelectionModel().getSelectedItem();
    editButton.setDisable(selected == null);
    deleteButton.setDisable(selected == null);
    ownedListings.clear();
    rentedRequests.clear();
    if (selected == null) {
      listingsTitle.setText("Объекты пользователя");
      requestsTitle.setText("Заявки пользователя");
      return;
    }
    try {
      List<ListingDetails> listings = context.listingService().getDetailsByOwner(selected.getId());
      for (ListingDetails listing : listings) {
        ownedListings.add(
            listing.title()
                + ", "
                + Labels.money(listing.pricePerDay())
                + "/день, "
                + (listing.available() ? "доступен" : "недоступен"));
      }
      listingsTitle.setText("Объекты пользователя: " + listings.size());
      List<RequestDetails> requests =
          context
              .rentalRequestService()
              .query(
                  new RequestQuery(
                      null, null, null, null, selected.getId(), null, null, null, false));
      for (RequestDetails request : requests) {
        rentedRequests.add(
            "№"
                + request.requestId()
                + ", "
                + request.listingTitle()
                + ", "
                + Labels.date(request.startDate())
                + " - "
                + Labels.date(request.endDate())
                + ", "
                + Labels.status(request.status()));
      }
      requestsTitle.setText("Заявки пользователя как арендатора: " + requests.size());
    } catch (RuntimeException e) {
      Dialogs.error(UserMessages.of(e));
    }
  }
}
