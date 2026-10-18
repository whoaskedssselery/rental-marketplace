package rentalmarketplace.fx.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rentalmarketplace.fx.AppContext;
import rentalmarketplace.fx.support.Dialogs;
import rentalmarketplace.fx.support.Exportable;
import rentalmarketplace.fx.support.FormValidator;
import rentalmarketplace.fx.support.Labels;
import rentalmarketplace.fx.support.Refreshable;
import rentalmarketplace.fx.support.UserMessages;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.service.RentalRequestService;
import rentalmarketplace.service.RequestDetails;
import rentalmarketplace.service.RequestQuery;

public class RequestsController implements Refreshable, Exportable {
  private static final Logger LOG = LoggerFactory.getLogger(RequestsController.class);

  private record LoadedForm(Node node, RequestFormController controller) {}

  private final AppContext context;
  private final RentalRequestService service;
  private final ObservableList<RequestDetails> items = FXCollections.observableArrayList();
  private RequestQuery query = RequestQuery.all();

  @FXML private TableView<RequestDetails> table;
  @FXML private TableColumn<RequestDetails, Integer> idColumn;
  @FXML private TableColumn<RequestDetails, String> listingColumn;
  @FXML private TableColumn<RequestDetails, String> renterColumn;
  @FXML private TableColumn<RequestDetails, String> ownerColumn;
  @FXML private TableColumn<RequestDetails, LocalDate> startColumn;
  @FXML private TableColumn<RequestDetails, LocalDate> endColumn;
  @FXML private TableColumn<RequestDetails, RentalRequestStatus> statusColumn;
  @FXML private TableColumn<RequestDetails, BigDecimal> totalColumn;
  @FXML private TableColumn<RequestDetails, LocalDateTime> createdColumn;
  @FXML private Button editButton;
  @FXML private Button statusButton;
  @FXML private Button detailsButton;
  @FXML private Button deleteButton;
  @FXML private Label countLabel;

  public RequestsController(AppContext context) {
    this.context = context;
    this.service = context.rentalRequestService();
  }

