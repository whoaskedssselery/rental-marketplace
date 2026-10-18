package rentalmarketplace.fx.controller;

import java.math.BigDecimal;
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
import rentalmarketplace.service.ListingDetails;
import rentalmarketplace.service.ListingService;
import rentalmarketplace.service.RequestDetails;
import rentalmarketplace.service.RequestQuery;

public class ListingsController implements Refreshable, Exportable {
  private final AppContext context;
  private final ListingService service;
  private final ObservableList<ListingDetails> items = FXCollections.observableArrayList();
  private final ObservableList<String> relatedRequests = FXCollections.observableArrayList();

  @FXML private TableView<ListingDetails> table;
  @FXML private TableColumn<ListingDetails, Integer> idColumn;
  @FXML private TableColumn<ListingDetails, String> titleColumn;
  @FXML private TableColumn<ListingDetails, String> ownerColumn;
  @FXML private TableColumn<ListingDetails, String> categoryColumn;
  @FXML private TableColumn<ListingDetails, BigDecimal> priceColumn;
  @FXML private TableColumn<ListingDetails, Boolean> availableColumn;
  @FXML private TableColumn<ListingDetails, LocalDateTime> createdColumn;
  @FXML private Button editButton;
  @FXML private Button deleteButton;
  @FXML private Label countLabel;
  @FXML private Label relatedTitle;
  @FXML private ListView<String> relatedList;

  public ListingsController(AppContext context) {
    this.context = context;
    this.service = context.listingService();
  }

  @FXML
  private void initialize() {
    Tables.column(idColumn, ListingDetails::listingId);
    Tables.column(titleColumn, ListingDetails::title);
    Tables.column(ownerColumn, ListingDetails::ownerName);
    Tables.column(categoryColumn, ListingDetails::category);
    Tables.column(priceColumn, ListingDetails::pricePerDay, Labels::money);
    Tables.column(availableColumn, ListingDetails::available, Labels::yesNo);
    Tables.column(createdColumn, ListingDetails::createdAt, Labels::dateTime);
    Tables.showSorted(table, items);
    table.getSelectionModel().selectedItemProperty().addListener((o, was, now) -> onSelected());
    relatedList.setItems(relatedRequests);
    relatedList.setPlaceholder(new Label("Выберите объект, чтобы увидеть его заявки"));
    onSelected();
    refresh();
  }

  @Override
  public void refresh() {
    ListingDetails selected = table.getSelectionModel().getSelectedItem();
    try {
      items.setAll(service.getDetails());
    } catch (RuntimeException e) {
      Dialogs.error(UserMessages.of(e));
      return;
    }
    Tables.reselect(table, items, selected, ListingDetails::listingId);
    table.setPlaceholder(new Label("Объектов пока нет. Нажмите «Добавить», чтобы создать первый"));
    countLabel.setText("Объектов: " + items.size());
    onSelected();
  }

  @Override
  public String exportName() {
    return "Объекты";
  }

  @Override
  public List<String> headers() {
    return List.of(
        "Номер", "Название", "Владелец", "Категория", "Цена за день", "Доступен", "Создан");
  }

  @Override
  public List<List<Object>> rows() {
    List<List<Object>> rows = new ArrayList<>();
    for (ListingDetails row : table.getItems()) {
      rows.add(
          List.of(
              row.listingId(),
              row.title(),
              row.ownerName(),
              row.category(),
              row.pricePerDay(),
              Labels.yesNo(row.available()),
              row.createdAt() == null ? "" : row.createdAt()));
    }
    return rows;
  }

  @FXML
  private void onAdd() {
    FormLoader.Loaded<ListingFormController> form =
        FormLoader.<ListingFormController>load(context, "/fxml/listing-form.fxml").orElse(null);
    if (form == null) {
      return;
    }
    boolean saved =
        Dialogs.saveForm(
            "Новый объект аренды",
            form.node(),
            form.controller()::validate,
            () -> {
              ListingFormController.Values values = form.controller().values();
              service.createListing(
                  values.ownerId(),
                  values.title(),
                  values.description(),
                  values.pricePerDay(),
                  values.category(),
                  values.available());
            });
    if (saved) {
      refresh();
    }
  }

  @FXML
  private void onEdit() {
    ListingDetails selected = table.getSelectionModel().getSelectedItem();
    if (selected == null) {
      return;
    }
    FormLoader.Loaded<ListingFormController> form =
        FormLoader.<ListingFormController>load(context, "/fxml/listing-form.fxml").orElse(null);
    if (form == null) {
      return;
    }
    form.controller().prepareForEdit(selected);
    boolean saved =
        Dialogs.saveForm(
            "Объект №" + selected.listingId(),
            form.node(),
            form.controller()::validate,
            () -> {
              ListingFormController.Values values = form.controller().values();
              service.updateListing(
                  selected.listingId(),
                  values.ownerId(),
                  values.title(),
                  values.description(),
                  values.pricePerDay(),
                  values.category(),
                  values.available());
            });
    if (saved) {
      refresh();
    }
  }

  @FXML
  private void onDelete() {
    ListingDetails row = table.getSelectionModel().getSelectedItem();
    if (row == null) {
      return;
    }
    boolean confirmed =
        Dialogs.confirm(
            "Удалить объект «" + row.title() + "»?",
            "Объект будет удален без возможности восстановления. "
                + "Если на него есть заявки, удалить его не получится.");
    if (!confirmed) {
      return;
    }
    try {
      service.deleteListing(row.listingId());
    } catch (RuntimeException e) {
      Dialogs.error(UserMessages.of(e));
      return;
    }
    refresh();
  }

  private void onSelected() {
    ListingDetails selected = table.getSelectionModel().getSelectedItem();
    editButton.setDisable(selected == null);
    deleteButton.setDisable(selected == null);
    relatedRequests.clear();
    if (selected == null) {
      relatedTitle.setText("Заявки на выбранный объект");
      return;
    }
    try {
      List<RequestDetails> requests =
          context
              .rentalRequestService()
              .query(
                  new RequestQuery(
                      null, null, null, selected.listingId(), null, null, null, null, false));
      relatedTitle.setText("Заявки на объект «" + selected.title() + "»: " + requests.size());
      for (RequestDetails request : requests) {
        relatedRequests.add(
            "№"
                + request.requestId()
                + ", "
                + request.renterName()
                + ", "
                + Labels.date(request.startDate())
                + " - "
                + Labels.date(request.endDate())
                + ", "
                + Labels.status(request.status())
                + ", "
                + Labels.money(request.totalPrice()));
      }
    } catch (RuntimeException e) {
      relatedTitle.setText("Заявки на выбранный объект");
      Dialogs.error(UserMessages.of(e));
    }
  }
}
