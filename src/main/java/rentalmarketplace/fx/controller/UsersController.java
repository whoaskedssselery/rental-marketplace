package rentalmarketplace.fx.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import rentalmarketplace.fx.support.Tables;
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
  @FXML private Button addButton;
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
    boolean admin = context.access().isAdmin();
    addButton.setVisible(admin);
    addButton.setManaged(admin);
    deleteButton.setVisible(admin);
    deleteButton.setManaged(admin);
    roleColumn.setVisible(admin);
    Tables.column(idColumn, User::getId);
    Tables.column(nameColumn, User::getFullName);
    Tables.column(emailColumn, User::getEmail);
    Tables.column(phoneColumn, user -> user.getPhone() == null ? "" : user.getPhone());
    Tables.column(roleColumn, User::getRole, Labels::role);
    Tables.column(createdColumn, User::getCreatedAt, Labels::dateTime);
    Tables.showSorted(table, items);
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
      items.setAll(service.getVisibleUsers());
    } catch (RuntimeException e) {
      Dialogs.error(UserMessages.of(e));
      return;
    }
    Tables.reselect(table, items, selected, User::getId);
    if (!context.access().isAdmin()) {
      table.getSelectionModel().selectFirst();
    }
    table.setPlaceholder(
        new Label("Пользователей пока нет. Нажмите «Добавить», чтобы создать первого"));
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
    if (!context.access().isAdmin()) {
      return;
    }
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
    if (selected == null || !context.access().mayEdit(selected.getId())) {
      return;
    }
    FormLoader.Loaded<UserFormController> form =
        FormLoader.<UserFormController>load(context, "/fxml/user-form.fxml").orElse(null);
    if (form == null) {
      return;
    }
    form.controller().prepareForEdit(selected);
    if (!context.access().isAdmin()) {
      form.controller().lockRole();
    }
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
    if (user == null || !context.access().isAdmin()) {
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
    editButton.setDisable(selected == null || !context.access().mayEdit(selected.getId()));
    deleteButton.setDisable(selected == null || !context.access().isAdmin());
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
              .requestSearchService()
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