  @FXML
  private void initialize() {
    idColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().requestId()));
    listingColumn.setCellValueFactory(
        row -> new ReadOnlyObjectWrapper<>(row.getValue().listingTitle()));
    renterColumn.setCellValueFactory(
        row -> new ReadOnlyObjectWrapper<>(row.getValue().renterName()));
    ownerColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().ownerName()));
    startColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().startDate()));
    endColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().endDate()));
    statusColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().status()));
    totalColumn.setCellValueFactory(
        row -> new ReadOnlyObjectWrapper<>(row.getValue().totalPrice()));
    createdColumn.setCellValueFactory(
        row -> new ReadOnlyObjectWrapper<>(row.getValue().createdAt()));
    startColumn.setCellFactory(column -> textCell(Labels::date));
    endColumn.setCellFactory(column -> textCell(Labels::date));
    statusColumn.setCellFactory(column -> textCell(Labels::status));
    totalColumn.setCellFactory(column -> textCell(Labels::money));
    createdColumn.setCellFactory(column -> textCell(Labels::dateTime));

    SortedList<RequestDetails> sorted = new SortedList<>(items);
    sorted.comparatorProperty().bind(table.comparatorProperty());
    table.setItems(sorted);
    table.getSelectionModel().selectedItemProperty().addListener((o, was, now) -> updateButtons());
    table.setRowFactory(
        view -> {
          TableRow<RequestDetails> row = new TableRow<>();
          row.setOnMouseClicked(
              event -> {
                if (event.getButton() == MouseButton.PRIMARY
                    && event.getClickCount() == 2
                    && !row.isEmpty()) {
                  onDetails();
                }
              });
          return row;
        });
    editButton.setTooltip(new Tooltip("Менять даты можно только у новых заявок"));
    updateButtons();
    refresh();
  }

  public void applyQuery(RequestQuery newQuery) {
    query = newQuery == null ? RequestQuery.all() : newQuery;
    refresh();
  }

  @Override
  public void refresh() {
    RequestDetails selected = table.getSelectionModel().getSelectedItem();
    try {
      items.setAll(service.query(query));
    } catch (RuntimeException e) {
      Dialogs.error(UserMessages.of(e));
      return;
    }
    if (selected != null) {
      items.stream()
          .filter(row -> row.requestId().equals(selected.requestId()))
          .findFirst()
          .ifPresent(row -> table.getSelectionModel().select(row));
    }
    boolean filtered = !RequestQuery.all().equals(query);
    table.setPlaceholder(new Label(filtered ? "Ничего не найдено" : "Заявок пока нет"));
    countLabel.setText("Заявок: " + items.size());
    updateButtons();
  }

  @Override
  public String exportName() {
    return "Заявки";
  }

  @Override
  public List<String> headers() {
    return List.of(
        "Номер",
        "Объект",
        "Арендатор",
        "Владелец",
        "Начало",
        "Конец",
        "Статус",
        "Сумма",
        "Создана");
  }

  @Override
  public List<List<Object>> rows() {
    List<List<Object>> rows = new ArrayList<>();
    for (RequestDetails row : table.getItems()) {
      rows.add(
          List.of(
              row.requestId(),
              row.listingTitle(),
              row.renterName(),
              row.ownerName(),
              row.startDate(),
              row.endDate(),
              Labels.status(row.status()),
              row.totalPrice(),
              row.createdAt() == null ? "" : row.createdAt()));
    }
    return rows;
  }

  @FXML
  private void onAdd() {
    LoadedForm form = loadForm();
    if (form == null) {
      return;
    }
    form.controller().prepareForCreate();
    boolean saved =
        Dialogs.saveForm(
            "Новая заявка на аренду",
            form.node(),
            form.controller()::validate,
            () -> {
              RequestFormController.Values values = form.controller().values();
              service.create(
                  values.listingId(), values.renterId(), values.startDate(), values.endDate());
            });
    if (saved) {
      refresh();
    }
  }

  @FXML
  private void onEdit() {
    RequestDetails selected = table.getSelectionModel().getSelectedItem();
    if (selected == null) {
      return;
    }
    LoadedForm form = loadForm();
    if (form == null) {
      return;
    }
    form.controller().prepareForEdit(selected);
    boolean saved =
        Dialogs.saveForm(
            "Заявка №" + selected.requestId(),
            form.node(),
            form.controller()::validate,
            () -> {
              if (service.canChangeDates(selected.status())) {
                RequestFormController.Values values = form.controller().values();
                service.updateDates(selected.requestId(), values.startDate(), values.endDate());
              }
            });
    if (saved) {
      refresh();
    }
  }

  @FXML
  private void onChangeStatus() {
    RequestDetails selected = table.getSelectionModel().getSelectedItem();
    if (selected == null) {
      return;
    }
    List<RentalRequestStatus> allowed = service.allowedTransitions(selected.status());
    if (allowed.isEmpty()) {
      Dialogs.message(
          "Статус изменить нельзя",
          "Заявка в статусе «" + Labels.status(selected.status()) + "» уже закрыта.");
      return;
    }
    ComboBox<RentalRequestStatus> box = new ComboBox<>(FXCollections.observableArrayList(allowed));
    box.setMaxWidth(Double.MAX_VALUE);
    box.setPromptText("Выберите новый статус");
    box.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(RentalRequestStatus status) {
            return Labels.status(status);
          }

          @Override
          public RentalRequestStatus fromString(String text) {
            return null;
          }
        });
    VBox content =
        new VBox(10, new Label("Текущий статус: " + Labels.status(selected.status())), box);
    content.setPrefWidth(340);
    boolean saved =
        Dialogs.saveForm(
            "Смена статуса заявки №" + selected.requestId(),
            content,
            () -> {
              FormValidator validator = new FormValidator();
              validator.selected("Новый статус", box.getValue());
              return validator.result();
            },
            () -> service.changeStatus(selected.requestId(), box.getValue()));
    if (saved) {
      refresh();
    }
  }

  @FXML
  private void onDetails() {
    RequestDetails row = table.getSelectionModel().getSelectedItem();
    if (row == null) {
      return;
    }
    String text =
        "Объект: "
            + row.listingTitle()
            + "\nВладелец: "
            + row.ownerName()
            + "\nАрендатор: "
            + row.renterName()
            + " ("
            + row.renterEmail()
            + ")"
            + "\nПериод: "
            + Labels.date(row.startDate())
            + " - "
            + Labels.date(row.endDate())
            + "\nСтатус: "
            + Labels.status(row.status())
            + "\nСумма: "
            + Labels.money(row.totalPrice())
            + "\nСоздана: "
            + Labels.dateTime(row.createdAt());
    Dialogs.message("Заявка №" + row.requestId(), text);
  }

  @FXML
  private void onDelete() {
    RequestDetails row = table.getSelectionModel().getSelectedItem();
    if (row == null) {
      return;
    }
    boolean confirmed =
        Dialogs.confirm(
            "Удалить заявку №" + row.requestId() + "?",
            "Заявка на «"
                + row.listingTitle()
                + "» от "
                + row.renterName()
                + " будет удалена без возможности восстановления."
                + " Отзыв на эту заявку, если он есть, удалится вместе с ней.");
    if (!confirmed) {
      return;
    }
    try {
      service.delete(row.requestId());
    } catch (RuntimeException e) {
      Dialogs.error(UserMessages.of(e));
      return;
    }
    refresh();
  }

  private void updateButtons() {
    RequestDetails selected = table.getSelectionModel().getSelectedItem();
    boolean none = selected == null;
    editButton.setDisable(none || !service.canChangeDates(selected.status()));
    statusButton.setDisable(none || service.allowedTransitions(selected.status()).isEmpty());
    detailsButton.setDisable(none);
    deleteButton.setDisable(none);
  }

  private LoadedForm loadForm() {
    try {
      FXMLLoader loader = context.loader("/fxml/request-form.fxml");
      Node node = loader.load();
      return new LoadedForm(node, loader.getController());
    } catch (IOException | RuntimeException e) {
      LOG.error("Не удалось открыть форму заявки", e);
      Dialogs.error(UserMessages.of(e));
      return null;
    }
  }

  private static <T> TableCell<RequestDetails, T> textCell(Function<T, String> format) {
    return new TableCell<>() {
      @Override
      protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty || item == null ? null : format.apply(item));
      }
    };
  }
}
